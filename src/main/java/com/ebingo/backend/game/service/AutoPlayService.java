//package com.ebingo.backend.game.service;
//
//import com.ebingo.backend.game.dto.CardInfo;
//import com.ebingo.backend.game.entity.Room;
//import com.ebingo.backend.game.enums.BingoColumn;
//import com.ebingo.backend.game.enums.GamePattern;
//import com.ebingo.backend.game.repository.RoomRepository;
//import com.ebingo.backend.game.service.state.PlayerStateService;
//import com.ebingo.backend.game.state.GameState;
//import com.ebingo.backend.system.redis.RedisKeys;
//import com.ebingo.backend.user.entity.UserProfile;
//import com.ebingo.backend.user.repository.UserProfileRepository;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
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
//public class AutoPlayService {
//
//    private final RoomRepository roomRepository;
//    private final UserProfileRepository userProfileRepository;
//    private final GameService gameService;
//    private final PlayerStateService playerStateService;
//    private final BingoPatternVerifier patternVerifier;
//    private final ObjectMapper objectMapper;
//    private final ReactiveStringRedisTemplate redisTemplate;
//
//    private String activeBotsKey(Long roomId) {
//        return "room:" + roomId + ":activeBots";
//    }
//
//    // track per-room subscription disposables (if you want to cancel later)
//    private final Map<Long, Disposable> roomEndSubscriptions = new ConcurrentHashMap<>();
//
//    private final Random random = new Random();
//
//    private static final long JOIN_DELAY_MIN_MS = 500;   // 0.5s
//    private static final long JOIN_DELAY_MAX_MS = 5000;  // 5s
//    private static final long MARK_DELAY_MIN_MS = 300;   // 0.3s
//    private static final long MARK_DELAY_MAX_MS = 1000;  // 1.0s
//    private static final int JOIN_CONCURRENCY = 4;
//
//    // Cache: gameId -> cardId -> CardInfo
//    private final Map<Long, Map<String, CardInfo>> gameCardPoolCache = new ConcurrentHashMap<>();
//
//    @PostConstruct
//    public void initAutoPlay() {
//        roomRepository.findAll()
//                .filter(room -> Boolean.TRUE.equals(room.getBotAllowed()))
//                .doOnNext(this::setupRoomAutoPlay)
//                .subscribe(
//                        r -> {
//                        },
//                        err -> log.error("AutoPlay bootstrap failure", err),
//                        () -> log.info("AutoPlayService bootstrap complete")
//                );
//    }
//
//    /**
//     * Sets up per-room listeners and bootstraps bots for current game (if any).
//     * IMPORTANT: does not rely on the captured Room for later events — those will reload the Room.
//     */
//    private void setupRoomAutoPlay(Room room) {
//        Long roomId = room.getId();
//        String channel = RedisKeys.roomChannel(roomId);
//
//        // subscribe to channel and react to game.ended events by reloading Room and then starting bots
//        Disposable d = redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload)
//                .filter(evt -> "game.ended".equals(evt.get("type")))
//                .flatMap(evt -> {
//                    // reload the Room to pick up runtime changes (botAllowed, maxBots, cardPoolJson, pattern, etc.)
//                    return roomRepository.findById(roomId)
//                            .defaultIfEmpty(room)
//                            .flatMap(this::startBotsForCurrentGameIfAllowed);
//                })
//                .doOnError(e -> log.warn("Error in game.ended listener for room {}: {}", roomId, e.getMessage()))
//                .subscribe();
//
//        roomEndSubscriptions.put(roomId, d);
//
//        // bootstrap immediately for current or new game using fresh room from DB
//        roomRepository.findById(roomId)
//                .defaultIfEmpty(room)
//                .flatMap(this::startBotsForCurrentGameIfAllowed)
//                .doOnError(e -> log.warn("Bootstrap bots for room {} failed: {}", roomId, e.getMessage()))
//                .subscribe();
//    }
//
//    /**
//     * Start bots for the current game if room allows it; returns Mono<Void> that completes
//     * when the attempt to spawn is kicked off (not when bots finish).
//     */
//    private Mono<Void> startBotsForCurrentGameIfAllowed(Room room) {
//        if (!Boolean.TRUE.equals(room.getBotAllowed())) {
//            log.info("Bots not allowed for room {}, skipping", room.getId());
//            return Mono.empty();
//        }
//        int maxBots = Optional.ofNullable(room.getMaxBots()).orElse(0);
//        if (maxBots <= 0) {
//            log.info("Room {} has maxBots={} - nothing to spawn", room.getId(), maxBots);
//            return Mono.empty();
//        }
//
//        return gameService.getOrInitializeGame(room.getId(), "AUTO", room.getCapacity())
//                .flatMap(gs -> startBotsForGame(room, gs, maxBots));
//    }
//
//    /**
//     * Kick off bot spawning for a specific game. We immediately ensure the game-end cleanup listener
//     * is registered (so active-bots redis set is removed when the game ends), then attempt to spawn bots.
//     */
//    private Mono<Void> startBotsForGame(Room room, GameState gs, int maxBots) {
//        Long roomId = room.getId();
//        Long gameId = gs.getGameId();
//        String key = activeBotsKey(roomId);
//
//        // Install cleanup immediately so that even if spawning fails the activeBots set will be cleared at game end.
//        subscribeCleanupOnGameEnd(roomId, gameId);
//
//        return redisTemplate.opsForSet().size(key)
//                .map(sz -> sz == null ? 0L : sz)
//                .flatMap(currentActiveLong -> {
//                    int currentActive = currentActiveLong.intValue();
//                    int toSpawn = Math.max(0, maxBots - currentActive);
//                    if (toSpawn == 0) {
//                        log.info("Room {} game {} already has {} active bots (max {})", roomId, gameId, currentActive, maxBots);
//                        return Mono.empty();
//                    }
//
//                    log.info("Spawning {} bots for room {} game {}", toSpawn, roomId, gameId);
//
//                    return userProfileRepository.findBots(toSpawn, room.getId())
//                            .flatMap(botProfile -> tryReserveAndJoinBot(room, gs, botProfile), JOIN_CONCURRENCY)
//                            .then();
//                });
//    }
//
//    /**
//     * Attempts to reserve a spot for the bot and then join. If the join fails or doesn't happen,
//     * the reservation is removed so subsequent spawn attempts can use the bot.
//     */
//    private Mono<Void> tryReserveAndJoinBot(Room room, GameState gs, UserProfile botProfile) {
//        Long roomId = room.getId();
//        Long gameId = gs.getGameId();
//        String key = activeBotsKey(roomId);
//        String botTelegramId = botProfile.getTelegramId().toString();
//
//        return redisTemplate.opsForSet().isMember(key, botTelegramId)
//                .flatMap(isMember -> {
//                    if (Boolean.TRUE.equals(isMember)) {
//                        log.info("Bot {} already active in room {}, skipping", botTelegramId, roomId);
//                        return Mono.empty();
//                    }
//
//                    return redisTemplate.opsForSet().add(key, botTelegramId)
//                            .flatMap(addedCount -> {
//                                boolean added = addedCount != null && addedCount > 0;
//                                if (!added) {
//                                    log.info("Failed to reserve bot {} in room {} (race), skipping", botTelegramId, roomId);
//                                    return Mono.empty();
//                                }
//
//                                long delayMs = JOIN_DELAY_MIN_MS + (long) (random.nextDouble() * (JOIN_DELAY_MAX_MS - JOIN_DELAY_MIN_MS));
//
//                                return Mono.delay(Duration.ofMillis(delayMs))
//                                        .then(attemptJoinBotWithCardSelection(room, gs, botProfile))
//                                        .flatMap(joined -> {
//                                            if (Boolean.TRUE.equals(joined)) {
//                                                // keep reservation (bot is active)
//                                                return Mono.empty();
//                                            } else {
//                                                // release reservation if bot didn't join
//                                                return redisTemplate.opsForSet().remove(key, botTelegramId)
//                                                        .doOnSuccess(v -> log.info("Released reservation for bot {} in room {} because join didn't happen", botTelegramId, roomId))
//                                                        .then();
//                                            }
//                                        })
//                                        .onErrorResume(e -> {
//                                            // release reservation on any unexpected error
//                                            return redisTemplate.opsForSet().remove(key, botTelegramId)
//                                                    .doOnSuccess(v -> log.warn("Released reservation for bot {} after error in room {}: {}", botTelegramId, roomId, e.getMessage()))
//                                                    .then();
//                                        });
//                            });
//                });
//    }
//
//    /**
//     * Attempt to select cards and join the game. Returns Mono<Boolean> true if bot successfully joined and subscribed,
//     * false if bot did not join (no cards available or join failed).
//     */
//    private Mono<Boolean> attemptJoinBotWithCardSelection(Room room, GameState gs, UserProfile botProfile) {
//        String botUserTelegramId = botProfile.getTelegramId().toString();
//        Long botUserId = botProfile.getId();
//        Long roomId = room.getId();
//        Long gameId = gs.getGameId();
//
//        return trySelectCardsWithRetries(room, gameId, 1, 1)
//                .flatMap(selectedCardIds -> {
//                    if (selectedCardIds.isEmpty()) {
//                        log.info("Bot {} could not obtain cards for room {} game {}; skipping for this game", botUserId, roomId, gameId);
//                        return Mono.just(false);
//                    }
//
//                    return gameService.playerJoin(roomId, gameId, botUserTelegramId, room.getCapacity(), room.getEntryFee(), selectedCardIds)
//                            .then(subscribeBotToDrawnNumbers(room, gs, botProfile, selectedCardIds))
//                            .doOnSuccess(v -> log.info("Bot {} joined room {} game {} with cards {}", botUserId, roomId, gameId, selectedCardIds))
//                            .thenReturn(true)
//                            .onErrorResume(e -> {
//                                log.warn("Bot {} join failed in room {} game {}: {}", botUserId, roomId, gameId, e.getMessage());
//                                return Mono.just(false);
//                            });
//                })
//                .onErrorResume(e -> {
//                    log.warn("Error selecting cards for bot {} in room {} game {}: {}", botProfile.getId(), room.getId(), gs.getGameId(), e.getMessage());
//                    return Mono.just(false);
//                });
//    }
//
//    private Mono<List<String>> trySelectCardsWithRetries(Room room, Long gameId, int count, int attempt) {
//        if (attempt > 3) {
//            return Mono.just(Collections.emptyList());
//        }
//
//        List<Map<String, Object>> pool;
//        try {
//            if (room.getCardPoolJson() == null || room.getCardPoolJson().isBlank())
//                return Mono.just(Collections.emptyList());
//            pool = objectMapper.readValue(room.getCardPoolJson(), new TypeReference<List<Map<String, Object>>>() {
//            });
//        } catch (Exception e) {
//            log.warn("Failed to parse cardPoolJson for room {}: {}", room.getId(), e.getMessage());
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
//        // Check Redis set for already-selected cards
//        return Flux.fromIterable(candidate)
//                .flatMap(cardId ->
//                        redisTemplate.opsForSet().isMember(redisKey, cardId)
//                                .map(isMember -> isMember ? null : cardId)
//                )
//                .filter(Objects::nonNull)
//                .collectList()
//                .flatMap(availableCards -> {
//                    if (availableCards.size() == candidate.size()) {
//                        // All candidate cards are free
//                        return Mono.just(candidate);
//                    } else {
//                        log.info("Selected cards taken; retrying selection attempt {}/3 for room {} game {}", attempt, room.getId(), gameId);
//                        return trySelectCardsWithRetries(room, gameId, count, attempt + 1);
//                    }
//                })
//                .onErrorResume(e -> {
//                    log.warn("Redis check failed attempt {} for room {} game {}: {}", attempt, room.getId(), gameId, e.getMessage());
//                    if (attempt >= 3) return Mono.just(Collections.emptyList());
//                    return trySelectCardsWithRetries(room, gameId, count, attempt + 1);
//                });
//    }
//
//    /**
//     * Subscribes to drawn numbers (and game end) for a single bot; returns a Mono that completes when subscription ends.
//     */
//    private Mono<Void> subscribeBotToDrawnNumbers(Room room, GameState gs, UserProfile botProfile, List<String> cardIds) {
//        Long roomId = room.getId();
//        Long gameId = gs.getGameId();
//        GamePattern pattern = room.getPattern();
//        String channel = RedisKeys.roomChannel(roomId);
//
//        Mono<Void> gameEndSignal = redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload)
//                .filter(evt -> "game.ended".equals(evt.get("type")))
//                .filter(evt -> {
//                    Object payload = evt.get("payload");
//                    if (payload instanceof Map) {
//                        return Objects.equals(convertToLong(((Map<?, ?>) payload).get("gameId")), gameId);
//                    }
//                    return false;
//                })
//                .next()
//                .then()
//                .doOnSuccess(v -> {
//                    log.info("Game {} ended; bot {} will stop marking in room {}", gameId, botProfile.getId(), roomId);
//                    gameCardPoolCache.remove(gameId);
//                })
//                .onErrorResume(e -> {
//                    log.warn("Error waiting for game end for room {} game {}: {}", roomId, gameId, e.getMessage());
//                    return Mono.empty();
//                });
//
//        Flux<Integer> drawnNumberFlux = redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload)
//                .filter(evt -> "game.numberDrawn".equals(evt.get("type")))
//                .filter(evt -> {
//                    Object payload = evt.get("payload");
//                    if (payload instanceof Map) {
//                        return Objects.equals(convertToLong(((Map<?, ?>) payload).get("gameId")), gameId);
//                    }
//                    return false;
//                })
//                .map(evt -> {
//                    Object payload = evt.get("payload");
//                    if (payload instanceof Map) {
//                        return convertToInteger(((Map<?, ?>) payload).get("number"));
//                    }
//                    return null;
//                })
//                .filter(Objects::nonNull)
//                .delayElements(Duration.ofMillis(MARK_DELAY_MIN_MS + random.nextInt((int) (MARK_DELAY_MAX_MS - MARK_DELAY_MIN_MS))));
//
//        return drawnNumberFlux
//                .flatMap(drawnNumber -> Flux.fromIterable(cardIds)
//                        .flatMap(cardId -> attemptMarkAndClaim(room, gameId, botProfile, cardId, drawnNumber, pattern))
//                )
//                .takeUntilOther(gameEndSignal)
//                .then();
//    }
//
//    private Mono<Void> attemptMarkAndClaim(Room room,
//                                           Long gameId,
//                                           UserProfile botProfile,
//                                           String cardId,
//                                           Integer drawnNumber,
//                                           GamePattern pattern) {
//
//        // Fetch or parse the card pool for this game
//        Map<String, CardInfo> cardPool = gameCardPoolCache.computeIfAbsent(gameId, gid -> parseCardPool(room));
//
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
//
//                    if (!bingo) return Mono.empty();
//
//                    log.info("Bot {} claims bingo in room {} game {} on card {}", botProfile.getTelegramId(), room.getId(), gameId, cardId);
//
//                    Map<String, Object> payload = buildBingoPayload(gameId, cardInfo, botProfile, pattern, updatedMarked);
//                    return gameService.claimBingo(room.getId(), botProfile.getTelegramId().toString(), payload);
//                })
//                .onErrorResume(e -> {
//                    log.warn("Error bot {} marking number {} in room {} game {} card {}: {}",
//                            botProfile.getTelegramId(), drawnNumber, room.getId(), gameId, cardId, e.getMessage());
//                    return Mono.empty();
//                });
//    }
//
//    /**
//     * Parses room's card pool JSON into a map keyed by cardId.
//     */
//    private Map<String, CardInfo> parseCardPool(Room room) {
//        if (room.getCardPoolJson() == null || room.getCardPoolJson().isBlank()) {
//            return Collections.emptyMap();
//        }
//
//        try {
//            List<CardInfo> cardPool = objectMapper.readValue(
//                    room.getCardPoolJson(),
//                    new TypeReference<List<CardInfo>>() {
//                    }
//            );
//
//            return cardPool.stream()
//                    .filter(c -> c.getCardId() != null)
//                    .collect(Collectors.toConcurrentMap(CardInfo::getCardId, c -> c));
//        } catch (Exception e) {
//            log.warn("Failed to parse cardPoolJson for room {}: {}", room.getId(), e.getMessage());
//            return Collections.emptyMap();
//        }
//    }
//
//    /**
//     * Builds payload when a bot claims bingo.
//     */
//    private Map<String, Object> buildBingoPayload(Long gameId,
//                                                  CardInfo cardInfo,
//                                                  UserProfile botProfile,
//                                                  GamePattern pattern,
//                                                  Set<Integer> updatedMarked) {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("gameId", gameId);
//        payload.put("cardId", cardInfo.getCardId());
//        payload.put("markedNumbers", new ArrayList<>(updatedMarked));
//        payload.put("pattern", pattern.name());
//        payload.put("userProfileId", botProfile.getId()); // trusted bot ID
//        payload.put("playerName", botProfile.getFirstName());
//
//        cardInfo.setMarked(updatedMarked);
//        payload.put("card", cardInfo);
//
/// /        payload.put("card", Map.of(
/// /                "numbers", cardInfo.getNumbers(),
/// /                "cardId", cardInfo.getCardId(),
/// /                "marked", new ArrayList<>(updatedMarked)
/// /        ));
//        return payload;
//    }
//
//    /**
//     * Flatten card numbers into a single list of all non-zero values.
//     */
//    private List<Integer> flattenCard(Map<BingoColumn, List<Integer>> cardNumbers) {
//        return cardNumbers.values().stream()
//                .flatMap(List::stream)
//                .filter(Objects::nonNull)
//                .filter(n -> n != 0)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Subscribes a one-shot listener to game.ended for the given game and deletes the active-bots set.
//     * The listener is installed by calling listenToChannel(...).next() which terminates after first matching event.
//     */
//    private void subscribeCleanupOnGameEnd(Long roomId, Long gameId) {
//        String channel = RedisKeys.roomChannel(roomId);
//        String key = activeBotsKey(roomId);
//
//        redisTemplate.listenToChannel(channel)
//                .map(Message::getMessage)
//                .map(String::valueOf)
//                .map(this::parsePayload)
//                .filter(evt -> "game.ended".equals(evt.get("type")))
//                .filter(evt -> {
//                    Object payload = evt.get("payload");
//                    if (payload instanceof Map) {
//                        return Objects.equals(convertToLong(((Map<?, ?>) payload).get("gameId")), gameId);
//                    }
//                    return false;
//                })
//                .next()
//                .flatMap(evt -> {
//                    log.info("Game {} ended for room {} - clearing active bots set {}", gameId, roomId, key);
//                    return redisTemplate.delete(key).then();
//                })
//                .doOnError(e -> log.warn("Failed to cleanup active bots for room {} game {}: {}", roomId, gameId, e.getMessage()))
//                .subscribe();
//    }
//
//    /* ------------ helpers ------------ */
//
//    private Map<String, Object> parsePayload(String json) {
//        try {
//            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
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
//}