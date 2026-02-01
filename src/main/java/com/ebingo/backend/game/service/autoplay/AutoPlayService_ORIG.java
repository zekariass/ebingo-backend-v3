//package com.ebingo.backend.game.service.autoplay;
//
//import com.ebingo.backend.common.Constants;
//import com.ebingo.backend.game.dto.CardInfo;
//import com.ebingo.backend.game.dto.RoomInternalDto;
//import com.ebingo.backend.game.entity.Room;
//import com.ebingo.backend.game.enums.BingoColumn;
//import com.ebingo.backend.game.enums.GamePattern;
//import com.ebingo.backend.game.mappers.RoomMapper;
//import com.ebingo.backend.game.service.BingoPatternVerifier;
//import com.ebingo.backend.game.service.GameService;
//import com.ebingo.backend.game.service.RoomService;
//import com.ebingo.backend.game.service.state.PlayerStateService;
//import com.ebingo.backend.system.redis.RedisKeys;
//import com.ebingo.backend.user.entity.UserProfile;
//import com.ebingo.backend.user.repository.UserProfileRepository;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.connection.ReactiveSubscription.Message;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.stereotype.Service;
//import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AutoPlayService_ORIG {
//
//    //    private final RoomRepository roomRepository;
//    private final RoomService roomService;
//    private final UserProfileRepository userProfileRepository;
//    private final GameService gameService;
//    private final PlayerStateService playerStateService;
//    private final BingoPatternVerifier patternVerifier;
//    private final ObjectMapper objectMapper;
//    private final ReactiveStringRedisTemplate redisTemplate;
//    private final GlobalAutoPlayManager autoPlayManager;
//    private final MeterRegistry meterRegistry;
//
//    private final Random random = new Random();
//
//    private static final long JOIN_DELAY_MIN_MS = 500;
//    private static final long JOIN_DELAY_MAX_MS = 800;
//    private static final long MARK_DELAY_MIN_MS = 1;
//    private static final long MARK_DELAY_MAX_MS = 2;
//    private static final int JOIN_CONCURRENCY = 6; // soft cap
//    private static final Duration SPAWN_LOCK_TTL = Duration.ofSeconds(5);
//    private static final Duration BINGO_CLAIM_DELAY = Duration.ofMillis(3000);
//
//    // caches / subscriptions
//    private final Map<Long, Map<String, CardInfo>> gameCardPoolCache = new ConcurrentHashMap<>();
//    private final Map<Long, Disposable> roomSubscriptions = new ConcurrentHashMap<>();
//    private final Map<String, Disposable> botMarkSubscriptions = new ConcurrentHashMap<>();
//
//    // metrics (initialized in @PostConstruct)
//    private Counter spawnAttempts;
//    private Counter spawnSuccess;
//    private Counter spawnFailures;
//    private Timer spawnTimer;
//
//    private Counter cardSelectionRetries;
//    private Counter cardSelectionSuccess;
//
//    private Counter markingAttempts;
//    private Counter bingoClaims;
//    private Counter markingErrors;
//
//    private String activeBotsKey(Long roomId) {
//        return "room:" + roomId + ":activeBots";
//    }
//
//    private String spawnLockKey(Long roomId, Long gameId) {
//        return "room:" + roomId + ":game:" + gameId + ":spawnLock";
//    }
//
//    @PostConstruct
//    public void initMetricsAndAutoPlay() {
//        // initialize metrics using the injected MeterRegistry
//        spawnAttempts = Counter.builder("autoplay.bots.spawn.attempts").register(meterRegistry);
//        spawnSuccess = Counter.builder("autoplay.bots.spawn.success").register(meterRegistry);
//        spawnFailures = Counter.builder("autoplay.bots.spawn.failures").register(meterRegistry);
//        spawnTimer = Timer.builder("autoplay.bots.spawn.duration").register(meterRegistry);
//
//        cardSelectionRetries = Counter.builder("autoplay.cards.selection.retries").register(meterRegistry);
//        cardSelectionSuccess = Counter.builder("autoplay.cards.selection.success").register(meterRegistry);
//
//        markingAttempts = Counter.builder("autoplay.marking.attempts").register(meterRegistry);
//        bingoClaims = Counter.builder("autoplay.bingo.claims").register(meterRegistry);
//        markingErrors = Counter.builder("autoplay.marking.errors").register(meterRegistry);
//
//        // keep the original startup behavior (flush redis then bootstrap room subscriptions)
//        try {
//            redisTemplate.getConnectionFactory().getReactiveConnection()
//                    .serverCommands()
//                    .flushDb()
//                    .doOnSuccess(v -> log.info("Redis DB flushed on startup"))
//                    .doOnError(e -> log.warn("Failed to flush Redis on startup: {}", e.getMessage()))
//                    .block();
//        } catch (Exception e) {
//            log.warn("Exception flushing Redis on startup: {}", e.getMessage());
//        }
//
//        // Clear stale active bot reservations on startup
//        //roomRepository.findAll()
//        roomService.getAllRoomsWIthCardPoolForAutoService()
//                .map(RoomMapper::toEntity)
//                .filter(Room::getBotAllowed)
//                .flatMap(room -> redisTemplate.delete(activeBotsKey(room.getId())))
//                .subscribe(
//                        v -> {
//                        },
//                        e -> log.warn("Failed clearing active bots on startup: {}", e.getMessage()),
//                        () -> log.info("Startup: stale active bot reservations cleanup complete")
//                );
//
//        // Setup per-room subscriptions and bootstrap
//        roomService.getAllRoomsWIthCardPoolForAutoService()
//                .filter(RoomInternalDto::getBotAllowed)
//                .doOnNext(this::setupRoomAutoPlay)
//                .subscribe(
//                        r -> {
//                        },
//                        err -> log.error("AutoPlay bootstrap failure", err),
//                        () -> log.info("AutoPlayService bootstrap complete")
//                );
//
//        // watch global toggle
//        watchGlobalToggle();
//    }
//
//    private void setupRoomAutoPlay(RoomInternalDto room) {
//        Long roomId = room.getId();
//        String channel = RedisKeys.roomChannel(roomId);
//
//        // dispose previous if exists
//        Disposable prev = roomSubscriptions.get(roomId);
//        if (prev != null && !prev.isDisposed()) prev.dispose();
//
//        Disposable subscription = redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload)
//                .flatMap(evt -> {
//                    String type = (String) evt.get("type");
//                    Object payload = evt.get("payload");
//                    if (!(payload instanceof Map)) return Mono.empty();
//                    Map<?, ?> map = (Map<?, ?>) payload;
//
//                    Long payloadRoomId = convertToLong(map.get("roomId"));
//                    if (payloadRoomId != null && !Objects.equals(payloadRoomId, roomId)) {
//                        return Mono.empty();
//                    }
//
//                    if ("game.ended".equals(type)) {
//                        Long endedGameId = convertToLong(map.get("gameId"));
//                        log.info("Room {}: game {} ended — cleaning and preparing next game", roomId, endedGameId);
//
//                        return Mono.fromRunnable(() -> gameCardPoolCache.remove(endedGameId))
//                                .then(redisTemplate.delete(activeBotsKey(roomId)))
//                                .then(gameService.getOrInitializeGame(roomId, "AUTO", room.getCapacity(), room.getAgentId()))
//                                .flatMap(gs -> startBotsForGameIfAllowed(room, gs.getGameId()))
//                                .onErrorResume(e -> {
//                                    log.warn("Room {}: error respawning bots after game ended: {}", roomId, e.getMessage());
//                                    return Mono.empty();
//                                });
//                    }
//
//                    return Mono.empty();
//                })
//                .doOnError(e -> log.warn("Room {} subscription error: {}", roomId, e.getMessage()))
//                .subscribe();
//
//        roomSubscriptions.put(roomId, subscription);
//
//        // Bootstrap immediately for current/next game
//        startBotsForCurrentGameIfAllowed(room)
//                .doOnError(e -> log.warn("Room {} initial spawn failed: {}", roomId, e.getMessage()))
//                .subscribe();
//    }
//
//    private Mono<Void> startBotsForCurrentGameIfAllowed(RoomInternalDto room) {
//        return gameService.getOrInitializeGame(room.getId(), "AUTO", room.getCapacity(), room.getAgentId())
//                .flatMap(gs -> startBotsForGameIfAllowed(room, gs.getGameId()));
//    }
//
//    /**
//     * Reload room entity then attempt spawn with a Redis spawn lock to guarantee single spawn flow.
//     */
//    private Mono<Void> startBotsForGameIfAllowed(RoomInternalDto room, Long gameId) {
//        return roomService.getRoomWithCardPoolById(room.getId())
//                .defaultIfEmpty(room)
//                .flatMap(freshRoom -> {
//                    String lockKey = spawnLockKey(freshRoom.getId(), gameId);
//
//                    // Try setIfAbsent with TTL to acquire lock
//                    return redisTemplate.opsForValue().setIfAbsent(lockKey, "1", SPAWN_LOCK_TTL)
//                            .flatMap(acquired -> {
//                                if (!Boolean.TRUE.equals(acquired)) {
//                                    log.debug("Spawn lock already held for room {} game {}; skipping spawn", freshRoom.getId(), gameId);
//                                    return Mono.empty();
//                                }
//
//                                // track metrics with Timer.Sample
//                                Timer.Sample sample = Timer.start(meterRegistry);
//                                spawnAttempts.increment();
//
//                                return startBotsForGameInternal(freshRoom, gameId)
//                                        .doOnSuccess(v -> {
//                                            spawnSuccess.increment();
//                                            log.info("Room {} game {} spawn completed successfully", freshRoom.getId(), gameId);
//                                        })
//                                        .doOnError(e -> {
//                                            spawnFailures.increment();
//                                            log.warn("Room {} game {} spawn failed: {}", freshRoom.getId(), gameId, e.getMessage());
//                                        })
//                                        .doFinally(sig -> {
//                                            // release lock (best-effort)
//                                            redisTemplate.delete(lockKey)
//                                                    .subscribe(v -> log.debug("Released spawn lock for room {} game {}", freshRoom.getId(), gameId),
//                                                            e -> log.warn("Failed to release spawn lock for room {} game {}: {}", freshRoom.getId(), gameId, e.getMessage()));
//                                            sample.stop(spawnTimer);
//                                        });
//                            });
//                })
//                .onErrorResume(e -> {
//                    spawnFailures.increment();
//                    log.warn("Failed to attempt spawn for room {} game {}: {}", room.getId(), gameId, e.getMessage());
//                    return Mono.empty();
//                });
//    }
//
//    /**
//     * Original spawn logic (refreshed room) with dynamic join concurrency.
//     */
//    private Mono<Void> startBotsForGameInternal(RoomInternalDto room, Long gameId) {
//
//        if (!autoPlayManager.isEnabled()) {
//            log.info("Autoplay globally disabled - skipping spawn for room {}", room.getId());
//            return Mono.empty();
//        }
//
//        if (!Boolean.TRUE.equals(room.getBotAllowed())) return Mono.empty();
//
//        int minBots = Optional.ofNullable(room.getMinBots()).orElse(0);
//        int maxBots = Optional.ofNullable(room.getMaxBots()).orElse(0);
//
//        if (maxBots <= 0 || maxBots < minBots) {
//            log.warn("Room {} has invalid bot config: minBots={}, maxBots={}", room.getId(), minBots, maxBots);
//            return Mono.empty();
//        }
//
//        int targetBots = minBots + (int) (Math.random() * (maxBots - minBots + 1));
//        String key = activeBotsKey(room.getId());
//
//        subscribeCleanupOnGameEnd(room.getId(), gameId);
//
//        return redisTemplate.opsForSet().size(key)
//                .map(sz -> sz == null ? 0L : sz)
//                .flatMap(currentActiveLong -> {
//                    int currentActive = currentActiveLong.intValue();
//                    int toSpawn = Math.max(0, targetBots - currentActive);
//                    if (toSpawn <= 0) {
//                        log.info("Room {} game {}: already has {} bots; target={} — no spawn needed", room.getId(), gameId, currentActive, targetBots);
//                        return Mono.empty();
//                    }
//
//                    int cpu = Math.max(1, Runtime.getRuntime().availableProcessors());
//                    int joinConcurrency = Math.max(1, Math.min(JOIN_CONCURRENCY, Math.min(toSpawn, cpu * 2)));
//
//                    log.info("Room {} game {} spawning {} bots (target={}, active={}), joinConcurrency={}",
//                            room.getId(), gameId, toSpawn, targetBots, currentActive, joinConcurrency);
//
//                    return userProfileRepository.findBotsByRoom(room.getId())
//                            .take(toSpawn)
//                            .flatMap(bot -> tryReserveAndJoinBot(room, gameId, bot), joinConcurrency)
//                            .then();
//                });
//    }
//
//    /**
//     * Reserve bot in the room's active set then subscribe and join.
//     * Reservation is released if join fails or error occurs.
//     */
//    private Mono<Void> tryReserveAndJoinBot(RoomInternalDto room, Long gameId, UserProfile botProfile) {
//        String key = activeBotsKey(room.getId());
//        String botId = Objects.toString(botProfile.getTelegramId(), null);
//        if (botId == null) return Mono.empty();
//
//        return redisTemplate.opsForSet().isMember(key, botId)
//                .flatMap(isMember -> {
//                    if (Boolean.TRUE.equals(isMember)) {
//                        log.debug("Bot {} already active/reserved in room {}, skipping", botId, room.getId());
//                        return Mono.empty();
//                    }
//
//                    return redisTemplate.opsForSet().add(key, botId)
//                            .flatMap(added -> {
//                                boolean reserved = added != null && added > 0;
//                                if (!reserved) {
//                                    log.debug("Failed to reserve bot {} in room {} (race)", botId, room.getId());
//                                    return Mono.empty();
//                                }
//
//                                long delayMs = JOIN_DELAY_MIN_MS + (long) (random.nextDouble() * (JOIN_DELAY_MAX_MS - JOIN_DELAY_MIN_MS));
//                                return Mono.delay(Duration.ofMillis(delayMs))
//                                        .then(subscribeAndJoin(room, gameId, botProfile))
//                                        .flatMap(joined -> {
//                                            if (Boolean.TRUE.equals(joined)) {
//                                                log.debug("Bot {} joined room {} game {}", botProfile.getTelegramId(), room.getId(), gameId);
//                                                return Mono.empty(); // keep reservation
//                                            } else {
//                                                // release reservation if join didn't happen
//                                                return redisTemplate.opsForSet().remove(key, botId)
//                                                        .doOnSuccess(v -> log.debug("Released reservation for bot {} in room {}", botId, room.getId()))
//                                                        .then();
//                                            }
//                                        })
//                                        .onErrorResume(e -> redisTemplate.opsForSet().remove(key, botId)
//                                                .doOnSuccess(v -> log.warn("Released reservation for bot {} after error in room {}: {}", botId, room.getId(), e.getMessage()))
//                                                .then());
//                            });
//                });
//    }
//
//    /**
//     * Subscribe first (so we don't miss any published draw events), then attempt to join.
//     * Returns Mono<Boolean> true if join succeeded, false otherwise.
//     */
//    private Mono<Boolean> subscribeAndJoin(RoomInternalDto room, Long gameId, UserProfile botProfile) {
//        return trySelectCardsWithRetries(room, gameId, 1, 1)
//                .flatMap(cards -> {
//                    if (cards.isEmpty()) return Mono.just(false);
//
//                    Disposable sub = createMarkingSubscription(room, gameId, botProfile, cards);
//                    String botKey = botProfile.getTelegramId().toString() + ":" + gameId;
//                    botMarkSubscriptions.put(botKey, sub);
//
//                    return gameService.playerJoin(room.getId(), gameId, botProfile.getTelegramId().toString(),
//                                    room.getCapacity(), room.getEntryFee(), cards, room.getAgentId())
//                            .thenReturn(true)
//                            .onErrorResume(e -> {
//                                log.warn("Bot {} failed to join room {} game {}: {}", botProfile.getTelegramId(), room.getId(), gameId, e.getMessage());
//                                try {
//                                    sub.dispose();
//                                } catch (Exception ignored) {
//                                }
//                                botMarkSubscriptions.remove(botKey);
//                                return Mono.just(false);
//                            });
//                })
//                .onErrorResume(e -> {
//                    log.warn("Error preparing subscribeAndJoin for bot {} room {} game {}: {}", botProfile.getTelegramId(), room.getId(), gameId, e.getMessage());
//                    return Mono.just(false);
//                });
//    }
//
//    /**
//     * Create per-bot, per-game marking subscription.
//     */
//    private Disposable createMarkingSubscription(RoomInternalDto room, Long gameId, UserProfile botProfile, List<String> cardIds) {
//        String channel = RedisKeys.roomChannel(room.getId());
//        String botKey = botProfile.getTelegramId().toString() + ":" + gameId;
//
//        Flux<Map<String, Object>> messages = redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload);
//
//        Mono<Void> stopSignal = messages
//                .filter(m -> "game.ended".equals(m.get("type")))
//                .map(m -> m.get("payload"))
//                .filter(payload -> payload instanceof Map)
//                .map(payload -> (Map<?, ?>) payload)
//                .filter(map -> Objects.equals(convertToLong(map.get("gameId")), gameId)
//                        && Objects.equals(convertToLong(map.get("roomId")), room.getId()))
//                .next()
//                .then()
//                .doOnSuccess(v -> {
//                    log.info("Bot {} - game {} ended in room {}; disposing marking subscription", botProfile.getTelegramId(), gameId, room.getId());
//                    gameCardPoolCache.remove(gameId);
//                    botMarkSubscriptions.remove(botKey);
//                });
//
//        Flux<Integer> drawnNumbers = messages
//                .filter(m -> "game.numberDrawn".equals(m.get("type")))
//                .map(m -> m.get("payload"))
//                .filter(payload -> payload instanceof Map)
//                .map(payload -> (Map<?, ?>) payload)
//                .filter(map -> Objects.equals(convertToLong(map.get("gameId")), gameId)
//                        && Objects.equals(convertToLong(map.get("roomId")), room.getId()))
//                .map(map -> convertToInteger(map.get("number")))
//                .filter(Objects::nonNull)
//                .delayElements(Duration.ofMillis(MARK_DELAY_MIN_MS + random.nextInt((int) (MARK_DELAY_MAX_MS - MARK_DELAY_MIN_MS))));
//
//        return drawnNumbers
//                .flatMap(num -> {
//                    if (!autoPlayManager.isEnabled()) return Mono.empty();
//                    markingAttempts.increment();
//                    return Flux.fromIterable(cardIds)
//                            .flatMap(cardId -> attemptMarkAndClaim(room, gameId, botProfile, cardId, num, room.getPattern()));
//                })
//                .takeUntilOther(stopSignal)
//                .subscribe(
//                        v -> {
//                        },
//                        err -> {
//                            markingErrors.increment();
//                            log.warn("Marking subscription error bot {} game {}: {}", botProfile.getTelegramId(), gameId, err.getMessage());
//                        },
//                        () -> log.debug("Marking subscription completed for bot {} game {}", botProfile.getTelegramId(), gameId)
//                );
//    }
//
//    /**
//     * Select cards by checking the room's cardPoolJson and Redis selected set; retry up to 3 times.
//     * Optimized to fetch the Redis selected set once per attempt and filter locally.
//     */
//    private Mono<List<String>> trySelectCardsWithRetries(RoomInternalDto room, Long gameId, int count, int attempt) {
//        if (attempt > 3) return Mono.just(Collections.emptyList());
//
//        List<Map<String, Object>> pool;
//        try {
//            if (room.getCardPoolJson() == null || room.getCardPoolJson().isBlank()) {
//                return Mono.just(Collections.emptyList());
//            }
//            pool = objectMapper.readValue(room.getCardPoolJson(), new TypeReference<>() {
//            });
//        } catch (Exception e) {
//            log.warn("Room {}: failed to parse cardPoolJson: {}", room.getId(), e.getMessage());
//            return Mono.just(Collections.emptyList());
//        }
//
//        Collections.shuffle(pool);
//        List<String> candidate = pool.stream()
//                .map(m -> Objects.toString(m.get("cardId"), null))
//                .filter(Objects::nonNull)
//                .limit(count)
//                .collect(Collectors.toList());
//
//        if (candidate.isEmpty()) return Mono.just(Collections.emptyList());
//
//        String redisKey = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);
//
//        return redisTemplate.opsForSet().members(redisKey)
//                .collectList()
//                .flatMap(selectedList -> {
//                    Set<String> selected = selectedList == null ? Collections.emptySet() : new HashSet<>(selectedList);
//                    List<String> available = candidate.stream()
//                            .filter(cardId -> !selected.contains(cardId))
//                            .toList();
//
//                    if (available.size() == candidate.size()) {
//                        cardSelectionSuccess.increment();
//                        return Mono.just(candidate);
//                    }
//
//                    cardSelectionRetries.increment();
//                    return trySelectCardsWithRetries(room, gameId, count, attempt + 1);
//                })
//                .onErrorResume(e -> {
//                    log.warn("Error checking selected cards for game {} room {}: {}", gameId, room.getId(), e.getMessage());
//                    cardSelectionRetries.increment();
//                    return trySelectCardsWithRetries(room, gameId, count, attempt + 1);
//                });
//    }
//
//    /**
//     * Attempt to mark and claim if pattern satisfied.
//     */
//    private Mono<Void> attemptMarkAndClaim(RoomInternalDto room, Long gameId, UserProfile botProfile, String cardId, Integer drawnNumber, GamePattern pattern) {
//        Map<String, CardInfo> cardPool = gameCardPoolCache.computeIfAbsent(gameId, gid -> parseCardPool(room));
//        CardInfo cardInfo = cardPool.get(cardId);
//        if (cardInfo == null) return Mono.empty();
//
//        List<Integer> flatNumbers = flattenCard(cardInfo.getNumbers());
//        if (!flatNumbers.contains(drawnNumber)) return Mono.empty();
//
//        return gameService.markNumber(room.getId(), gameId, botProfile.getTelegramId().toString(),
//                        Map.of("number", drawnNumber, "cardId", cardId))
//                .then(playerStateService.getMarkedNumbers(gameId, botProfile.getTelegramId().toString(), cardId))
//                .flatMap(updatedMarked -> {
//                    boolean bingo = patternVerifier.verifyPattern(cardInfo.getNumbers(), updatedMarked, pattern.name());
//                    if (!bingo) return Mono.empty();
//
//                    Map<String, Object> payload = buildBingoPayload(gameId, cardInfo, botProfile, pattern, updatedMarked);
//                    bingoClaims.increment();
/// /                    return gameService.claimBingo(room.getId(), botProfile.getTelegramId().toString(), payload);
//                    return Mono.just(payload)
//                            .delayElement(BINGO_CLAIM_DELAY)   // 👈 1 second delay
//                            .flatMap(p ->
//                                    gameService.claimBingo(
//                                            room.getId(),
//                                            botProfile.getTelegramId().toString(),
//                                            p,
//                                            room.getAgentId())
//                            );
//                })
//                .onErrorResume(e -> {
//                    markingErrors.increment();
//                    log.warn("Bot {} marking error in room {} game {}: {}", botProfile.getTelegramId(), room.getId(), gameId, e.getMessage());
//                    return Mono.empty();
//                });
//    }
//
//    private Map<String, CardInfo> parseCardPool(RoomInternalDto room) {
//        if (room.getCardPoolJson() == null || room.getCardPoolJson().isBlank()) return Collections.emptyMap();
//        try {
//            List<CardInfo> pool = objectMapper.readValue(room.getCardPoolJson(), new TypeReference<>() {
//            });
//            return pool.stream().filter(c -> c.getCardId() != null)
//                    .collect(Collectors.toConcurrentMap(CardInfo::getCardId, c -> c));
//        } catch (Exception e) {
//            log.warn("Failed to parse cardPoolJson for room {}: {}", room.getId(), e.getMessage());
//            return Collections.emptyMap();
//        }
//    }
//
//    private Map<String, Object> buildBingoPayload(Long gameId, CardInfo cardInfo, UserProfile botProfile, GamePattern pattern, Set<Integer> updatedMarked) {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("gameId", gameId);
//        payload.put("cardId", cardInfo.getCardId());
//        payload.put("markedNumbers", new ArrayList<>(updatedMarked));
//        payload.put("pattern", pattern.name());
//        payload.put("userProfileId", botProfile.getId());
//        payload.put("playerName", Constants.getRandomName());
//        cardInfo.setMarked(updatedMarked);
//        payload.put("card", cardInfo);
//        return payload;
//    }
//
//    private List<Integer> flattenCard(Map<BingoColumn, List<Integer>> numbers) {
//        return numbers.values().stream().flatMap(List::stream).filter(n -> n != null && n != 0).collect(Collectors.toList());
//    }
//
//    private void subscribeCleanupOnGameEnd(Long roomId, Long gameId) {
//        String key = activeBotsKey(roomId);
//        String channel = RedisKeys.roomChannel(roomId);
//
//        redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload)
//                .filter(evt -> "game.ended".equals(evt.get("type")))
//                .filter(evt -> {
//                    Object payload = evt.get("payload");
//                    if (payload instanceof Map) {
//                        Map<?, ?> map = (Map<?, ?>) payload;
//                        return Objects.equals(convertToLong(map.get("gameId")), gameId)
//                                && Objects.equals(convertToLong(map.get("roomId")), roomId);
//                    }
//                    return false;
//                })
//                .next()
//                .flatMap(evt -> redisTemplate.delete(key))
//                .subscribe(
//                        v -> log.debug("Cleared activeBots for room {} game {}", roomId, gameId),
//                        e -> log.warn("Failed to cleanup active bots for room {} game {}: {}", roomId, gameId, e.getMessage())
//                );
//    }
//
//    private Map<String, Object> parsePayload(String json) {
//        try {
//            return objectMapper.readValue(json, new TypeReference<>() {
//            });
//        } catch (Exception e) {
//            log.warn("Failed to parse Redis message JSON: {}", e.getMessage());
//            return Collections.emptyMap();
//        }
//    }
//
//    private Long convertToLong(Object o) {
//        if (o == null) return null;
//        if (o instanceof Number) return ((Number) o).longValue();
//        try {
//            return Long.parseLong(o.toString());
//        } catch (Exception ignored) {
//            return null;
//        }
//    }
//
//    private Integer convertToInteger(Object o) {
//        if (o == null) return null;
//        if (o instanceof Number) return ((Number) o).intValue();
//        try {
//            return Integer.parseInt(o.toString());
//        } catch (Exception ignored) {
//            return null;
//        }
//    }
//
//    private void watchGlobalToggle() {
//        autoPlayManager.events()
//                .subscribe(evt -> {
//                    if (!evt.isEnabled()) {
//                        log.warn("AUTOPLAY DISABLED — {}", evt.getReason());
//
//                        // stop all marking subscriptions
//                        botMarkSubscriptions.values().forEach(Disposable::dispose);
//                        botMarkSubscriptions.clear();
//
//                        // clear all activeBots
//                        roomService.getAllRoomsWIthCardPoolForAutoService()
//                                .filter(RoomInternalDto::getBotAllowed)
//                                .flatMap(room -> redisTemplate.delete(activeBotsKey(room.getId())))
//                                .subscribe();
//
//                    } else {
//                        log.warn("AUTOPLAY ENABLED — {}", evt.getReason());
//
//                        roomService.getAllRoomsWIthCardPoolForAutoService()
//                                .filter(RoomInternalDto::getBotAllowed)
//                                .flatMap(room ->
//                                        gameService.getOrInitializeGame(room.getId(), "AUTO", room.getCapacity(), room.getAgentId())
//                                                .flatMap(gs -> startBotsForGameIfAllowed(room, gs.getGameId()))
//                                )
//                                .subscribe();
//                    }
//                });
//    }
//}