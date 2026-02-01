package com.ebingo.backend.game.service;

import com.ebingo.backend.game.service.state.PlayerStateService;
import com.ebingo.backend.system.redis.RedisKeys;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisTimeoutException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSelectionService {

    private final ReactiveStringRedisTemplate redis;
    private final RedisPublisher publisher;
    private final CardPoolService cardPoolService;
    private final PlayerStateService playerStateService;


    private static final String CLAIM_SCRIPT =
            "local ownerKey = KEYS[1]\n" +
                    "local playerSelectedCardsKey = KEYS[2]\n" +
                    "local allPlayersSelectedCardsKey = KEYS[3]\n" +
                    "local cardLockKey = KEYS[4]\n" +
                    "local userLockKey = KEYS[5]\n" +
                    "\n" +
                    "local cardId = ARGV[1]\n" +
                    "local userId = ARGV[2]\n" +
                    "local maxPerUser = tonumber(ARGV[3])\n" +
                    "local lockTtl = tonumber(ARGV[4])\n" +
                    "local ownerTtl = tonumber(ARGV[5])\n" +
                    "\n" +
                    "-- Acquire or refresh card-level lock\n" +
                    "local cardLockAcquired = redis.call('set', cardLockKey, userId, 'NX', 'EX', lockTtl)\n" +
                    "if not cardLockAcquired then\n" +
                    "    local currentLocker = redis.call('get', cardLockKey)\n" +
                    "    if currentLocker ~= userId then\n" +
                    "        return 'CARD_LOCKED'\n" +
                    "    end\n" +
                    "    redis.call('expire', cardLockKey, lockTtl)\n" +
                    "end\n" +
                    "\n" +
                    "-- Check if card is already taken\n" +
                    "if redis.call('exists', ownerKey) == 1 or redis.call('sismember', allPlayersSelectedCardsKey, cardId) == 1 then\n" +
                    "    redis.call('del', cardLockKey)\n" +
                    "    redis.call('del', userLockKey)\n" +
                    "    return 'CARD_TAKEN'\n" +
                    "end\n" +
                    "\n" +
                    "-- Acquire or refresh user-level lock\n" +
                    "local userLockAcquired = redis.call('set', userLockKey, userId, 'NX', 'EX', lockTtl)\n" +
                    "if not userLockAcquired then\n" +
                    "    local currentUserLocker = redis.call('get', userLockKey)\n" +
                    "    if currentUserLocker ~= userId then\n" +
                    "        redis.call('del', cardLockKey)\n" +
                    "        return 'USER_BUSY'\n" +
                    "    end\n" +
                    "    redis.call('expire', userLockKey, lockTtl)\n" +
                    "end\n" +
                    "\n" +
                    "-- Enforce per-user card limit\n" +
                    "local count = redis.call('scard', playerSelectedCardsKey)\n" +
                    "if tonumber(count) >= maxPerUser then\n" +
                    "    redis.call('del', cardLockKey)\n" +
                    "    redis.call('del', userLockKey)\n" +
                    "    return 'USER_LIMIT'\n" +
                    "end\n" +
                    "\n" +
                    "-- Claim the card atomically\n" +
                    "redis.call('set', ownerKey, userId, 'EX', ownerTtl)\n" +
                    "redis.call('sadd', playerSelectedCardsKey, cardId)\n" +
                    "redis.call('sadd', allPlayersSelectedCardsKey, cardId)\n" +
                    "\n" +
                    "-- Cleanup locks\n" +
                    "redis.call('del', cardLockKey)\n" +
                    "redis.call('del', userLockKey)\n" +
                    "\n" +
                    "return 'OK';";

    private static final RedisScript<String> CLAIM_SCRIPT_OBJ = RedisScript.of(CLAIM_SCRIPT, String.class);

    public Mono<String> claimCard(Long roomId, Long gameId, String userId, String cardId, int maxCardsPerPlayer) {
        if (cardId == null || userId == null || roomId == null) {
            log.info("Cannot claim card: missing required params userId={}, roomId={}, cardId={}", userId, roomId, cardId);
            return Mono.just("Invalid request parameters");
        }

        final String ownerKey = RedisKeys.cardOwnerKey(gameId, cardId);
        final String playerSelectedCardsKey = RedisKeys.playerCardsIdsKey(gameId, userId);
        final String allPlayersSelectedCardsKey = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);
        final String cardLockKey = RedisKeys.cardLockKey(gameId, cardId);
        final String userLockKey = RedisKeys.userLockKey(gameId, userId);

        final int lockTtlSeconds = 10;
        final int ownerTtlSeconds = 600;

        return redis.execute(
                        CLAIM_SCRIPT_OBJ,
                        List.of(ownerKey, playerSelectedCardsKey, allPlayersSelectedCardsKey, cardLockKey, userLockKey),
                        cardId, userId,
                        String.valueOf(maxCardsPerPlayer),
                        String.valueOf(lockTtlSeconds),
                        String.valueOf(ownerTtlSeconds)
                )
                .next()
                .flatMap(result -> {
                    if (result == null) {
                        log.error("Card claim returned null for user {} card {}", userId, cardId);
                        return Mono.just("Claim operation failed");
                    }
                    switch (result) {
                        case "OK":
                            return Mono.just("OK");
                        case "CARD_TAKEN":
                            return Mono.just("Card is already taken");
                        case "USER_LIMIT":
                            return Mono.just("You have reached the maximum number of cards");
                        case "USER_BUSY":
                            return Mono.just("Please wait before making another selection");
                        case "CARD_LOCKED":
                            return Mono.just("Card is being claimed by another user");
                        default:
                            return Mono.just("Unexpected error: " + result);
                    }
                })
                .onErrorResume(err -> {
                    log.error("Error claiming card {} for user {} room {}: {}", cardId, userId, roomId, err.getMessage(), err);
                    return cleanupLocks(gameId, cardId, userId)
                            .then(Mono.just("Internal server error"));
                });
    }


    private static final String RELEASE_SCRIPT =
            "local ownerKey = KEYS[1]\n" +
                    "local playerSelectedCardsKey = KEYS[2]\n" +
                    "local allPlayersSelectedCardsKey = KEYS[3]\n" +
                    "local cardLockKey = KEYS[4]\n" +
                    "local userLockKey = KEYS[5]\n" +
                    "\n" +
                    "local cardId = ARGV[1]\n" +
                    "local userId = ARGV[2]\n" +
                    "local lockTtl = tonumber(ARGV[3])\n" +
                    "\n" +
                    "-- Acquire card lock\n" +
                    "local cardLockAcquired = redis.call('set', cardLockKey, userId, 'NX', 'EX', lockTtl)\n" +
                    "if not cardLockAcquired then\n" +
                    "    local currentLocker = redis.call('get', cardLockKey)\n" +
                    "    if currentLocker ~= userId then\n" +
                    "        return 'CARD_LOCKED'\n" +
                    "    end\n" +
                    "    redis.call('expire', cardLockKey, lockTtl)\n" +
                    "end\n" +
                    "\n" +
                    "-- Acquire user lock\n" +
                    "local userLockAcquired = redis.call('set', userLockKey, userId, 'NX', 'EX', lockTtl)\n" +
                    "if not userLockAcquired then\n" +
                    "    local currentUser = redis.call('get', userLockKey)\n" +
                    "    if currentUser ~= userId then\n" +
                    "        redis.call('del', cardLockKey)\n" +
                    "        return 'USER_BUSY'\n" +
                    "    end\n" +
                    "    redis.call('expire', userLockKey, lockTtl)\n" +
                    "end\n" +
                    "\n" +
                    "-- Check ownership\n" +
                    "local owner = redis.call('get', ownerKey)\n" +
                    "redis.call('srem', playerSelectedCardsKey, cardId)\n" +
                    "redis.call('srem', allPlayersSelectedCardsKey, cardId)\n" +
                    "if owner == nil then\n" +
                    "    redis.call('del', cardLockKey)\n" +
                    "    redis.call('del', userLockKey)\n" +
                    "    return 'FORCED_RELEASE'\n" +
                    "end\n" +
                    "if owner ~= userId then\n" +
                    "    redis.call('del', cardLockKey)\n" +
                    "    redis.call('del', userLockKey)\n" +
                    "    return 'NOT_OWNER'\n" +
                    "end\n" +
                    "\n" +
                    "-- Normal release\n" +
                    "redis.call('del', ownerKey)\n" +
                    "redis.call('del', cardLockKey)\n" +
                    "redis.call('del', userLockKey)\n" +
                    "\n" +
                    "return 'OK';";

    private static final RedisScript<String> RELEASE_SCRIPT_OBJ = RedisScript.of(RELEASE_SCRIPT, String.class);

    public Mono<String> releaseCard(Long roomId, Long gameId, String userId, String cardId) {
        if (cardId == null || userId == null || roomId == null) {
            log.info("Cannot release card: missing required params userId={}, roomId={}, cardId={}", userId, roomId, cardId);
            return Mono.just("Invalid request parameters");
        }

        final String ownerKey = RedisKeys.cardOwnerKey(gameId, cardId);
        final String playerSelectedCardsKey = RedisKeys.playerCardsIdsKey(gameId, userId);
        final String allPlayersSelectedCardsKey = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);
        final String cardLockKey = RedisKeys.cardLockKey(gameId, cardId);
        final String userLockKey = RedisKeys.userLockKey(gameId, userId);

        final int lockTtlSeconds = 5;

        return redis.execute(
                        RELEASE_SCRIPT_OBJ,
                        List.of(ownerKey, playerSelectedCardsKey, allPlayersSelectedCardsKey, cardLockKey, userLockKey),
                        cardId, userId, String.valueOf(lockTtlSeconds)
                )
                .next()
                .flatMap(result -> {
                    if (result == null) {
                        log.error("Card release returned null for user {} card {}", userId, cardId);
                        return Mono.just("Release operation failed");
                    }
                    switch (result) {
                        case "OK":
                            return Mono.just("OK");
                        case "FORCED_RELEASE":
                            log.warn("Card {} had expired ownerKey but was cleaned up for user {}", cardId, userId);
                            return Mono.just("Card released (ownerKey expired)");
                        case "NOT_OWNER":
                            return Mono.just("You do not own this card");
                        case "USER_BUSY":
                            return Mono.just("Please wait before releasing another card");
                        case "CARD_LOCKED":
                            return Mono.just("Card is currently locked by another operation");
                        default:
                            return Mono.just("Unexpected error: " + result);
                    }
                })
                .onErrorResume(err -> {
                    log.error("Error releasing card {} for user {} room {}: {}", cardId, userId, roomId, err.getMessage(), err);
                    return Mono.just("Internal server error");
                });
    }


    private Mono<Void> handleSuccessfulRelease(Long roomId, Long gameId, String userId, String cardId, Set<String> selectedCards) {
        return Mono.when(
                playerStateService.removePlayerCard(gameId, userId, cardId),
                publishCardReleased(roomId, cardId, userId, selectedCards),
                updateGameStateIfNeeded(roomId, gameId, userId)
        ).then();
    }

    private Mono<Void> handleReleaseError(Long roomId, Long gameId, String userId, String cardId, String errorCode) {
        return publishCardError(roomId, userId, cardId, "Failed to release card: " + errorCode, errorCode);
    }

    private boolean isRetryableError(Throwable error) {
        return error instanceof RedisConnectionException ||
                error instanceof RedisCommandTimeoutException ||
                error instanceof RedisTimeoutException;
    }

    private Mono<Void> updateGameStateIfNeeded(Long roomId, Long gameId, String userId) {
        // Optional: Update game state if card release affects game logic
        return Mono.empty();
    }


    private Mono<Void> publishSuccess(Long roomId, String cardId, String userId, Set<String> selectedCards) {
        return publisher.publishEvent(RedisKeys.roomChannel(roomId),
                Map.of(
                        "type", "game.cardSelected",
                        "payload", Map.of(
                                "cardId", cardId,
                                "playerId", Long.valueOf(userId),
                                "message", "Card claimed successfully",
                                "selectedCards", selectedCards
                        )
                )).then();
    }

    private Mono<Void> publishCardReleased(Long roomId, String cardId, String userId, Set<String> selectedCards) {
        return publisher.publishEvent(RedisKeys.roomChannel(roomId),
                Map.of(
                        "type", "game.cardReleased",
                        "payload", Map.of(
                                "cardId", cardId,
                                "userId", userId,
                                "message", "Card released successfully",
                                "selectedCards", selectedCards
                        )
                )).then();
    }

    private Mono<Void> cleanupLocks(Long gameId, String cardId, String userId) {
        String cardLockKey = RedisKeys.cardLockKey(gameId, cardId);
        String userLockKey = RedisKeys.userLockKey(gameId, userId);

        return Mono.when(
                redis.delete(cardLockKey)
                        .doOnError(err -> log.warn("Failed to cleanup card lock for {}: {}", cardId, err.getMessage()))
                        .onErrorResume(err -> Mono.empty()),

                redis.delete(userLockKey)
                        .doOnError(err -> log.warn("Failed to cleanup user lock for {}: {}", userId, err.getMessage()))
                        .onErrorResume(err -> Mono.empty())
        ).then();
    }

    private Mono<Void> publishCardError(Long roomId, String userId, String cardId, String message, String errorType) {
        return publisher.publishUserEvent(userId,
                Map.of(
                        "type", "error",
                        "payload", Map.of(
                                "message", message,
                                "roomId", roomId,
                                "cardId", cardId == null ? "" : cardId,
                                "errorType", errorType
                        )
                )).then();
    }

}