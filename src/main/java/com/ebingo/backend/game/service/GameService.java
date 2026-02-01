package com.ebingo.backend.game.service;

import com.ebingo.backend.common.service.DailyLeaderboardService;
import com.ebingo.backend.common.service.TotalLeaderboardService;
import com.ebingo.backend.game.dto.BingoClaimDto;
import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.dto.GameEndResponse;
import com.ebingo.backend.game.dto.RoomInternalDto;
import com.ebingo.backend.game.entity.Game;
import com.ebingo.backend.game.entity.Room;
import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.GameStatus;
import com.ebingo.backend.game.enums.ParticipantType;
import com.ebingo.backend.game.mappers.GameEndResponseMapper;
import com.ebingo.backend.game.mappers.GameMapper;
import com.ebingo.backend.game.repository.GameRepository;
import com.ebingo.backend.game.repository.RoomRepository;
import com.ebingo.backend.game.service.state.GameStateService;
import com.ebingo.backend.game.service.state.PlayerCleanupService;
import com.ebingo.backend.game.service.state.PlayerStateService;
import com.ebingo.backend.game.state.GameState;
import com.ebingo.backend.payment.dto.GameTransactionDto;
import com.ebingo.backend.payment.enums.GameTxnType;
import com.ebingo.backend.payment.service.GameTransactionService;
import com.ebingo.backend.payment.service.PaymentService;
import com.ebingo.backend.system.dto.SystemConfigDto;
import com.ebingo.backend.system.redis.RedisKeys;
import com.ebingo.backend.system.service.SystemConfigService;
import com.ebingo.backend.user.service.UserProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final Map<Long, GameState> activeGames = new ConcurrentHashMap<>();
    private final RedisPublisher publisher;
    private final CardPoolService cardPoolService;
    private final BingoPatternVerifier patternVerifier;
    private final PlayerStateService playerStateService;
    private final GameStateService gameStateService;
    private final PaymentService paymentService;
    private final ReactiveSetOperations<String, String> setOps;
    private final PlayerCleanupService playerCleanupService;
    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private final ReactiveRedisMessageListenerContainer redisListenerContainer;
    private final BingoClaimService bingoClaimService;
    private final ObjectMapper objectMapper;
    private final GameTransactionService gameTransactionService;
    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final CardSelectionService cardSelectionService;
    private final DailyLeaderboardService dailyLeaderboardService;
    private final TotalLeaderboardService totalLeaderboardService;
    private final SystemConfigService systemConfigService;
    private final RoomService roomService;
    private final UserProfileService userProfileService;
    private final BingoGameMetricsRedisService gameMetricsRedisService;
    private final GameParticipantTypeRedisStore participantTypeStore;


    // Stop signal sinks per room
//    private final Map<Long, MonoSink<Void>> stopLoopSinks = new ConcurrentHashMap<>();
    private final Map<Long, Sinks.One<Void>> stopLoopSinks = new ConcurrentHashMap<>();

    private final Set<Long> subscribedRooms = ConcurrentHashMap.newKeySet();


    @Value("${game.draw.intervalInSeconds:5}")
    private Integer drawInterval; // seconds

    @Value("${game.draw.iteration:75}")
    private Integer drawIteration; // seconds

    @Value("${game.countdown.initialInSeconds:30}")
    private Integer initialCountdownSeconds;

//


    // ==========================================================================
    public Mono<Void> playerJoin(Long roomId, Long gameId_, String userId, Integer capacity,
                                 BigDecimal entryFee, List<String> selectedCardIds, Long agentId, ParticipantType participantType) {

        log.info("USER {} SELECTED CARDS FOR ROOM {} ===== {}", userId, roomId, selectedCardIds);

        AtomicBoolean paymentCompleted = new AtomicBoolean(false);

        Mono<GameState> gameState = gameStateService.getOrInitializeGame(roomId, userId, capacity, agentId);

        return gameState.flatMap(state -> {
            String playersKey = RedisKeys.gamePlayersKey(state.getGameId());
            Long gameId = state.getGameId(); // use the actual gameId from state

            return setOps.add(playersKey, userId) // SADD
                    .flatMap(added -> {
                        if (added == 0L) {
                            log.info("User {} already joined game {}", userId, gameId);
                            return publisher.publishUserEvent(userId, Map.of(
                                    "type", "error",
                                    "payload", Map.of(
                                            "eventType", "game.playerJoinRequest",
                                            "errorType", "User already joined before",
                                            "message", "User already joined before",
                                            "amount", entryFee,
                                            "roomId", roomId
                                    )
                            )).then();
                        }

                        // 1️⃣ Claim all cards in parallel
                        return Flux.fromIterable(selectedCardIds)
                                .flatMap(cardId ->
                                        cardSelectionService.claimCard(roomId, gameId, userId, cardId, 2)
                                                .map(result -> Map.entry(cardId, result))
                                )
                                .collectList()
                                .flatMap(results -> {
                                    // Check if any claim failed
                                    List<Map.Entry<String, String>> failed = results.stream()
                                            .filter(e -> !"OK".equals(e.getValue()))
                                            .toList();

                                    if (!failed.isEmpty()) {
                                        String firstError = failed.getFirst().getValue();
                                        log.warn("Card claim failed for user {} in game {}: {}", userId, gameId, firstError);

                                        // Notify user about the error via WebSocket
                                        return publisher.publishUserEvent(userId, Map.of(
                                                        "type", "error",
                                                        "payload", Map.of(
                                                                "roomId", roomId,
                                                                "eventType", "game.playerJoinRequest",
                                                                "message", firstError,
                                                                "failedCards", failed.stream().map(Map.Entry::getKey).toList()
                                                        )
                                                ))
                                                // Rollback membership since they never joined successfully
                                                .then(setOps.remove(playersKey, userId))
                                                .then(playerCleanupService.removePlayerFromGame(roomId, gameId, userId))// Cleanup all keys
                                                .then();
                                    }

                                    // 2️⃣ All claims succeeded → Process payment
                                    log.info("All cards claimed successfully for user {} in game {}. Proceeding with payment...", userId, gameId);
                                    return paymentService.processPayment(Long.parseLong(userId), entryFee, gameId, agentId)
                                            .flatMap(paymentSuccess -> {
                                                if (!paymentSuccess) {
                                                    log.warn("Payment failed for user {} in game {}", userId, gameId);

                                                    return Flux.fromIterable(selectedCardIds)
                                                            .flatMap(cardId -> cardSelectionService.releaseCard(roomId, gameId, userId, cardId))
//                                                        .then(paymentService.processRefund(Long.parseLong(userId), gameId)
//                                                                .onErrorResume(err -> {
//                                                                    log.error("Refund failed for user {}: {}", userId, err.getMessage(), err);
//                                                                    return Mono.empty();
//                                                                })
//                                                        )
                                                            .then(setOps.remove(playersKey, userId))
                                                            .then(publisher.publishUserEvent(userId, Map.of(
                                                                    "type", "error",
                                                                    "payload", Map.of(
                                                                            "eventType", "game.playerJoinRequest",
                                                                            "errorType", "paymentFailed",
                                                                            "message", "Payment failed for user " + userId,
                                                                            "amount", entryFee,
                                                                            "roomId", roomId
                                                                    )
                                                            )))
                                                            .then(); // ✅ no Mono.error() here
                                                }


                                                // ✅ Payment success → complete join
                                                paymentCompleted.set(true);
                                                log.info("Payment successful for user {} in game {}", userId, gameId);

                                                // Store cardIds to participant type mapping in redis to identify card owner type
//                                                return afterSuccessfulJoin(roomId, gameId, userId, capacity, selectedCardIds, agentId);
                                                return participantTypeStore.putCards(gameId, selectedCardIds, participantType)
                                                        .then(participantTypeStore.expire(gameId, Duration.ofHours(3)))
                                                        .then(afterSuccessfulJoin(roomId, gameId, userId, capacity, selectedCardIds, agentId));
                                            })
                                            .onErrorResume(error -> {
                                                log.error("Unexpected error during payment for user {}: {}", userId, error.getMessage(), error);

                                                if (paymentCompleted.get()) {
                                                    return paymentService.processRefund(Long.parseLong(userId), gameId, agentId)
                                                            .onErrorResume(refundErr -> {
                                                                log.error("Refund failed for user {}: {}", userId, refundErr.getMessage(), refundErr);
                                                                return Mono.empty();
                                                            })
                                                            .then(Flux.fromIterable(selectedCardIds)
                                                                    .flatMap(cardId -> cardSelectionService.releaseCard(roomId, gameId, userId, cardId))
                                                                    .then(setOps.remove(playersKey, userId))
                                                                    .then());
                                                }

                                                // If payment not completed → release cards, remove user
                                                return Flux.fromIterable(selectedCardIds)
                                                        .flatMap(cardId -> cardSelectionService.releaseCard(roomId, gameId, userId, cardId))
                                                        .then(setOps.remove(playersKey, userId))
                                                        .then();
                                            });
                                });
                    });
        });
    }


    public Mono<Boolean> releaseCountdownLock(ReactiveStringRedisTemplate redisTemplate, String lockKey) {
        // Lua script: only delete the key if it exists (simple release)
        String luaScript = """
                if redis.call('EXISTS', KEYS[1]) == 1 then
                    return redis.call('DEL', KEYS[1])
                else
                    return 0
                end
                """;

        RedisScript<Long> script = RedisScript.of(luaScript, Long.class);

        return redisTemplate.execute(script, List.of(lockKey))
                .next()
                .map(result -> result != null && result > 0)
                .doOnNext(released -> {
                    if (released) {
                        log.info("Countdown lock {} released", lockKey);
                    } else {
                        log.info("Countdown lock {} was not held", lockKey);
                    }
                });
    }


//    private Mono<Void> afterSuccessfulJoin(Long roomId,
//                                           Long gameId,
//                                           String userId,
//                                           Integer capacity,
//                                           List<String> selectedCardIds, Long agentId) {
//
//        log.info("afterSuccessfulJoin: user {} joined game {}", userId, gameId);
//
//        return gameStateService.getGameState(roomId, agentId)
//                .flatMap(state -> {
//
//                    Set<String> joinedPlayers = Optional.ofNullable(state.getJoinedPlayers()).orElse(Set.of());
//                    int playersCount = joinedPlayers.size();
//                    List<String> allSelectedCardIds = new ArrayList<>(state.getAllSelectedCardsIds());
//
//                    Long countdownDurationSeconds = Optional.ofNullable(state.getCountdownDurationSeconds()).orElse(-1L);
//                    Instant countdownEndTime = state.getCountdownEndTime();
//                    GameStatus status = state.getStatus();
//
//                    return broadcastPlayerJoin(
//                            roomId, userId, joinedPlayers, playersCount, selectedCardIds,
//                            allSelectedCardIds, countdownDurationSeconds, countdownEndTime, status, agentId
//                    )
//                            // Update leaderboards
//                            .then(updateLeaderboardsOnJoin(Long.valueOf(userId), state.getEntryFee() * selectedCardIds.size(), agentId))
//                            .then(startCountdownIfEligible(state, roomId, gameId, userId, capacity, playersCount, agentId));
//                });
//    }


    private Mono<Void> afterSuccessfulJoin(
            Long roomId,
            Long gameId,
            String userId,
            Integer capacity,
            List<String> selectedCardIds,
            Long agentId
    ) {
        log.info("afterSuccessfulJoin: user {} joined game {}", userId, gameId);

        final int cardsCount = selectedCardIds != null ? selectedCardIds.size() : 0;

        if (cardsCount <= 0) {
            return Mono.error(new IllegalArgumentException("selectedCardIds must not be empty"));
        }

        // 1) Get user from DB to determine bot/real
        return userProfileService.getUserProfileByTelegramIdAndAgentId(Long.parseLong(userId), agentId) // Mono<User>
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + userId)))
                .flatMap(user -> {
                    final boolean isBot = user.getIsBot(); // <-- adjust getter
                    final long botCardsDelta = isBot ? cardsCount : 0L;
                    final long realCardsDelta = isBot ? 0L : cardsCount;

                    // 2) Update Redis metrics atomically (concurrency-safe)
                    return gameMetricsRedisService.addDeltas(
                                    gameId,
                                    botCardsDelta,
                                    realCardsDelta,
                                    java.math.BigDecimal.ZERO // realMoneyAmount not changed here
                            )
                            // 3) Continue your existing workflow
                            .then(gameStateService.getGameState(roomId, agentId))
                            .flatMap(state -> {

                                Set<String> joinedPlayers = Optional.ofNullable(state.getJoinedPlayers()).orElse(Set.of());
                                int playersCount = joinedPlayers.size();
                                List<String> allSelectedCardIds = new ArrayList<>(state.getAllSelectedCardsIds());

                                Long countdownDurationSeconds = Optional.ofNullable(state.getCountdownDurationSeconds()).orElse(-1L);
                                Instant countdownEndTime = state.getCountdownEndTime();
                                GameStatus status = state.getStatus();

                                return broadcastPlayerJoin(
                                        roomId, userId, joinedPlayers, playersCount, selectedCardIds,
                                        allSelectedCardIds, countdownDurationSeconds, countdownEndTime, status, agentId
                                )
                                        // Update leaderboards
                                        .then(updateLeaderboardsOnJoin(
                                                Long.valueOf(userId),
                                                state.getEntryFee() * cardsCount,
                                                agentId
                                        ))
                                        .then(startCountdownIfEligible(
                                                state, roomId, gameId, userId, capacity, playersCount, agentId
                                        ));
                            });
                });
    }


    private Mono<Void> updateLeaderboardsOnJoin(Long userId, Double betAmount, Long agentId) {

        // No update for zero or negative bets
        if (betAmount == null || betAmount <= 0) {
            return Mono.empty();
        }

        return dailyLeaderboardService.incrementDailyStats(userId, BigDecimal.valueOf(betAmount), agentId)
                .then(totalLeaderboardService.incrementTotalStats(userId, BigDecimal.valueOf(betAmount), agentId));
    }


    private Mono<Void> broadcastPlayerJoin(Long roomId,
                                           String userId,
                                           Set<String> joinedPlayers,
                                           int playersCount,
                                           List<String> selectedCardIds,
                                           List<String> allSelectedCardIds,
                                           Long countdownDurationSeconds,
                                           Instant countdownEndTime,
                                           GameStatus status, Long agentId) {
        Map<String, Object> payload = Map.of(
                "type", "game.playerJoined",
                "payload", Map.of(
                        "roomId", roomId,
                        "joinedPlayers", joinedPlayers,
                        "playerId", userId,
                        "playersCount", playersCount,
                        "playerSelectedCardIds", selectedCardIds,
                        "allSelectedCardIds", allSelectedCardIds,
                        "countdownDurationSeconds", countdownDurationSeconds,
                        "countdownEndTime", countdownEndTime == null ? "null" : countdownEndTime.toString(),
                        "backendEpochMillis", System.currentTimeMillis(),
                        "status", status
                )
        );

        return publisher.publishEvent(RedisKeys.roomChannel(roomId), payload)
                .doOnSuccess(id -> log.debug("Broadcasted player join event to room {}", roomId))
                .then(Mono.delay(Duration.ofMillis(100))) // Make sure the event is received
                .then(publisher.publishEvent(RedisKeys.roomChannel(roomId), payload))
                .then();
    }

    private Mono<Void> startCountdownIfEligible(
            GameState state,
            Long roomId,
            Long gameId,
            String userId,
            Integer capacity,
            int playersCount,
            Long agentId) {
        return getMinPlayersToStart(roomId)
                .flatMap(minPlayersToStart -> {
                    // Check eligibility
                    if (playersCount < minPlayersToStart
                            || state.isStarted()
                            || !GameStatus.READY.equals(state.getStatus())) {
                        return Mono.empty();
                    }

                    log.info("Starting countdown for game {}", gameId);
                    String countdownLockKey = RedisKeys.countdownLockKey(gameId);

                    // Lua script with TTL (EX seconds)
                    RedisScript<Long> acquireLockScript = RedisScript.of("""
                            if redis.call('exists', KEYS[1]) == 0 then
                                redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2])
                                return 1
                            else
                                return 0
                            end
                            """, Long.class);

                    // TTL in seconds — you can adjust this safely (e.g. 60)
                    String lockTTL = "60";

                    return reactiveRedisTemplate.execute(acquireLockScript, List.of(countdownLockKey), "locked", lockTTL)
                            .next()
                            .cast(Long.class)
                            .flatMap(acquired -> {
                                if (acquired != null && acquired == 1) {
                                    log.info("Countdown lock acquired for game {}", gameId);

                                    return startCountdownByGameId(roomId, gameId, userId, capacity, initialCountdownSeconds, agentId)
                                            // Release lock after countdown completes
                                            .then(releaseCountdownLock(reactiveRedisTemplate, countdownLockKey))
                                            .doOnError(err -> log.error("Countdown failed for game {}", gameId, err))
                                            .onErrorResume(err ->
                                                    releaseCountdownLock(reactiveRedisTemplate, countdownLockKey)
                                                            .then(Mono.error(err))
                                            );
                                } else {
                                    log.info("Countdown already started for game {}", gameId);
                                    return Mono.empty();
                                }
                            })
                            .onErrorResume(err -> {
                                log.error("Error acquiring countdown lock for game {}", gameId, err);
                                return Mono.empty();
                            });
                })
                .then(); // completes with Mono<Void>
    }


    public Mono<Void> leaveGame(Long roomId, Long gameId, String userId, Long agentId) {
        System.out.println("User " + userId + " is leaving game " + gameId);
        return gameStateService.getGameState(roomId, agentId)
                .flatMap(state -> {
                    if (state == null) {
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "error",
                                        "payload", Map.of(
                                                "eventType", "game.playerLeaveRequest",
                                                "errorType", "invalidGame",
                                                "message", "State not found for game.",
                                                "userId", userId,
                                                "gameId", gameId,
                                                "roomId", roomId,
                                                "playerId", userId
                                        )
                                )).then();
                    }

                    String playersKey = RedisKeys.gamePlayersKey(state.getGameId());
                    String roomPlayersKey = RedisKeys.roomPlayersKey(roomId);

                    boolean gameStarted = state.isStarted();
                    boolean gameEnded = state.isEnded();

                    Instant gameCountdownEndTime = state.getCountdownEndTime();
                    long timeLeft = 15; // default to 15 seconds

                    if (gameCountdownEndTime != null) {
                        timeLeft = Instant.now().until(gameCountdownEndTime, ChronoUnit.SECONDS);
                    } else {
                        // Handle the case where countdown end time is missing
                        log.warn("Countdown end time is null for game state with game id: {}", state.getGameId());
                        // or some default value
                    }

                    if (gameStarted) {
                        // Already started → personal acknowledgement only
                        log.info("User {} tried to cancel, but game {} already started", userId, gameId);
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "game.playerLeft",
                                        "payload", Map.of(
                                                "errorType", "gameStarted",
                                                "message", "Game already started.",
                                                "playerId", userId,
                                                "gameId", gameId,
                                                "roomId", roomId,
                                                "agentId", agentId
                                        )
                                )).then();
                    }

                    if (gameEnded) {
                        // Game already ended → personal acknowledgement only
                        log.info("User {} tried to cancel, but game {} already ended", userId, gameId);
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "game.playerLeft",
                                        "payload", Map.of(
                                                "errorType", "gameEnded",
                                                "message", "Game already ended.",
                                                "playerId", userId,
                                                "gameId", gameId,
                                                "roomId", roomId,
                                                "agentId", agentId
                                        )
                                )).then();
                    }

                    if (timeLeft <= 10 && timeLeft >= 0) {
                        // Game is almost starting → personal acknowledgement only
                        log.info("User {} tried to cancel, but game {} is almost starting", userId, gameId);
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "game.playerLeft",
                                        "payload", Map.of(
                                                "errorType", "gameStarting",
                                                "message", "Game is almost starting.",
                                                "playerId", userId,
                                                "gameId", gameId,
                                                "roomId", roomId,
                                                "agentId", agentId
                                        )
                                )).then();
                    }

                    // Game not started → attempt SREM
                    return setOps.remove(playersKey, userId)
                            .flatMap(removed -> {
                                // Explicitly check if user was in the game
                                if (removed == 0) {
                                    log.warn("User {} was not part of game {} when attempting cancel", userId, gameId);
                                    return publisher.publishUserEvent(userId,
                                            Map.of(
                                                    "type", "error",
                                                    "payload", Map.of(
                                                            "eventType", "game.playerLeaveRequest",
                                                            "errorType", "notInGame",
                                                            "userId", userId,
                                                            "gameId", gameId,
                                                            "roomId", roomId,
                                                            "agentId", agentId,
                                                            "message", "You were not part of the game."
                                                    )
                                            )).then();
                                }

                                log.info("User {} successfully removed from game {}", userId, gameId);

                                // Refund payment
                                Mono<Boolean> refund = paymentService.processRefund(Long.parseLong(userId), gameId, agentId)
                                        .doOnNext(refunded -> log.info("Refund {} for user {} in game {}",
                                                refunded ? "succeeded" : "failed", userId, gameId));

                                // Broadcast updated players
                                Mono<Long> broadcastPlayers = gameStateService.getGameState(roomId, agentId)
                                        .flatMap(updatedState -> {
                                            Set<String> players = updatedState.getJoinedPlayers();
                                            int playersCount = players.size();

//                                            return playerCleanupService.removePlayerFromGame(roomId, gameId, userId)
//                                                    .flatMap(cardIds -> {
//                                                        return publisher.publishEvent(
//                                                                RedisKeys.roomChannel(roomId),
//                                                                Map.of(
//                                                                        "type", "game.playerLeft",
//                                                                        "payload", Map.of(
//                                                                                "playerId", userId,
//                                                                                "gameId", gameId,
////                                                                                "gameState", updatedState,
//                                                                                "joinedPlayers", players,
//                                                                                "playersCount", playersCount,
//                                                                                "releasedCardsIds", cardIds,
//                                                                                "roomId", roomId
//                                                                        )
//                                                                )
//                                                        );
//                                                    });

                                            return playerCleanupService.removePlayerFromGame(roomId, gameId, userId)
                                                    .flatMap(cardIds -> {

                                                        // Remove cards from bingoGameMetricsRedisService (atomic + concurrency-safe)
                                                        final long cardsCount = cardIds != null ? cardIds.size() : 0L;

                                                        Mono<Void> metricsUpdate = Mono.empty();
                                                        if (cardsCount > 0) {
                                                            metricsUpdate = userProfileService.getUserProfileByTelegramIdAndAgentId(Long.parseLong(userId), agentId) // adjust to your user lookup method
                                                                    .flatMap(user -> {
                                                                        boolean isBot = user.getIsBot(); // adjust getter if needed

                                                                        long botDelta = isBot ? -cardsCount : 0L;
                                                                        long realDelta = isBot ? 0L : -cardsCount;

                                                                        return gameMetricsRedisService.addDeltas(
                                                                                gameId,
                                                                                botDelta,
                                                                                realDelta,
                                                                                BigDecimal.ZERO
                                                                        ).then();
                                                                    });
                                                        }

                                                        return metricsUpdate.then(
                                                                publisher.publishEvent(
                                                                        RedisKeys.roomChannel(roomId),
                                                                        Map.of(
                                                                                "type", "game.playerLeft",
                                                                                "payload", Map.of(
                                                                                        "playerId", userId,
                                                                                        "gameId", gameId,
//                                                                                          "gameState", updatedState,
                                                                                        "joinedPlayers", players,
                                                                                        "playersCount", playersCount,
                                                                                        "releasedCardsIds", cardIds,
                                                                                        "roomId", roomId
                                                                                )
                                                                        )
                                                                )
                                                        );
                                                    });

                                        });

                                return refund.then(broadcastPlayers).then(setOps.remove(roomPlayersKey, userId)).then();
                            });
                })
                .onErrorResume(error -> {
                    log.error("Error in playerCancel for user {} in room {}: {}", userId, roomId, error.getMessage(), error);
                    // Always send personal error acknowledgement
                    return publisher.publishUserEvent(userId,
                            Map.of(
                                    "type", "error",
                                    "payload", Map.of(
                                            "errorType", "leaveError",
                                            "userId", userId,
                                            "gameId", gameId,
                                            "roomId", roomId,
                                            "agentId", agentId,
                                            "success", false,
                                            "message", "Unable to cancel: " + error.getMessage()
                                    )
                            )).then();
                });
    }


    /**
     * Start countdown for a game
     */
    public Mono<Void> startCountdownByGameId(Long roomId, Long gameId, String userId, Integer capacity, int countdownSeconds, Long agentId) {
        Instant countdownEndTime = Instant.now().plusSeconds(countdownSeconds);
        Mono<Boolean> updateGameState = gameStateService.getGameState(roomId, agentId)
                .flatMap(state -> {
                    // Update countdown end time and status
                    state.setCountdownEndTime(countdownEndTime);
                    state.setStatus(GameStatus.COUNTDOWN);
                    state.setStatusUpdatedAt(Instant.now());
                    return gameStateService.saveGameStateToRedis(state, roomId, agentId);
                });

        // Publish countdown start event (only once)
        Mono<Long> countdownEvent = publisher.publishEvent(
                RedisKeys.roomChannel(roomId),
                Map.of(
                        "type", "game.countdown",
                        "payload", Map.of(
                                "roomId", roomId,
                                "gameId", gameId,
                                "countdownDurationSeconds", countdownSeconds,
                                "countdownEndTime", countdownEndTime.toString(),
                                "backendEpochMillis", System.currentTimeMillis()
                        )
                )
        );

        // Run countdown internally, then conditionally start game

        return getMinPlayersToStart(roomId)
                .flatMap(minPlayersToStart -> {
                    return updateGameState
                            .then(countdownEvent)
                            .then(Mono.delay(Duration.ofMillis(100))) // Make sure event is sent before countdown starts
                            .then(countdownEvent)
                            .thenMany(
                                    Flux.range(0, countdownSeconds)
                                            .delayElements(Duration.ofSeconds(1))
                                            .doOnNext(sec -> log.debug("Countdown {} / {}", sec + 1, countdownSeconds))
                            )
                            .then(
                                    // After countdown, check player count again before starting
                                    Mono.defer(() ->
                                            gameStateService.getAllPlayers(gameId)
                                                    .flatMap(state -> {
                                                        int playersCount = state.size();
                                                        log.info("Countdown finished. Players: {} / min: {}", playersCount, minPlayersToStart);
                                                        if (playersCount >= minPlayersToStart) {
                                                            return startGame(gameId, roomId, userId, capacity, agentId);
                                                        } else {
                                                            log.warn("Not enough players after countdown. Game {} will not start.", gameId);

                                                            return gameStateService.getGameState(roomId, agentId)
                                                                    .flatMap(gState -> {
                                                                        // Reset game state to READY
                                                                        gState.setStatus(GameStatus.READY);
                                                                        gState.setCountdownEndTime(null);
                                                                        gState.setStatusUpdatedAt(Instant.now());
                                                                        return gameStateService.saveGameStateToRedis(gState, roomId, agentId)
                                                                                .then(updateGameToDatabase(gState, agentId))
                                                                                .then(publisher.publishEvent(
                                                                                        RedisKeys.roomChannel(roomId),
                                                                                        Map.of(
                                                                                                "type", "game.notEnoughPlayers",
                                                                                                "payload", Map.of(
                                                                                                        "roomId", roomId,
                                                                                                        "gameId", gameId,
                                                                                                        "status", GameStatus.READY,
                                                                                                        "joinedPlayers", gState.getJoinedPlayers(),
                                                                                                        "playersCount", playersCount
                                                                                                )
                                                                                        )
                                                                                ));
                                                                    }).then();
                                                        }
                                                    })
                                    )
                            );
                }).then();

    }


    /**
     * Start the game
     */
    private Mono<Void> startGame(Long gameId, Long roomId, String userId, Integer capacity, Long agentId) {
        return gameStateService.getGameState(roomId, agentId)
                .flatMap(state -> {

                    // Update game state to started and playing
                    state.setStarted(true);
                    state.setEnded(false);
                    state.setStatus(GameStatus.PLAYING);
                    state.setStatusUpdatedAt(Instant.now());

                    // Save the updated state first
                    return gameStateService.saveGameStateToRedis(state, roomId, agentId)
                            .then(publisher.publishEvent(
                                    RedisKeys.roomChannel(roomId),
                                    Map.of(
                                            "type", "game.started",
                                            "payload", Map.of(
                                                    "message", "Game has started.",
                                                    "roomId", roomId,
                                                    "gameId", gameId,
                                                    "agentId", agentId
                                            ) // empty payload
                                    )
                            ))
                            .then(
                                    startNumberDrawingWithLuaLock(reactiveRedisTemplate, state, userId, agentId)
                                            .onErrorResume(e -> {
                                                log.error("Number drawing failed", e);
                                                return Mono.empty();
                                            })
                            );
                });
    }

    /**
     * Start number drawing with distributed lock to ensure only one instance handles it
     */

    private Mono<Void> startNumberDrawingWithLuaLock(ReactiveStringRedisTemplate redisTemplate,
                                                     GameState state,
                                                     String userId, Long agentId) {
        String lockKey = RedisKeys.gameDrawingLockKey(state.getGameId());
        String lockValue = UUID.randomUUID().toString(); // unique owner
        int lockTTLSeconds = 250;

        // Lua script: acquire lock with NX + EX
        String acquireScriptStr = """
                if redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) then
                    return 1
                else
                    return 0
                end
                """;

        RedisScript<Long> acquireScript = RedisScript.of(acquireScriptStr, Long.class);

        // Lua script: release only if owner matches
        String releaseScriptStr = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;

        RedisScript<Long> releaseScript = RedisScript.of(releaseScriptStr, Long.class);

        return redisTemplate.execute(acquireScript, List.of(lockKey), lockValue, String.valueOf(lockTTLSeconds))
                .next()
                .flatMap(acquired -> {
                    if (acquired != null && acquired == 1L) {
                        log.info("Instance acquired drawing lock for game {}", state.getGameId());

                        return gameStateService.deleteDrawnNumbers(state.getGameId())
                                .flatMap(deleted -> {
                                    return drawNumbersLoop(state, userId, agentId)
                                            .then(redisTemplate.execute(releaseScript, List.of(lockKey), lockValue)
                                                    .next()
                                                    .map(result -> result != null && result > 0)
                                                    .doOnNext(released -> {
                                                        if (released) {
                                                            log.info("Released drawing lock for game {}", state.getGameId());
                                                        } else {
                                                            log.warn("Lock for game {} was not released (owner mismatch or expired)", state.getGameId());
                                                        }
                                                    })
                                                    .onErrorResume(e -> {
                                                        log.warn("Failed to unlock drawing lock for game {}", state.getGameId(), e);
                                                        return Mono.empty();
                                                    })
                                            )
                                            .then();

                                });

                    } else {
                        log.info("Another instance is handling number drawing for game {}", state.getGameId());
                        return Mono.empty();
                    }
                });
    }


    // NEW WITH SELECTED CARD DRAWING LOGIC
//    public Mono<Void> drawNumbersLoop(GameState state, String userId, Long agentId) {
//        final Long roomId = state.getRoomId();
//        final int maxDraws = 75;
//        final String endLockKey = "game:end-lock:" + roomId;
//
//        Sinks.One<Void> stopSink = Sinks.one();
//        stopLoopSinks.put(roomId, stopSink);
//
//        Mono<SystemConfigDto> configMono =
//                systemConfigService.getSystemConfigByNameAndAgentId("DRAW_FROM_SELECTED_CARDS_ONLY", agentId);
//
//        Mono<RoomInternalDto> roomMono =
//                roomService.getRoomWithCardPoolById(roomId);
//
//        // Randomly select one card ID from selected cards - ONLY ORIGINAL IDS LIST

    /// /        List<String> selectedCardIds = new ArrayList<>(state.getAllSelectedCardsIds());
    /// /        Collections.shuffle(selectedCardIds);
    /// /        String randomSelectedCardId = selectedCardIds.get(0);
//
//
//        // MAKE THE LIST AT LEAST 100 ITEMS
//
//        List<String> originalIds = new ArrayList<>(state.getAllSelectedCardsIds());
//        Random random = new Random();
//
//        if (originalIds.isEmpty()) {
//            throw new IllegalStateException("No cards selected!");
//        }
//
//        // Shuffle original IDs first
//        Collections.shuffle(originalIds, random);
//
//        // Create a new list starting with the shuffled original IDs
//        List<String> selectedCardIds = new ArrayList<>(originalIds);
//
//        // Add random duplicates until the list has at least 100 IDs
//        for (int i = selectedCardIds.size(); i < 100; i++) {
//            selectedCardIds.add(originalIds.get(random.nextInt(originalIds.size())));
//        }
//
//        // Shuffle the final list once to mix original + duplicates
//        Collections.shuffle(selectedCardIds, random);
//
//        // Pick a random ID from the final list
//        String randomSelectedCardId = selectedCardIds.get(random.nextInt(selectedCardIds.size()));
//
//
//        Mono<List<Integer>> drawSequenceMono =
//                configMono.flatMap(config -> {
//                    boolean drawFromCardOnly =
//                            config != null && "TRUE".equalsIgnoreCase(config.getValue());
//
//                    if (!drawFromCardOnly) {
//                        // Normal 1–75 draw
//                        log.info("NORMAL DRAW LOOP STARTED: FOR GAME {}", state.getGameId());
//                        List<Integer> all = IntStream.rangeClosed(1, 75)
//                                .boxed()
//                                .collect(Collectors.toList());
//                        Collections.shuffle(all);
//                        return Mono.just(all);
//                    }
//
//                    // DRAW_FROM_SELECTED_CARDS_ONLY = TRUE
//                    return roomMono.map(room -> {
//                        try {
//                            ObjectMapper mapper = new ObjectMapper();
//                            JsonNode root = mapper.readTree(room.getCardPoolJson());
//
//                            // Find the selected card
//                            JsonNode selectedCard = StreamSupport.stream(root.spliterator(), false)
//                                    .filter(node ->
//                                            randomSelectedCardId.equals(node.get("cardId").asText()))
//                                    .findFirst()
//                                    .orElseThrow(() ->
//                                            new IllegalStateException("Selected card not found in card pool"));
//
//                            log.info("SELECTED CARD DRAW LOOP STARTED: FOR GAME {} AND CARD {}", state.getGameId(), selectedCard);
//
//                            // Extract 24 numbers (exclude center free = 0)
//                            Set<Integer> cardNumbers = new HashSet<>();
//                            JsonNode numbersNode = selectedCard.get("numbers");
//
//                            for (String col : List.of("B", "I", "N", "G", "O")) {
//                                for (JsonNode n : numbersNode.get(col)) {
//                                    int value = n.asInt();
//                                    if (value != 0) {
//                                        cardNumbers.add(value);
//                                    }
//                                }
//                            }
//
//                            if (cardNumbers.size() != 24) {
//                                throw new IllegalStateException("Invalid card numbers count: " + cardNumbers.size());
//                            }
//
//                            // Shuffle card numbers first
//                            List<Integer> primary = new ArrayList<>(cardNumbers);
//                            Collections.shuffle(primary);
//
//                            // Remaining numbers from 1–75 not in card
//                            List<Integer> remaining = IntStream.rangeClosed(1, 75)
//                                    .filter(n -> !cardNumbers.contains(n))
//                                    .boxed()
//                                    .collect(Collectors.toList());
//                            Collections.shuffle(remaining);
//
//                            // Final draw order
//                            List<Integer> finalSequence = new ArrayList<>(75);
//                            finalSequence.addAll(primary);
//                            finalSequence.addAll(remaining);
//
//                            log.info(
//                                    "Room {} drawing from selected card {} (24 numbers first)",
//                                    roomId,
//                                    randomSelectedCardId
//                            );
//
//                            return finalSequence;
//
//                        } catch (Exception e) {
//                            throw new RuntimeException("Failed to prepare draw sequence", e);
//                        }
//                    });
//                });
//
//        return drawSequenceMono.flatMapMany(drawSequence ->
//                        Flux.fromIterable(drawSequence)
//                                .delayElements(Duration.ofSeconds(drawInterval))
//                                .flatMap(number ->
//                                        gameStateService.getGameState(roomId, agentId)
//                                                .flatMap(latestState -> {
//                                                    if (latestState.isEnded()
//                                                            || latestState.getStopNumberDrawing()
//                                                            || latestState.getDrawnNumbers().contains(number)) {
//                                                        stopSink.tryEmitEmpty();
//                                                        return Mono.empty();
//                                                    }
//                                                    return drawSingleNumber(latestState, number, agentId);
//                                                })
//                                )
//                                .takeUntilOther(stopSink.asMono())
//                )
//                .then(Mono.defer(() ->
//                        handleNoWinnerEnd(roomId, userId, endLockKey, agentId)))
//                .doFinally(signal -> stopLoopSinks.remove(roomId));
//    }
    public Mono<Void> drawNumbersLoop(GameState state, String userId, Long agentId) {
        final Long roomId = state.getRoomId();
        final long gameId = state.getGameId();
        final String endLockKey = "game:end-lock:" + roomId;

        Sinks.One<Void> stopSink = Sinks.one();
        stopLoopSinks.put(roomId, stopSink);

        Mono<SystemConfigDto> drawFromSelectedConfigMono =
                systemConfigService.getSystemConfigByNameAndAgentId("DRAW_FROM_SELECTED_CARDS_ONLY", agentId);

        Mono<SystemConfigDto> realUserOnlyPlay75ConfigMono =
                systemConfigService.getSystemConfigByNameAndAgentId("REAL_USER_ONLY_PLAY_75", agentId);

        Mono<SystemConfigDto> selectedCardsMultiplierMono =
                systemConfigService.getSystemConfigByNameAndAgentId("SELECTED_CARDS_MULTIPLIER", agentId);

        Mono<RoomInternalDto> roomMono =
                roomService.getRoomWithCardPoolById(roomId);

        // Original card IDs (real + bot selected cards for this game)
        final List<String> originalCardIds = new ArrayList<>(state.getAllSelectedCardsIds());
        if (originalCardIds.isEmpty()) {
            return Mono.error(new IllegalStateException("No cards selected!"));
        }

        final Random random = new Random();
        final ObjectMapper mapper = new ObjectMapper();

        Mono<List<Integer>> drawSequenceMono =
                drawFromSelectedConfigMono.flatMap(drawFromSelectedCfg -> {
                    boolean drawFromSelectedOnly = isTrue(drawFromSelectedCfg);

                    // ✅ Requirement 1:
                    // If DRAW_FROM_SELECTED_CARDS_ONLY is FALSE -> ignore other configs and do normal draw
                    if (!drawFromSelectedOnly) {
                        log.info("NORMAL DRAW LOOP STARTED: FOR GAME {}", gameId);
                        return Mono.just(generateNormal75Draw(random));
                    }

                    // ✅ Requirement 2: DRAW_FROM_SELECTED_CARDS_ONLY is TRUE
                    return Mono.zip(
                                    realUserOnlyPlay75ConfigMono.defaultIfEmpty(new SystemConfigDto()),
                                    selectedCardsMultiplierMono.defaultIfEmpty(new SystemConfigDto())
                            )
                            .flatMap(tuple -> {
                                boolean realUserOnlyPlay75 = isTrue(tuple.getT1());
                                BigDecimal multiplier = parseMultiplier(tuple.getT2()); // default 1 if missing/bad

                                // a) Build list based on multiplier
                                //    NOTE: rule (b) may override later
                                List<String> weightedCardIds =
                                        buildWeightedCardIdList(originalCardIds, multiplier, random);

                                // This is the "pick random from final list" line (your key requirement)
                                String randomSelectedCardId =
                                        weightedCardIds.get(random.nextInt(weightedCardIds.size()));

                                log.info("FINAL SEQUENCE SIZE: {}", weightedCardIds.size());


                                // b) If REAL_USER_ONLY_PLAY_75 is TRUE and selected card is REAL -> normal 75 draw
                                return participantTypeStore.get(gameId, randomSelectedCardId)
                                        // choose your preferred default. I recommend error, but keeping safe default:
                                        .defaultIfEmpty(ParticipantType.REAL)
                                        .flatMap(type -> {
                                            if (realUserOnlyPlay75 && type == ParticipantType.REAL) {
                                                log.info("REAL_USER_ONLY_PLAY_75=TRUE and chosen card is REAL -> NORMAL 75 draw. gameId={}, cardId={}",
                                                        gameId, randomSelectedCardId);
                                                return Mono.just(generateNormal75Draw(random));
                                            }

                                            // Otherwise draw "selected card numbers first (24), then remaining"
                                            return roomMono.map(room -> {
                                                try {
                                                    JsonNode root = mapper.readTree(room.getCardPoolJson());

                                                    JsonNode selectedCard = StreamSupport.stream(root.spliterator(), false)
                                                            .filter(node -> randomSelectedCardId.equals(node.get("cardId").asText()))
                                                            .findFirst()
                                                            .orElseThrow(() ->
                                                                    new IllegalStateException("Selected card not found in card pool: " + randomSelectedCardId));

                                                    log.info(
                                                            "SELECTED CARD DRAW LOOP STARTED: gameId={} roomId={} cardId={} type={}",
                                                            gameId, roomId, randomSelectedCardId, type
                                                    );
                                                    // Extract 24 numbers (exclude center free = 0)
                                                    Set<Integer> cardNumbers = new HashSet<>();
                                                    JsonNode numbersNode = selectedCard.get("numbers");

                                                    for (String col : List.of("B", "I", "N", "G", "O")) {
                                                        for (JsonNode n : numbersNode.get(col)) {
                                                            int value = n.asInt();
                                                            if (value != 0) cardNumbers.add(value);
                                                        }
                                                    }

                                                    if (cardNumbers.size() != 24) {
                                                        throw new IllegalStateException("Invalid card numbers count: " + cardNumbers.size());
                                                    }

                                                    // 24 from selected card first
                                                    List<Integer> primary = new ArrayList<>(cardNumbers);
                                                    Collections.shuffle(primary, random);

                                                    // Remaining 1–75 not in card
                                                    List<Integer> remaining = IntStream.rangeClosed(1, 75)
                                                            .filter(n -> !cardNumbers.contains(n))
                                                            .boxed()
                                                            .collect(Collectors.toList());
                                                    Collections.shuffle(remaining, random);

                                                    List<Integer> finalSequence = new ArrayList<>(75);
                                                    finalSequence.addAll(primary);
                                                    finalSequence.addAll(remaining);

                                                    log.info("Room {} drawing from selected card {} (24 numbers first)", roomId, randomSelectedCardId);
                                                    return finalSequence;

                                                } catch (Exception e) {
                                                    throw new RuntimeException("Failed to prepare draw sequence", e);
                                                }
                                            });
                                        });
                            });
                });

        return drawSequenceMono
                .flatMapMany(drawSequence ->
                        Flux.fromIterable(drawSequence)
                                .delayElements(Duration.ofSeconds(drawInterval))
                                .flatMap(number ->
                                        gameStateService.getGameState(roomId, agentId)
                                                .flatMap(latestState -> {
                                                    if (latestState.isEnded()
                                                            || latestState.getStopNumberDrawing()
                                                            || latestState.getDrawnNumbers().contains(number)) {
                                                        stopSink.tryEmitEmpty();
                                                        return Mono.empty();
                                                    }
                                                    return drawSingleNumber(latestState, number, agentId);
                                                })
                                )
                                .takeUntilOther(stopSink.asMono())
                )
                .then(participantTypeStore.deleteGame(gameId))
                .then(Mono.defer(() -> handleNoWinnerEnd(roomId, userId, endLockKey, agentId)))
                .doFinally(signal -> stopLoopSinks.remove(roomId));
    }

/* -----------------------------
   Helpers
------------------------------ */

    private boolean isTrue(SystemConfigDto cfg) {
        return cfg != null && cfg.getValue() != null && "TRUE".equalsIgnoreCase(cfg.getValue().trim());
    }

    private List<Integer> generateNormal75Draw(Random random) {
        List<Integer> all = IntStream.rangeClosed(1, 75).boxed().collect(Collectors.toList());
        Collections.shuffle(all, random);
        return all;
    }

    private BigDecimal parseMultiplier(SystemConfigDto cfg) {
        if (cfg == null || cfg.getValue() == null) return BigDecimal.ONE;
        try {
            BigDecimal m = new BigDecimal(cfg.getValue().trim());
            if (m.compareTo(BigDecimal.ONE) <= 0) return BigDecimal.ONE;
            return m;
        } catch (Exception e) {
            return BigDecimal.ONE;
        }
    }

    /**
     * Requirement (2a):
     * If multiplier > 1:
     * - duplicate the entire original list floor(multiplier) times
     * - for fractional part, add random picks for that fraction of the original size
     * <p>
     * Examples:
     * - m=2   => list repeated 2x
     * - m=3   => list repeated 3x
     * - m=1.5 => list repeated 1x + extra random picks equal to 0.5 * originalSize
     */
    private List<String> buildWeightedCardIdList(List<String> original, BigDecimal multiplier, Random random) {
        List<String> base = new ArrayList<>(original);
        Collections.shuffle(base, random);

        if (multiplier == null || multiplier.compareTo(BigDecimal.ONE) <= 0) {
            return base;
        }

        int whole = multiplier.intValue(); // floor for positive
        BigDecimal frac = multiplier.subtract(BigDecimal.valueOf(whole));

        List<String> out = new ArrayList<>(base.size() * whole + base.size());

        // whole repeats
        for (int i = 0; i < whole; i++) {
            out.addAll(base);
        }

        // fractional part -> random picks
        if (frac.compareTo(BigDecimal.ZERO) > 0) {
            int extra = frac.multiply(BigDecimal.valueOf(base.size()))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();

            for (int i = 0; i < extra; i++) {
                out.add(base.get(random.nextInt(base.size())));
            }
        }

        Collections.shuffle(out, random);
        return out;
    }

//===========================================================================================

    private Mono<Void> handleNoWinnerEnd(Long roomId, String userId, String endLockKey, Long agentId) {
        return gameStateService.getGameState(roomId, agentId)
                .flatMap(state -> {
                    if (state.isEnded() || state.getClaimRequested()) return Mono.empty();

                    List<Integer> remaining = IntStream.rangeClosed(1, drawIteration)
                            .filter(n -> !state.getDrawnNumbers().contains(n))
                            .boxed()
                            .collect(Collectors.toList());

                    if (!remaining.isEmpty()) return Mono.empty();

                    return Mono.delay(Duration.ofSeconds(3))
                            .then(gameStateService.getGameState(roomId, agentId))
                            .flatMap(latest -> {
                                if (latest.isEnded() || latest.getClaimRequested()) return Mono.empty();

                                return reactiveRedisTemplate.opsForValue()
                                        .setIfAbsent(endLockKey, "locked", Duration.ofSeconds(10))
                                        .flatMap(acquired -> {
                                            if (!Boolean.TRUE.equals(acquired)) return Mono.empty();

                                            GameEndResponse response = GameEndResponse.builder()
                                                    .gameId(latest.getGameId())
                                                    .roomId(latest.getRoomId())
                                                    .cardId("")
                                                    .playerId(0L)
                                                    .playerName("No Winner")
                                                    .pattern("")
                                                    .prizeAmount(BigDecimal.ZERO)
                                                    .hasWinner(false)
                                                    .winAt(LocalDateTime.now())
                                                    .markedNumbers(Set.of())
                                                    .card(new CardInfo())
                                                    .agentId(agentId)
                                                    .build();

                                            return endGame(latest, userId, response, agentId)
                                                    .then(reactiveRedisTemplate.delete(endLockKey))
                                                    .then();
                                        });
                            });
                });
    }


    /**
     * Draw a single number and update state
     */
    private Mono<Void> drawSingleNumber(GameState state, Integer number, Long agentId) {
        return Mono.defer(() -> {
            if (state.isEnded() || state.getStopNumberDrawing()) {
                log.info("Game {} ended during drawing, stopping", state.getGameId());
                return Mono.empty(); // Stop if game ended
            }

            state.getDrawnNumbers().add(number);
//            log.info("Drawing number {} for game {}: ", number, state.getGameId());
            log.info("Drawing number {} for game {} and room {}: drawnNumbers={}", number, state.getGameId(), state.getRoomId(), state.getDrawnNumbers());

            // Save updated state to Redis
            return gameStateService.saveGameStateToRedis(state, state.getRoomId(), agentId)
                    .then(gameStateService.addOrInitDrawnNumber(state.getGameId(), number))
                    .then(publisher.publishEvent(
                            RedisKeys.roomChannel(state.getRoomId()),
                            Map.of("type", "game.numberDrawn",
                                    "payload", Map.of(
                                            "number", number,
                                            "gameId", state.getGameId(),
                                            "roomId", state.getRoomId()))
                    ))
                    .then();
        });
    }

    private Mono<Void> endGame(GameState state, String userId, GameEndResponse responseObject, Long agentId) {
        // Mark the state as ended
        state.setEnded(true);
        state.setStatus(GameStatus.COMPLETED);

        Long roomId = state.getRoomId();

        log.info("===================================>>> Game End Broadcast: {}", responseObject);

        // Fully reactive: delete Redis state, then publish the event
        return gameStateService.deleteGameState(roomId, agentId)
                .then(Mono.defer(() ->
                        publisher.publishEvent(
                                RedisKeys.roomChannel(roomId),
                                Map.of(
                                        "type", "game.ended",
                                        "payload", GameEndResponseMapper.toMap(responseObject)
                                )
                        )
                ))
                .then(); // ensure Mono<Void>
    }


    private Mono<Boolean> updateGameToDatabase(GameState latestState, Long agentId) {
        return gameRepository.findById(latestState.getGameId())
                .flatMap(existingGame -> {
                    Game updatedGame = GameMapper.toEntity(latestState, existingGame, objectMapper);
                    updatedGame.setAgentId(agentId);

                    return gameRepository.save(updatedGame)
                            .doOnSubscribe(sub -> log.info("Updating game {} to database", updatedGame.getId()))
                            .doOnNext(saved -> log.info("Updated game {} to database", saved.getId()))
                            .thenReturn(true); // Return true after saving
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Game not found for ID: {}", latestState.getGameId());
                    return Mono.just(false);
                }))
                .onErrorResume(e -> {
                    log.error("Failed to update game {}: {}", latestState.getGameId(), e.getMessage(), e);
                    return Mono.just(false);
                });
    }


    public Mono<Void> claimBingo(Long roomId, String userId, Map<String, Object> payload, Long agentId, ParticipantType participantType) {
        log.info("BINGO CLAIM PAYLOAD: {}", payload);

        Long gameId = payload.get("gameId") != null ? Long.valueOf(payload.get("gameId").toString()) : null;
        String playerName = payload.get("playerName") != null ? payload.get("playerName").toString() : "";
        String cardId = payload.get("cardId") != null ? payload.get("cardId").toString() : "";
        @SuppressWarnings("unchecked")
        List<Integer> markedList = (List<Integer>) payload.get("markedNumbers");
        String pattern = payload.get("pattern") != null ? payload.get("pattern").toString() : GamePattern.LINE_AND_CORNERS.name();
        Long dbUserId = payload.get("userProfileId") != null ? Long.valueOf(payload.get("userProfileId").toString()) : null;

        CardInfo cardInfo = payload.get("card") != null
                ? objectMapper.convertValue(payload.get("card"), CardInfo.class)
                : new CardInfo();

        // Basic validation
        if (gameId == null || dbUserId == null || cardId == null || cardId.isBlank() || markedList == null || cardInfo.getNumbers() == null) {
            return sendUserError(userId, cardId, roomId, "INVALID_CLAIM", "Invalid claim data");
        }

        Set<Integer> claimedMarkedNumbers = new HashSet<>(markedList);

        String claimLockKey = "game:" + gameId + ":claim-lock";
        String endLockKey = "game:end-lock:" + roomId;

        // Lua script for claim lock
        String luaClaimLock = """
                if redis.call('exists', KEYS[1]) == 0 then
                    redis.call('set', KEYS[1], ARGV[1], 'EX', 10)
                    return 1
                else
                    return 0
                end
                """;
        RedisScript<Long> acquireClaimLockScript = RedisScript.of(luaClaimLock, Long.class);

        return reactiveRedisTemplate.execute(acquireClaimLockScript, List.of(claimLockKey), userId)
                .next()
                .flatMap(acquired -> {
                    if (acquired == null || acquired != 1) {
                        log.info("Claim lock not acquired for user {} because of lock busy", userId);
                        return Mono.error(new IllegalStateException("LOCK_BUSY"));
                    }
                    return Mono.just(true);
                })
                .retryWhen(Retry.backoff(10, Duration.ofMillis(100))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(e -> e instanceof IllegalStateException && "LOCK_BUSY".equals(e.getMessage()))
                        .onRetryExhaustedThrow((spec, signal) -> new RuntimeException("Failed to acquire claim lock after retries"))
                )
                .flatMap(ignore -> gameStateService.getGameState(roomId, agentId))
                .flatMap(state -> {
                    if (state.isEnded()) {
                        log.info("Game {} already ended", state.getGameId());
                        return sendUserError(userId, cardId, roomId, "GAME_ALREADY_COMPLETED", "Game already completed").then();
                    }

                    // Check if user is part of the game
                    return gameStateService.getAllPlayers(state.getGameId())
                            .flatMap(players -> {
                                if (!players.contains(userId)) {
                                    log.info("User {} not in game {}", userId, state.getGameId());
                                    return releaseClaimLock(claimLockKey, userId)
                                            .then(sendUserError(userId, cardId, roomId, "USER_NOT_IN_GAME", "You are not in the game"));
                                }

                                // Get server-marked numbers for validation
                                return playerStateService.getMarkedNumbers(state.getGameId(), userId, cardId)
                                        .flatMap(serverMarkedNumbers -> {
                                            if (!claimedMarkedNumbers.containsAll(serverMarkedNumbers)) {
                                                log.info("Marked numbers mismatch for user {} in game {}", userId, state.getGameId());
                                                return releaseClaimLock(claimLockKey, userId)
                                                        .then(sendUserError(userId, cardId, roomId, "MARKED_NUMBERS_MISMATCH", "Marked numbers mismatch"))
                                                        .then(createClaim(serverMarkedNumbers, state, cardId, cardInfo, dbUserId, pattern, false, "Invalid claim", agentId));
                                            }

                                            // Verify pattern
                                            return Mono.fromCallable(() ->
                                                            patternVerifier.verifyPattern(cardInfo.getNumbers(), new HashSet<>(serverMarkedNumbers), pattern))
                                                    .subscribeOn(Schedulers.boundedElastic())
                                                    .flatMap(isWinner -> {
                                                        if (!Boolean.TRUE.equals(isWinner)) {
                                                            log.info("Invalid claim for user {} in game {}", userId, state.getGameId());
                                                            return releaseClaimLock(claimLockKey, userId)
                                                                    .then(sendUserError(userId, cardId, roomId, "INVALID_BINGO_CLAIM", "Invalid claim"))
                                                                    .then(createClaim(serverMarkedNumbers, state, cardId, cardInfo, dbUserId, pattern, false, "Invalid claim", agentId));
                                                        }

                                                        // Acquire end lock to ensure only one winner
                                                        return reactiveRedisTemplate.opsForValue()
                                                                .setIfAbsent(endLockKey, "locked", Duration.ofSeconds(60))
                                                                .flatMap(endLockAcquired -> {
                                                                    if (!Boolean.TRUE.equals(endLockAcquired)) {
                                                                        log.info("Another instance is ending game {} for user {}", state.getGameId(), userId);
                                                                        return releaseClaimLock(claimLockKey, userId)
                                                                                .then(sendUserError(userId, cardId, roomId, "GAME_ENDED_BY_ANOTHER_INSTANCE", "Another instance is ending the game"))
                                                                                .then(createClaim(serverMarkedNumbers, state, cardId, cardInfo, dbUserId, pattern, false, "Another instance ending game", agentId));
                                                                    }

                                                                    // Stop number drawing loop
                                                                    Optional.ofNullable(stopLoopSinks.get(roomId)).ifPresent(sink -> sink.tryEmitEmpty());

                                                                    // Update game state
                                                                    state.setEnded(true);
                                                                    state.setStopNumberDrawing(true);
                                                                    state.setStatus(GameStatus.COMPLETED);
                                                                    state.setClaimRequested(true);

                                                                    // If BOT, only reveal winning numbers + half of other marked numbers
                                                                    Set<Integer> markedNumbers = ParticipantType.BOT.equals(participantType)
                                                                            ? new HashSet<>(patternVerifier.getWinningAndHalfOfOtherMarkedNumbers(cardInfo.getNumbers(), serverMarkedNumbers))
                                                                            : serverMarkedNumbers;

                                                                    log.info("ORIGINAL MARKED NUMBERS: {}", serverMarkedNumbers);
                                                                    log.info("WINNING NUMBERS: {}", patternVerifier.getWinningAndHalfOfOtherMarkedNumbers(cardInfo.getNumbers(), serverMarkedNumbers));


                                                                    return gameStateService.saveGameStateToRedis(state, roomId, agentId)
                                                                            .then(updateGameToDatabase(state, agentId))
                                                                            .then(Mono.defer(() -> {
                                                                                GameEndResponse response = GameEndResponse.builder()
                                                                                        .gameId(state.getGameId())
                                                                                        .roomId(roomId)
                                                                                        .cardId(cardId)
                                                                                        .playerId(Long.parseLong(userId))
                                                                                        .playerName(playerName)
                                                                                        .pattern(pattern)
                                                                                        .prizeAmount(BigDecimal.ZERO)
                                                                                        .hasWinner(true)
                                                                                        .winAt(LocalDateTime.now())
                                                                                        .markedNumbers(markedNumbers)
//                                                                                        .markedNumbers(serverMarkedNumbers)
                                                                                        .card(cardInfo)
                                                                                        .build();

                                                                                String channel = "bingo:room:" + roomId + ":stop";

                                                                                log.info("User {} won game {} with card {}", userId, gameId, cardId);

                                                                                Mono<BingoClaimDto> bingoClaimMono = createBingoClaimDto(serverMarkedNumbers, state, cardId, cardInfo, dbUserId, pattern, true, null, agentId);
                                                                                Mono<GameTransactionDto> gameTransactionMono = gameTransactionService.createGameTransactionForPrizePayout(state, dbUserId, GameTxnType.PRIZE_PAYOUT, gameId, agentId);

                                                                                return bingoClaimMono.flatMap(bingoClaim ->
                                                                                                bingoClaimService.createBingoClaim(bingoClaim)
                                                                                                        .then(reactiveRedisTemplate.convertAndSend(channel, "STOP"))
                                                                                                        .then(endGame(state, userId, response, agentId))
                                                                                                        .then(gameTransactionMono)
                                                                                        ).then()
                                                                                        .doFinally(sig -> {
                                                                                            reactiveRedisTemplate.delete(endLockKey).subscribe();
                                                                                            releaseClaimLock(claimLockKey, userId).subscribe();
                                                                                        });
                                                                            }));
                                                                });
                                                    });
                                        });
                            });
                })
                .onErrorResume(e -> releaseClaimLock(claimLockKey, userId)
                        .then(sendUserError(userId, cardId, roomId, "CLAIM_ERROR", "Failed to process bingo claim")))
                .then();
    }


    private Mono<Void> createClaim(Set<Integer> serverMarkedNumbers, GameState state, String cardId, CardInfo cardInfo, Long dbUserId, String pattern, Boolean isWinner, String error, Long agentId) {
        return createBingoClaimDto(serverMarkedNumbers, state, cardId, cardInfo, dbUserId, pattern, isWinner, error, agentId)
                .flatMap(bingoClaimService::createBingoClaim)
                .doOnSuccess(bingoClaim -> log.info(
                        "Bingo claim for game {} created successfully. Bingo claim id: {}",
                        state.getGameId(), bingoClaim.getId()))
                .doOnError(e -> log.error(
                        "Failed to create bingo claim for game {}.", state.getGameId(), e))
                .then();
    }

    private Mono<BingoClaimDto> createBingoClaimDto(
            Set<Integer> serverMarkedNumbers,
            GameState state,
            String cardId,
            CardInfo card,
            Long dbUserId,
            String pattern,
            Boolean isWinner,
            String error,
            Long agentId) {
        try {
            if (card != null) {
                // Return directly a Mono.just()
                BingoClaimDto dto = BingoClaimDto.builder()
                        .agentId(agentId)
                        .gameId(state.getGameId())
                        .card(objectMapper.writeValueAsString(card))
                        .playerId(dbUserId)
                        .pattern(GamePattern.valueOf(pattern))
                        .markedNumbers(objectMapper.writeValueAsString(serverMarkedNumbers))
                        .isWinner(isWinner)
                        .error(error)
                        .build();

                return Mono.just(dto);
            } else if (cardId != null) {
                // Use reactive call for card fetch
                return cardPoolService.getCard(state.getRoomId(), cardId)
                        .map(cardInfo -> {
                            try {
                                return BingoClaimDto.builder()
                                        .agentId(agentId)
                                        .gameId(state.getGameId())
                                        .card(objectMapper.writeValueAsString(cardInfo))
                                        .playerId(dbUserId)
                                        .pattern(GamePattern.valueOf(pattern))
                                        .markedNumbers(objectMapper.writeValueAsString(serverMarkedNumbers))
                                        .isWinner(true)
                                        .error(error)
                                        .build();
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } else {
                return Mono.error(new IllegalArgumentException("Both card and cardId are null"));
            }
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }


    // Safe lock release helper
    private Mono<Boolean> releaseClaimLock(String claimLockKey, String userId) {
        String luaReleaseLock = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;

        RedisScript<Long> releaseLockScript = RedisScript.of(luaReleaseLock, Long.class);

        return reactiveRedisTemplate.execute(releaseLockScript, List.of(claimLockKey), userId)
                .next()
                .map(result -> result != null && result == 1)
                .doOnNext(released -> {
                    if (released) {
                        log.debug("Lock released for key {}, user {}", claimLockKey, userId);
                    } else {
                        log.debug("No lock released (not owner or already deleted) for key {}, user {}", claimLockKey, userId);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error releasing lock for key {}, user {}", claimLockKey, userId, e);
                    return Mono.just(false);
                });
    }


    // Helper to send errors
    private Mono<Void> sendUserError(String userId, String cardId, Long roomId, String errorType, String message) {
        return publisher.publishUserEvent(userId,
                Map.of(
                        "type", "error",
                        "payload", Map.of(
                                "message", message,
                                "errorType", errorType,
                                "eventType", "bingo.claim",
                                "cardId", cardId,
                                "roomId", roomId
                        )
                )).then();
    }


    public Mono<GameState> getOrInitializeGame(Long roomId, String userId, Integer capacity, Long agentId) {
//        log.info("Getting or initializing game for room {} and user {}", roomId, userId);
        return gameStateService.getOrInitializeGame(roomId, userId, capacity, agentId);
    }

    public Mono<Void> markNumber(Long roomId, Long gameId, String userId, Map<String, Object> payload) {
        String cardId = (String) payload.get("cardId");
        Integer number = (Integer) payload.get("number");
        if (cardId == null || cardId.isBlank() || !payload.containsKey("number") || number == null || number < 1 || number > 75) {
            return publisher.publishUserEvent(userId, Map.of(
                    "type", "error",
                    "payload", Map.of(
                            "message", "Invalid markNumber payload",
                            "errorType", "CARD_OR_NUMBER_MISSING_OR_INVALID",
                            "roomId", roomId
                    )
            )).then();
        }

//        log.info("===================================>>>>>>>>>>>>: Marking number for room {} and user {} and cardId {}", roomId, userId, cardId);

        return playerStateService.addMarkedNumber(gameId, userId, cardId, number)
                .flatMap(updatedCard -> publisher.publishUserEvent(userId, Map.of(
                        "type", "card.markNumberResponse",
                        "payload", Map.of(
                                "cardId", cardId,
                                "marked", updatedCard
                        )
                )))
                .then();
    }

    public Mono<Void> unmarkNumber(Long roomId, Long gameId, String userId, Map<String, Object> payload) {
        String cardId = (String) payload.get("cardId");
        Integer number = (Integer) payload.get("number");
        if (cardId == null || cardId.isBlank() || !payload.containsKey("number") || number == null || number < 1 || number > 75) {
            return publisher.publishUserEvent(userId, Map.of(
                    "type", "error",
                    "payload", Map.of(
                            "message", "Invalid markNumber payload",
                            "errorType", "CARD_OR_NUMBER_MISSING_OR_INVALID",
                            "roomId", roomId
                    )
            )).then();
        }
        return playerStateService.removeMarkedNumber(gameId, userId, cardId, number)
                .flatMap(updatedCard -> publisher.publishUserEvent(userId, Map.of(
                        "type", "card.unmarkNumberResponse",
                        "payload", Map.of(
                                "cardId", cardId,
                                "marked", updatedCard
                        )
                )))
                .then();
    }


    public Mono<Integer> getMinPlayersToStart(Long roomId) {
        return roomRepository.findById(roomId)
                .map(Room::getMinPlayers)
                .onErrorMap(e -> new RuntimeException("Error getting room by id: " + roomId, e))
                .doOnSubscribe(s -> log.info("Getting min players to start for room: {}", roomId))
                .doOnSuccess(id -> log.info("Got min players to start for room: {}", roomId));
    }

    public Mono<Void> getCard(Long roomId, String cardId3, String userId) {
        log.info("Getting card {} for user {} in room {}", cardId3, userId, roomId);
        if (roomId == null || cardId3 == null || userId == null) {
            return publisher.publishUserEvent(userId, Map.of(
                    "type", "error",
                    "payload", Map.of(
                            "message", "Invalid getCard payload",
                            "errorType", "CARD_ID_OR_ROOM_ID_OR_USER_ID_MISSING"
                    )
            )).then();
        }

        return cardPoolService.getCard(roomId, cardId3)
                .flatMap(cardInfo -> publisher.publishUserEvent(userId, Map.of(
                        "type", "game.getCardResponse",
                        "payload", Map.of(
                                "cardId", cardId3,
                                "card", cardInfo
                        )
                )))
                .then() // ensure Mono<Void> return type
                .onErrorResume(ex ->
                        publisher.publishUserEvent(userId, Map.of(
                                "type", "error",
                                "payload", Map.of(
                                        "message", "Failed to get card: " + ex.getMessage(),
                                        "errorType", "CARD_FETCH_ERROR",
                                        "roomId", roomId
                                )
                        )).then()
                );
    }

}