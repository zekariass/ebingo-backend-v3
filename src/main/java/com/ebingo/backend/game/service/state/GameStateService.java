package com.ebingo.backend.game.service.state;

import com.ebingo.backend.game.enums.GameStatus;
import com.ebingo.backend.game.mappers.GameMapper;
import com.ebingo.backend.game.repository.GameRepository;
import com.ebingo.backend.game.repository.RoomRepository;
import com.ebingo.backend.game.service.CardPoolService;
import com.ebingo.backend.game.service.RedisPublisher;
import com.ebingo.backend.game.service.sync.RoomRegistry;
import com.ebingo.backend.game.state.GameState;
import com.ebingo.backend.system.redis.RedisKeys;
import com.ebingo.backend.system.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameStateService {

    private final ReactiveRedisTemplate<String, Object> redis;
    private final ReactiveHashOperations<String, String, Object> hashOps;
    private final ReactiveSetOperations<String, String> setOps;
    private final CardPoolService cardPoolService;
    private final PlayerStateService playerStateService; // Add this dependency
    private final RedissonReactiveClient redissonReactiveClient;
    private final RedisPublisher publisher;
    private final GameRepository repository;
    private final SystemConfigService systemConfigService;
    //    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;
    private final RoomRegistry roomRegistry;
//    private final RoomStateService roomStateService;

    @Value("${game.stale.ttlInMinutes:5}")
    private Integer gameStaleTTLInMinutes;

    private static final Duration GAME_STATE_TTL = Duration.ofHours(24);


    @PostConstruct
    public void cleanUpGameStatesOnStartup() {
        roomRepository.findAll()
                .flatMap(room -> deleteGameState(room.getId(), room.getAgentId()))
                .doOnNext(result -> log.info("Deleted game state result: {}", result))
                .doOnError(e -> log.error("Cleanup failed", e))
                .doOnComplete(() -> log.info("Cleanup complete"))
                .subscribe();
    }


    // ----------------------------
    // Initialize Game
    // ----------------------------
    public Mono<GameState> getOrInitializeGame(Long roomId, String userId, Integer capacity, Long agentId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        return redis.hasKey(gameKey)
                .flatMap(exists -> {
                    if (exists) {
                        return getGameState(roomId, agentId)
                                .flatMap(gs -> {
                                    if (gs.isEnded() || gs.getStatus().equals(GameStatus.COMPLETED)) {
                                        return deleteGameState(roomId, agentId)
                                                .then(initializeGameWithLock(roomId, userId, capacity, agentId));
                                    } else if ((gs.getStatus().equals(GameStatus.PLAYING) || gs.getStatus().equals(GameStatus.COUNTDOWN)) && gs.getStatusUpdatedAt().isBefore(Instant.now().minus(Duration.ofMinutes(gameStaleTTLInMinutes)))) {
                                        return deleteGameState(roomId, agentId)
                                                .then(initializeGameWithLock(roomId, userId, capacity, agentId));
                                    }

                                    return Mono.just(gs);
                                })
                                .flatMap(gameState -> playerStateService.getPlayerCardIds(gameState.getGameId(), userId)
                                        .flatMap(uc -> {
                                            gameState.setUserSelectedCardsIds(uc);
                                            return Mono.just(gameState);
                                        }));
                    } else {
                        return deleteGameState(roomId, agentId)
                                .then(initializeGameWithLock(roomId, userId, capacity, agentId));
                    }
                });
    }


    public Mono<GameState> initializeGameWithLock(Long roomId, String userId, Integer capacity, Long agentId) {
        return initializeGameWithLockWithRetry(roomId, userId, capacity, agentId)
                .flatMap(gs ->
                        playerStateService.getPlayerCardIds(gs.getGameId(), userId)
                                .defaultIfEmpty(Set.of())
                                .map(userCardIds -> {
                                    gs.setUserSelectedCardsIds(userCardIds);
                                    return gs;
                                })
                );
    }


    private Mono<GameState> initializeGameWithLockWithRetry(Long roomId, String userId, Integer capacity, Long agentId) {
        String lockKey = RedisKeys.gameInitLockKey(roomId);
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);
        return Mono.defer(() ->
                                lock.tryLock(0, 30, TimeUnit.SECONDS)
                                        .timeout(Duration.ofSeconds(10)) // safeguard if Redis hangs
                                        .flatMap(isLocked -> {
                                            if (Boolean.TRUE.equals(isLocked)) {
                                                // Only one instance will enter this block
                                                return Mono.usingWhen(
                                                        Mono.just(lock),
                                                        l -> initializeGame(roomId, capacity, agentId)
                                                                .flatMap(gs -> saveGameStateToDb(gs, agentId))
//                                                        .flatMap(this::saveGameStateToDb)
                                                                .flatMap(gsFromDb ->
                                                                        cardPoolService.getCurrentPool(roomId)
                                                                                .flatMap(pool ->
                                                                                        saveGameStateToRedis(gsFromDb, roomId, agentId)
                                                                                                .thenReturn(gsFromDb)
                                                                                )
                                                                ),
                                                        l -> l.unlock().onErrorResume(e -> Mono.empty())
                                                );
                                            } else {
                                                log.debug("Game init lock busy for room {} — will retry shortly", roomId);
                                                return Mono.error(new IllegalStateException("LOCK_BUSY"));
                                            }
                                        })
                )
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(200))       // retry up to 3 times
                                .maxBackoff(Duration.ofSeconds(2))         // exponential delay up to 2s
                                .filter(e ->
                                        e instanceof IllegalStateException && "LOCK_BUSY".equals(e.getMessage())
                                                || e instanceof TimeoutException
                                )
                                .onRetryExhaustedThrow((spec, signal) ->
                                        new RuntimeException("Failed to acquire initialization lock after retries"))
                )
                .onErrorResume(e -> {
                    // handle final failure gracefully
                    log.warn("Game initialization failed for room {}: {}", roomId, e.toString());
                    publisher.publishUserEvent(userId, Map.of(
                            "type", "error",
                            "payload", Map.of("message", "Failed to initialize game after retries")
                    ));
                    return checkAndGetExistingGameState(roomId, capacity, agentId);
                });
    }


    /**
     * Checks Redis for an existing game state.
     * If found, it returns the state; otherwise, it throws an error or returns an empty Mono.
     */
    private Mono<GameState> checkAndGetExistingGameState(Long roomId, int capacity, Long agentId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        // Check if the key exists in Redis
        return redis.hasKey(gameKey)
                .flatMap(hasKey -> {
                    if (Boolean.TRUE.equals(hasKey)) {
                        return getGameState(roomId, agentId); // gameId and capacity can be fetched or passed as needed
                    } else {
                        // The game has not been initialized yet, and we couldn't get the lock.
                        // This is an edge case that might require retrying or an error response.
                        log.error("Game {} is not yet initialized. Please retry.", roomId);
                        return Mono.empty();
                    }
                });
    }


    public Mono<GameState> initializeGame(Long roomId, Integer capacity, Long agentId) {
        // 1. Initialize GameState object
        GameState gameState = new GameState();
        gameState.setRoomId(roomId);
        gameState.setStarted(false);
        gameState.setEnded(false);
        gameState.setStatus(GameStatus.READY);
        gameState.setStopNumberDrawing(false);
        gameState.setClaimRequested(false);
        gameState.setCountdownEndTime(null);
        gameState.setStatusUpdatedAt(Instant.now());


        gameState.setDrawnNumber(new LinkedHashSet<>());
        gameState.setJoinedPlayers(Set.of());
        gameState.setUserSelectedCardsIds(Set.of());
        gameState.setAllSelectedCardsIds(Set.of());
        gameState.setBackendEpochMillis(0L);

        gameState.setAgentId(agentId);

//        return Mono.zip(
//                systemConfigService.getSystemConfig("COMMISSION_RATE"),
//                roomRepository.findById(roomId)
//        ).flatMap(tuple -> {
//            var commissionConfig = tuple.getT1();
//            log.info("TotalCommission rate: {}", commissionConfig);
//            log.info("ROOM: {}", tuple.getT2());
//            gameState.setCommissionRate(
//                    Double.parseDouble(commissionConfig.getValue())
//            );
//            gameState.setEntryFee(tuple.getT2().getEntryFee().doubleValue());
//            gameState.setCapacity(tuple.getT2().getCapacity());
//
//            // Add room to room registry for sync
//            // Add room to room registry for sync, reactive chaining
//            log.info("Adding room {} to room registry. rooms in registry: {}", roomId, roomRegistry.getActiveRooms());
//            return roomRegistry.addRoom(roomId)
//                    .thenReturn(gameState);
//        });

        return roomRepository.findById(roomId)
                .flatMap(room -> {
                    log.info("TotalCommission rate: {}", room.getCommissionRate());
//                    log.info("ROOM: {}", room);
                    gameState.setCommissionRate(
                            room.getCommissionRate().doubleValue()
                    );
                    gameState.setEntryFee(room.getEntryFee().doubleValue());
                    gameState.setCapacity(room.getCapacity());

                    // Add room to room registry for sync
                    // Add room to room registry for sync, reactive chaining
//                    log.info("Adding room {} to room registry. rooms in registry: {}", roomId, roomRegistry.getActiveRooms());
                    return roomRegistry.addRoom(roomId, agentId)
                            .thenReturn(gameState);
                });

    }

    // DB save logic
    private Mono<GameState> saveGameStateToDb(GameState gameState, Long agentId) {

        return Mono.fromCallable(() -> GameMapper.toEntity(gameState, objectMapper))
                .flatMap(gs -> {
                    gs.setStartedAt(LocalDateTime.now());
                    gs.setAgentId(agentId);
                    return repository.save(gs);
                })
                .map(savedGame -> {
                    gameState.setGameId(savedGame.getId());
                    gameState.setAgentId(savedGame.getAgentId());
                    return gameState;
                });
    }


    public Mono<Boolean> saveGameStateToRedis(GameState gameState, Long roomId, Long agentId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        Instant countdownEndTime = gameState.getCountdownEndTime();

        Map<String, Object> gameData = new HashMap<>();
        gameData.put("gameId", String.valueOf(gameState.getGameId()));
        gameData.put("roomId", String.valueOf(gameState.getRoomId()));
        gameData.put("started", String.valueOf(gameState.isStarted()));
        gameData.put("ended", String.valueOf(gameState.isEnded()));
        gameData.put("status", gameState.getStatus().name());
        gameData.put("stopNumberDrawing", String.valueOf(gameState.getStopNumberDrawing()));
        gameData.put("claimRequested", gameState.getClaimRequested());
        gameData.put("entryFee", String.valueOf(gameState.getEntryFee()));
        gameData.put("commissionRate", String.valueOf(gameState.getCommissionRate()));
        gameData.put("capacity", String.valueOf(gameState.getCapacity()));
        gameData.put("statusUpdatedAt", gameState.getStatusUpdatedAt().toString());
        gameData.put("agentId", String.valueOf(agentId));

        // Add countdownEndTime only if present
        if (countdownEndTime != null) {
            gameData.put("countdownEndTime", countdownEndTime.toString());
        }

        return hashOps.putAll(gameKey, gameData)
                .then(redis.expire(gameKey, GAME_STATE_TTL))
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("Failed to save game state for game {}: {}", gameState.getGameId(), e.getMessage(), e);
                    return Mono.just(false);
                });
    }

    // ----------------------------
    // Add Drawn Number (delta)
    // ----------------------------


    public Mono<LinkedHashSet<Integer>> addOrInitDrawnNumber(Long gameId, int number) {
        String drawnKey = RedisKeys.gameDrawnNumbersKey(gameId);

        return redis.hasKey(drawnKey)
                .flatMap(exists -> {
                    Mono<Boolean> addNumber = setOps.add(drawnKey, String.valueOf(number))
                            .thenReturn(true)
                            .onErrorResume(e -> {
                                log.error("Failed to add drawn number {} for game {}: {}", number, gameId, e.getMessage(), e);
                                return Mono.just(false);
                            });

                    if (!exists) {
                        // First time: add + set TTL
                        return addNumber.then(redis.expire(drawnKey, GAME_STATE_TTL))
                                .thenMany(setOps.members(drawnKey))
                                .map(obj -> Integer.valueOf((String) obj))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                    } else {
                        // Just add
                        return addNumber
                                .thenMany(setOps.members(drawnKey))
                                .map(obj -> Integer.valueOf((String) obj))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                    }
                });

    }

    public Mono<LinkedHashSet<Integer>> getDrawnNumbers(Long gameId) {
        String drawnKey = RedisKeys.gameDrawnNumbersKey(gameId);
        return setOps.members(drawnKey)
                .map(obj -> Integer.valueOf((String) obj))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .onErrorResume(e -> Mono.just(new LinkedHashSet<>()));
    }


    public Mono<GameState> getGameState(Long roomId, Long agentId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        return hashOps.entries(gameKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(gameMeta -> {
                    if (gameMeta.isEmpty()) {
                        return Mono.empty();
                    }

                    GameState state = new GameState();

                    Object gameIdObj = gameMeta.get("gameId");
                    if (gameIdObj == null || "null".equals(gameIdObj.toString())) {
                        return Mono.empty();
                    }

                    state.setGameId(Long.valueOf(gameMeta.get("gameId").toString()));
                    state.setRoomId(Long.valueOf(gameMeta.get("roomId").toString()));
                    state.setStatus(GameStatus.valueOf(gameMeta.get("status").toString()));
                    state.setStarted(Boolean.parseBoolean(gameMeta.get("started").toString()));
                    state.setEnded(Boolean.parseBoolean(gameMeta.get("ended").toString()));
                    state.setStopNumberDrawing(Boolean.parseBoolean(gameMeta.get("stopNumberDrawing").toString()));
                    state.setClaimRequested(Boolean.parseBoolean(gameMeta.get("claimRequested").toString()));
                    state.setEntryFee(gameMeta.get("entryFee") != null ? Double.parseDouble(gameMeta.get("entryFee").toString()) : 0.0);
                    state.setCommissionRate(gameMeta.get("commissionRate") != null ? Double.parseDouble(gameMeta.get("commissionRate").toString()) : 0.0);
                    state.setCapacity(gameMeta.get("capacity") != null ? Integer.parseInt(gameMeta.get("capacity").toString()) : 0);
                    state.setAgentId(gameMeta.get("agentId") != null ? Long.parseLong(gameMeta.get("agentId").toString()) : null);
                    
                    Object statusUpdatedAtRaw = gameMeta.get("statusUpdatedAt");
                    if (statusUpdatedAtRaw != null) {
                        state.setStatusUpdatedAt(Instant.parse(statusUpdatedAtRaw.toString()));
                    } else {
                        log.warn("Missing statusUpdatedAt in gameMeta for game {}", state.getGameId());
                        state.setStatusUpdatedAt(Instant.now());
                        saveGameStateToRedis(state, roomId, agentId)
                                .subscribe(null, err -> log.error("Failed to save default statusUpdatedAt for game {}: {}", state.getGameId(), err.getMessage()));
                    }

                    Object countdownRaw = gameMeta.get("countdownEndTime");
                    if (countdownRaw != null) {
                        try {
                            Instant countdownTime = Instant.parse(countdownRaw.toString());
                            state.setCountdownEndTime(countdownTime);
                            long durationSeconds = Duration.between(Instant.now(), countdownTime).getSeconds();
                            state.setCountdownDurationSeconds(Math.max(durationSeconds, 0));
                        } catch (DateTimeParseException e) {
                            log.warn("Invalid countdownEndTime format in gameMeta for game {}: {}", state.getGameId(), countdownRaw);
                            state.setCountdownEndTime(null);
                            state.setCountdownDurationSeconds(-1L);
                        }
                    } else {
//                        log.warn("Missing countdownEndTime in gameMeta for game {}", state.getGameId());
                        state.setCountdownEndTime(null);
                        state.setCountdownDurationSeconds(-1L);
                    }

                    state.setBackendEpochMillis(System.currentTimeMillis());

                    // Reactive dependencies
                    Mono<LinkedHashSet<Integer>> drawnNumbers = getDrawnNumbers(state.getGameId());
                    Mono<Set<String>> players = getAllPlayers(state.getGameId());
                    Mono<Set<String>> allSelectedCardsIds =
                            playerStateService.getAllSelectedCardsIds(state.getGameId());


                    // Combine everything
                    return Mono.zip(drawnNumbers, players, allSelectedCardsIds)
                            .flatMap(tuple -> {
                                LinkedHashSet<Integer> drawnNums = tuple.getT1();
                                Set<String> joinedPlayers = tuple.getT2();
                                Set<String> allSelected = tuple.getT3();

                                // Assign light-weight fields only
                                state.setDrawnNumber(drawnNums);
                                state.setJoinedPlayers(joinedPlayers);
                                state.setAllSelectedCardsIds(allSelected);

                                return Mono.just(state);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Failed to get game state for room {}: {}", roomId, e.getMessage(), e);
                    return Mono.empty();
                });
    }


    // ----------------------------
    // Delete GameState
    // ----------------------------
    public Mono<Boolean> deleteGameState(Long roomId, Long agentId) {
        return getGameState(roomId, agentId)
                .flatMap(state -> {
                    Long gameId = state.getGameId();
                    return redis.delete(
                                    RedisKeys.gameStateKey(roomId),
                                    RedisKeys.gameDrawnNumbersKey(gameId),
                                    RedisKeys.gamePlayersKey(gameId),
                                    RedisKeys.currentCardPoolKey(gameId),
                                    RedisKeys.roomCardsSetKey(roomId)
                            )
                            .map(count -> {
                                return count > 0;
                            })
                            // after deleting keys, remove room from active registry
                            .flatMap(deleted -> roomRegistry.removeRoom(roomId)
                                    .thenReturn(deleted)
                            )

                            .onErrorReturn(false);
                })
                .defaultIfEmpty(false) // ensures that if getGameState is empty, Mono emits false
                .doOnSuccess(deleted -> log.info("Deleted game state for roomId={} -> {}", roomId, deleted));
    }


    public Mono<Boolean> deleteDrawnNumbers(Long gameId) {
        return redis.delete(RedisKeys.gameDrawnNumbersKey(gameId))
                .map(count -> count > 0)
                .doOnError(e -> log.error("Failed to delete game state for gameId={}", gameId, e))
                .onErrorReturn(false)
                .doOnSuccess(deleted -> log.info("Deleted game state for gameId={} -> {}", gameId, deleted));
    }


    public Mono<Set<String>> getAllPlayers(Long gameId) {
        String playersKey = RedisKeys.gamePlayersKey(gameId);
        return setOps.members(playersKey)
                .map(String::valueOf)  // safer conversion
                .collect(Collectors.toSet())
                .onErrorResume(e -> {
                    log.warn("Failed to fetch players for game {}: {}", gameId, e.getMessage());
                    return Mono.just(Set.of());
                });
    }


}
