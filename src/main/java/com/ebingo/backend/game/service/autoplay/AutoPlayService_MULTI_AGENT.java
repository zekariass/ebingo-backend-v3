

package com.ebingo.backend.game.service.autoplay;

import com.ebingo.backend.common.Constants;
import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.dto.RoomInternalDto;
import com.ebingo.backend.game.entity.Room;
import com.ebingo.backend.game.enums.BingoColumn;
import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.ParticipantType;
import com.ebingo.backend.game.mappers.RoomMapper;
import com.ebingo.backend.game.service.BingoPatternVerifier;
import com.ebingo.backend.game.service.GameService;
import com.ebingo.backend.game.service.RoomService;
import com.ebingo.backend.system.redis.RedisKeys;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.repository.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription.Message;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AutoPlayService
 * <p>
 * Key changes vs ORIG:
 * - ONE Redis pub/sub subscription per room (not per bot).
 * - In-memory bot state + per-number index to find affected bots quickly.
 * - Bounded concurrency for joins, marks, and claims.
 * <p>
 * Additional optimization:
 * - Removed playerStateService.getMarkedNumbers() and verify pattern using local BotSession marks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoPlayService_MULTI_AGENT {

    private final RoomService roomService;
    private final UserProfileRepository userProfileRepository;
    private final GameService gameService;
    private final BingoPatternVerifier patternVerifier;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final GlobalAutoPlayManager autoPlayManager;
    private final MeterRegistry meterRegistry;

    // Tunables (increase carefully)
    private static final long JOIN_DELAY_MIN_MS = 500;
    private static final long JOIN_DELAY_MAX_MS = 800;
    private static final Duration SPAWN_LOCK_TTL = Duration.ofSeconds(5);
    private static final Duration BINGO_CLAIM_DELAY = Duration.ofMillis(3000);

    // Concurrency controls
    private static final int JOIN_CONCURRENCY_SOFT_CAP = 6;     // per room spawn
    private static final int MARK_CONCURRENCY_PER_ROOM = 64;    // per draw, per room
    private static final int GLOBAL_INFLIGHT_MARK_CALLS = 512;  // global safety cap (soft)

    private final Random random = new Random();

    // room runtime registry
    private final Map<Long, RoomRuntime> runtimes = new ConcurrentHashMap<>();

    // metrics
    private Counter spawnAttempts;
    private Counter spawnSuccess;
    private Counter spawnFailures;
    private Timer spawnTimer;

    private Counter markingAttempts;
    private Counter markingErrors;
    private Counter bingoClaims;

    private final AtomicLong globalInFlightMarks = new AtomicLong(0);

    private String activeBotsKey(Long roomId) {
        return "room:" + roomId + ":activeBots";
    }

    private String spawnLockKey(Long roomId, Long gameId) {
        return "room:" + roomId + ":game:" + gameId + ":spawnLock";
    }

    @PostConstruct
    public void init() {
        spawnAttempts = Counter.builder("autoplay.bots.spawn.attempts").register(meterRegistry);
        spawnSuccess = Counter.builder("autoplay.bots.spawn.success").register(meterRegistry);
        spawnFailures = Counter.builder("autoplay.bots.spawn.failures").register(meterRegistry);
        spawnTimer = Timer.builder("autoplay.bots.spawn.duration").register(meterRegistry);

        markingAttempts = Counter.builder("autoplay.marking.attempts").register(meterRegistry);
        markingErrors = Counter.builder("autoplay.marking.errors").register(meterRegistry);
        bingoClaims = Counter.builder("autoplay.bingo.claims").register(meterRegistry);

        // clear stale reservations
        roomService.getAllRoomsWIthCardPoolForAutoService()
                .map(RoomMapper::toEntity)
                .filter(Room::getBotAllowed)
                .flatMap(room -> redisTemplate.delete(activeBotsKey(room.getId())))
                .subscribe(
                        v -> {
                        },
                        e -> log.warn("Failed clearing active bots on startup: {}", e.getMessage()),
                        () -> log.info("Startup: active bot reservations cleared")
                );

        // bootstrap room runtimes
        roomService.getAllRoomsWIthCardPoolForAutoService()
                .filter(RoomInternalDto::getBotAllowed)
                .doOnNext(this::ensureRoomRuntime)
                .subscribe(
                        v -> {
                        },
                        e -> log.error("AutoPlay bootstrap failure", e),
                        () -> log.info("AutoPlayService bootstrap complete")
                );

        watchGlobalToggle();
    }

    private void ensureRoomRuntime(RoomInternalDto room) {
        runtimes.compute(room.getId(), (roomId, existing) -> {
            if (existing != null) {
                existing.refreshRoom(room);
                return existing;
            }
            RoomRuntime rt = new RoomRuntime(room);
            rt.start();
            return rt;
        });
    }

    private void watchGlobalToggle() {
        autoPlayManager.events()
                .subscribe(evt -> {
                    if (!evt.isEnabled()) {
                        log.warn("AUTOPLAY DISABLED â€” {}", evt.getReason());
                        // stop all room runtimes activity and clear redis reservations
                        runtimes.values().forEach(RoomRuntime::disable);
                        roomService.getAllRoomsWIthCardPoolForAutoService()
                                .filter(RoomInternalDto::getBotAllowed)
                                .flatMap(room -> redisTemplate.delete(activeBotsKey(room.getId())))
                                .subscribe();
                    } else {
                        log.warn("AUTOPLAY ENABLED â€” {}", evt.getReason());
                        runtimes.values().forEach(RoomRuntime::enable);
                        roomService.getAllRoomsWIthCardPoolForAutoService()
                                .filter(RoomInternalDto::getBotAllowed)
                                .doOnNext(this::ensureRoomRuntime)
                                .flatMap(room ->
                                        gameService.getOrInitializeGame(room.getId(), "AUTO", room.getCapacity(), room.getAgentId())
                                                .flatMap(gs -> runtimes.get(room.getId()).spawnBotsForGameIfNeeded(gs.getGameId()))
                                )
                                .subscribe();
                    }
                });
    }

    /**
     * Per-room runtime: ONE Redis subscription, per-game in-memory state.
     */
    private final class RoomRuntime {
        private volatile RoomInternalDto room;
        private final Long roomId;

        private volatile boolean enabled = true;

        @SuppressWarnings("unused")
        private volatile Disposable roomSubscription;

        // current game state
        private volatile Long currentGameId;
        private final Object gameLock = new Object();

        // per-game bot sessions: botTelegramId -> session
        private final Map<String, BotSession> botSessions = new ConcurrentHashMap<>();

        // cached card pool for room: cardId -> CardInfo
        private volatile Map<String, CardInfo> roomCardPool = Collections.emptyMap();

        // per-game index: number -> list of (botId, cardId)
        // rebuilt each game when bots join (incrementally updated)
        private final Map<Integer, List<BotCardRef>> numberIndex = new ConcurrentHashMap<>();

        RoomRuntime(RoomInternalDto initial) {
            this.room = initial;
            this.roomId = initial.getId();
            this.roomCardPool = parseCardPool(initial);
        }

        void refreshRoom(RoomInternalDto updated) {
            this.room = updated;
            this.roomCardPool = parseCardPool(updated);
        }

        void start() {
            String channel = RedisKeys.roomChannel(roomId);

            roomSubscription = redisTemplate.listenToChannel(channel)
                    .map(Message::getMessage)
                    .map(String::valueOf)
                    .map(AutoPlayService_MULTI_AGENT.this::parsePayload)
                    .flatMap(this::handleRoomEvent)
                    .onErrorContinue((err, o) -> log.warn("Room {} subscription error: {}", roomId, err.getMessage()))
                    .subscribe();

            // bootstrap for current/next game
            gameService.getOrInitializeGame(roomId, "AUTO", room.getCapacity(), room.getAgentId())
                    .flatMap(gs -> {
                        synchronized (gameLock) {
                            currentGameId = gs.getGameId();
                        }
                        return spawnBotsForGameIfNeeded(gs.getGameId());
                    })
                    .doOnError(e -> log.warn("Room {} initial spawn failed: {}", roomId, e.getMessage()))
                    .subscribe();
        }

        void disable() {
            enabled = false;
        }

        void enable() {
            enabled = true;
        }

        Mono<Void> handleRoomEvent(Map<String, Object> evt) {
            Object typeObj = evt.get("type");
            if (!(typeObj instanceof String type)) return Mono.empty();

            Object payloadObj = evt.get("payload");
            if (!(payloadObj instanceof Map<?, ?> payload)) return Mono.empty();

            Long payloadRoomId = convertToLong(payload.get("roomId"));
            if (payloadRoomId != null && !Objects.equals(payloadRoomId, roomId)) return Mono.empty();

            if ("game.ended".equals(type)) {
                Long endedGameId = convertToLong(payload.get("gameId"));
                if (endedGameId == null) return Mono.empty();
                return onGameEnded(endedGameId);
            }

            if ("game.numberDrawn".equals(type)) {
                Long gameId = convertToLong(payload.get("gameId"));
                Integer number = convertToInteger(payload.get("number"));
                if (gameId == null || number == null) return Mono.empty();
                return onNumberDrawn(gameId, number);
            }

            return Mono.empty();
        }

        Mono<Void> onGameEnded(Long endedGameId) {
            log.info("Room {}: game {} ended â€” clearing runtime state", roomId, endedGameId);

            synchronized (gameLock) {
                if (Objects.equals(currentGameId, endedGameId)) {
                    currentGameId = null;
                }
            }

            botSessions.clear();
            numberIndex.clear();

            Mono<Long> clearReservations = redisTemplate.delete(activeBotsKey(roomId))
                    .onErrorResume(e -> {
                        log.warn("Room {}: failed clearing activeBots after game ended: {}", roomId, e.getMessage());
                        return Mono.just(0L);
                    });

            Mono<Void> startNext = gameService.getOrInitializeGame(roomId, "AUTO", room.getCapacity(), room.getAgentId())
                    .flatMap(gs -> {
                        synchronized (gameLock) {
                            currentGameId = gs.getGameId();
                        }
                        return spawnBotsForGameIfNeeded(gs.getGameId());
                    })
                    .onErrorResume(e -> {
                        log.warn("Room {}: error reinitializing game after end: {}", roomId, e.getMessage());
                        return Mono.empty();
                    });

            return clearReservations.then(startNext);
        }

        Mono<Void> onNumberDrawn(Long gameId, Integer drawnNumber) {
            Long cg;
            synchronized (gameLock) {
                cg = currentGameId;
            }
            if (cg == null || !Objects.equals(cg, gameId)) return Mono.empty();
            if (!autoPlayManager.isEnabled() || !enabled) return Mono.empty();

            List<BotCardRef> refs = numberIndex.getOrDefault(drawnNumber, List.of());
            if (refs.isEmpty()) return Mono.empty();

            if (globalInFlightMarks.get() > GLOBAL_INFLIGHT_MARK_CALLS) {
                log.warn("Room {} game {}: global in-flight marks high ({}), dropping draw {} processing",
                        roomId, gameId, globalInFlightMarks.get(), drawnNumber);
                return Mono.empty();
            }

            return Flux.fromIterable(refs)
                    .flatMap(ref -> markForBot(gameId, drawnNumber, ref), MARK_CONCURRENCY_PER_ROOM)
                    .then();
        }

        /**
         * Mark + verify using LOCAL marked numbers snapshot (no getMarkedNumbers() round-trip).
         */
        private Mono<Void> markForBot(Long gameId, Integer drawnNumber, BotCardRef ref) {
            BotSession session = botSessions.get(ref.botTelegramId);
            if (session == null) return Mono.empty();
            if (!session.hasCard(ref.cardId)) return Mono.empty();

            CardInfo cardInfo = roomCardPool.get(ref.cardId);
            if (cardInfo == null) return Mono.empty();

            boolean newlyMarked = session.mark(ref.cardId, drawnNumber);
            if (!newlyMarked) return Mono.empty();

            markingAttempts.increment();
            globalInFlightMarks.incrementAndGet();

            return gameService.markNumber(roomId, gameId, session.botTelegramId,
                            Map.of("number", drawnNumber, "cardId", ref.cardId))
                    .then(Mono.defer(() -> {
                        Set<Integer> localMarked = session.markedSnapshot(ref.cardId);

                        boolean bingo = patternVerifier.verifyPattern(
                                cardInfo.getNumbers(),
                                localMarked,
                                room.getPattern().name()
                        );

                        if (!bingo) return Mono.empty();

                        Map<String, Object> payload = buildBingoPayload(
                                gameId,
                                cardInfo,
                                session.botProfile,
                                room.getPattern(),
                                localMarked
                        );
                        bingoClaims.increment();

                        return Mono.just(payload)
                                .delayElement(BINGO_CLAIM_DELAY)
                                .flatMap(p -> gameService.claimBingo(roomId, session.botTelegramId, p, room.getAgentId(), ParticipantType.BOT))
                                .then();
                    }))
                    .onErrorResume(e -> {
                        markingErrors.increment();
                        log.warn("Room {} game {}: mark/claim error bot {}: {}", roomId, gameId, session.botTelegramId, e.getMessage());
                        return Mono.empty();
                    })
                    .doFinally(sig -> globalInFlightMarks.decrementAndGet());
        }

        Mono<Void> spawnBotsForGameIfNeeded(Long gameId) {
            return roomService.getRoomWithCardPoolById(roomId)
                    .defaultIfEmpty(room)
                    .flatMap(fresh -> {
                        refreshRoom(fresh);

                        if (!autoPlayManager.isEnabled() || !enabled) return Mono.empty();
                        if (!Boolean.TRUE.equals(fresh.getBotAllowed())) return Mono.empty();

                        int minBots = Optional.ofNullable(fresh.getMinBots()).orElse(0);
                        int maxBots = Optional.ofNullable(fresh.getMaxBots()).orElse(0);

                        if (maxBots <= 0 || maxBots < minBots) {
                            log.warn("Room {} has invalid bot config: minBots={}, maxBots={}", roomId, minBots, maxBots);
                            return Mono.empty();
                        }

                        int targetBots = minBots + (int) (Math.random() * (maxBots - minBots + 1));

                        String lockKey = spawnLockKey(roomId, gameId);

                        return redisTemplate.opsForValue().setIfAbsent(lockKey, "1", SPAWN_LOCK_TTL)
                                .flatMap(acquired -> {
                                    if (!Boolean.TRUE.equals(acquired)) {
                                        log.debug("Room {} game {} spawn lock held; skip", roomId, gameId);
                                        return Mono.empty();
                                    }

                                    Timer.Sample sample = Timer.start(meterRegistry);
                                    spawnAttempts.increment();

                                    return spawnInternal(fresh, gameId, targetBots)
                                            .doOnSuccess(v -> spawnSuccess.increment())
                                            .doOnError(e -> {
                                                spawnFailures.increment();
                                                log.warn("Room {} game {} spawn failed: {}", roomId, gameId, e.getMessage());
                                            })
                                            .doFinally(sig -> {
                                                redisTemplate.delete(lockKey).subscribe();
                                                sample.stop(spawnTimer);
                                            });
                                });
                    })
                    .onErrorResume(e -> {
                        spawnFailures.increment();
                        log.warn("Room {} game {} spawn attempt failed: {}", roomId, gameId, e.getMessage());
                        return Mono.empty();
                    });
        }

        private Mono<Void> spawnInternal(RoomInternalDto fresh, Long gameId, int targetBots) {
            String key = activeBotsKey(roomId);

            return redisTemplate.opsForSet().size(key)
                    .map(sz -> sz == null ? 0L : sz)
                    .flatMap(currentActiveLong -> {
                        int currentActive = currentActiveLong.intValue();
                        int toSpawn = Math.max(0, targetBots - currentActive);
                        if (toSpawn <= 0) {
                            log.info("Room {} game {}: activeBots={} target={} -> no spawn", roomId, gameId, currentActive, targetBots);
                            return Mono.empty();
                        }

                        int cpu = Math.max(1, Runtime.getRuntime().availableProcessors());
                        int joinConcurrency = Math.max(1, Math.min(JOIN_CONCURRENCY_SOFT_CAP, Math.min(toSpawn, cpu * 2)));

                        log.info("Room {} game {} spawning {} bots (target={}, active={}), joinConcurrency={}",
                                roomId, gameId, toSpawn, targetBots, currentActive, joinConcurrency);

                        return userProfileRepository.findBotsByRoom(roomId)
                                .take(toSpawn)
                                .flatMap(bot -> tryReserveAndJoinBotClean(fresh, gameId, bot), joinConcurrency)
                                .then();
                    });
        }

        private Mono<Void> tryReserveAndJoinBotClean(RoomInternalDto fresh, Long gameId, UserProfile botProfile) {
            String key = activeBotsKey(roomId);
            String botId = Objects.toString(botProfile.getTelegramId(), null);
            if (botId == null) return Mono.empty();

            if (botSessions.containsKey(botId)) return Mono.empty();

            return redisTemplate.opsForSet().isMember(key, botId)
                    .flatMap(isMember -> {
                        if (Boolean.TRUE.equals(isMember)) return Mono.empty();

                        return redisTemplate.opsForSet().add(key, botId)
                                .flatMap(added -> {
                                    boolean reserved = added != null && added > 0;
                                    if (!reserved) return Mono.empty();

                                    long delayMs = JOIN_DELAY_MIN_MS
                                            + (long) (random.nextDouble() * (JOIN_DELAY_MAX_MS - JOIN_DELAY_MIN_MS));

                                    return Mono.delay(Duration.ofMillis(delayMs))
                                            .then(selectCards(fresh, gameId, 1))
                                            .flatMap(cards -> {
                                                if (cards.isEmpty()) {
                                                    return redisTemplate.opsForSet().remove(key, botId).then();
                                                }

                                                Long cg;
                                                synchronized (gameLock) {
                                                    cg = currentGameId;
                                                }
                                                if (!Objects.equals(cg, gameId)) {
                                                    return redisTemplate.opsForSet().remove(key, botId).then();
                                                }

                                                if (!autoPlayManager.isEnabled() || !enabled) {
                                                    return redisTemplate.opsForSet().remove(key, botId).then();
                                                }

                                                return gameService.playerJoin(
                                                                roomId, gameId, botId,
                                                                fresh.getCapacity(),
                                                                fresh.getEntryFee(),
                                                                cards,
                                                                fresh.getAgentId(),
                                                                ParticipantType.BOT
                                                        )
                                                        .then(registerBot(botProfile, cards))
                                                        .onErrorResume(e -> {
                                                            log.warn("Room {} game {}: bot {} join failed: {}", roomId, gameId, botId, e.getMessage());
                                                            return redisTemplate.opsForSet().remove(key, botId).then();
                                                        });
                                            })
                                            .onErrorResume(e -> {
                                                log.warn("Room {} game {}: bot {} join pipeline error: {}", roomId, gameId, botId, e.getMessage());
                                                return redisTemplate.opsForSet().remove(key, botId).then();
                                            });
                                });
                    });
        }

        private Mono<Void> registerBot(UserProfile botProfile, List<String> cardIds) {
            String botId = botProfile.getTelegramId().toString();
            BotSession session = new BotSession(botProfile);

            for (String cardId : cardIds) {
                session.addCard(cardId);

                CardInfo cardInfo = roomCardPool.get(cardId);
                if (cardInfo == null) continue;

                for (Integer n : flattenCard(cardInfo.getNumbers())) {
                    if (n == null || n == 0) continue;
                    numberIndex.compute(n, (num, list) -> {
                        List<BotCardRef> out = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
                        out.add(new BotCardRef(botId, cardId));
                        return out;
                    });
                }
            }

            botSessions.put(botId, session);
            return Mono.empty();
        }

        private Mono<List<String>> selectCards(RoomInternalDto fresh, Long gameId, int count) {
            if (count <= 0) return Mono.just(Collections.emptyList());
            if (fresh.getCardPoolJson() == null || fresh.getCardPoolJson().isBlank()) {
                return Mono.just(Collections.emptyList());
            }

            List<String> poolCardIds = roomCardPool.keySet().stream().toList();
            if (poolCardIds.isEmpty()) return Mono.just(Collections.emptyList());

            String redisKey = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);

            List<String> shuffled = new ArrayList<>(poolCardIds);
            Collections.shuffle(shuffled);
            List<String> candidates = shuffled.stream()
                    .limit(Math.max(10, count * 5L))
                    .limit(shuffled.size())
                    .toList();

            return redisTemplate.opsForSet().members(redisKey)
                    .collectList()
                    .map(selectedList -> {
                        Set<String> selected = (selectedList == null) ? Set.of() : new HashSet<>(selectedList);
                        return candidates.stream()
                                .filter(c -> !selected.contains(c))
                                .limit(count)
                                .collect(Collectors.toList());
                    })
                    .onErrorResume(e -> {
                        log.warn("Room {} game {}: selectCards error: {}", roomId, gameId, e.getMessage());
                        return Mono.just(shuffled.stream().limit(count).collect(Collectors.toList()));
                    });
        }
    }

    /**
     * In-memory bot session state.
     */
    private static final class BotSession {
        private final UserProfile botProfile;
        private final String botTelegramId;

        // cardId -> marked numbers
        private final Map<String, Set<Integer>> markedByCard = new ConcurrentHashMap<>();

        BotSession(UserProfile botProfile) {
            this.botProfile = botProfile;
            this.botTelegramId = botProfile.getTelegramId().toString();
        }

        void addCard(String cardId) {
            markedByCard.putIfAbsent(cardId, ConcurrentHashMap.newKeySet());
        }

        boolean hasCard(String cardId) {
            return markedByCard.containsKey(cardId);
        }

        /**
         * @return true if newly marked; false if already marked or card missing
         */
        boolean mark(String cardId, Integer number) {
            Set<Integer> s = markedByCard.get(cardId);
            if (s == null) return false;
            return s.add(number);
        }

        /**
         * Immutable snapshot of marked numbers for verification/payload.
         */
        Set<Integer> markedSnapshot(String cardId) {
            Set<Integer> s = markedByCard.get(cardId);
            if (s == null || s.isEmpty()) return Set.of();
            return Set.copyOf(s);
        }
    }

    private record BotCardRef(String botTelegramId, String cardId) {
    }

    // ---------- helpers ----------

    private Map<String, CardInfo> parseCardPool(RoomInternalDto room) {
        if (room.getCardPoolJson() == null || room.getCardPoolJson().isBlank()) return Collections.emptyMap();
        try {
            List<CardInfo> pool = objectMapper.readValue(room.getCardPoolJson(), new TypeReference<>() {
            });
            return pool.stream()
                    .filter(c -> c.getCardId() != null)
                    .collect(Collectors.toConcurrentMap(CardInfo::getCardId, c -> c));
        } catch (Exception e) {
            log.warn("Failed to parse cardPoolJson for room {}: {}", room.getId(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> buildBingoPayload(Long gameId, CardInfo cardInfo, UserProfile botProfile, GamePattern pattern, Set<Integer> updatedMarked) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("gameId", gameId);
        payload.put("cardId", cardInfo.getCardId());
        payload.put("markedNumbers", new ArrayList<>(updatedMarked));
        payload.put("pattern", pattern.name());
        payload.put("userProfileId", botProfile.getId());
        payload.put("playerName", Constants.getRandomName());
        cardInfo.setMarked(updatedMarked);
        payload.put("card", cardInfo);
        return payload;
    }

    private List<Integer> flattenCard(Map<BingoColumn, List<Integer>> numbers) {
        if (numbers == null) return List.of();
        return numbers.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(n -> n != null && n != 0)
                .collect(Collectors.toList());
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse Redis message JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Long convertToLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer convertToInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
