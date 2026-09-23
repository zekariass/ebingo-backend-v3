

package com.ebingo.backend.game.service.state;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.system.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStateService {

    private final ReactiveRedisTemplate<String, Object> redis;
    private final ReactiveHashOperations<String, String, CardInfo> hashOps;
    private final ReactiveSetOperations<String, String> setOps;

    private static final Duration PLAYER_STATE_TTL = Duration.ofHours(24);





    public Mono<Boolean> addPlayerCardId(Long gameId, String userId, String cardId) {
        String cardsKey = RedisKeys.playerCardsIdsKey(gameId, userId);
        return setOps.add(cardsKey, cardId)
                .then(redis.expire(cardsKey, PLAYER_STATE_TTL))
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("Failed to add player card ID {} for user {} in game {}: {}",
                            cardId, userId, gameId, e.getMessage(), e);
                    return Mono.just(false);
                });
    }


    public Mono<Boolean> removePlayerCardId(Long gameId, String userId, String cardId) {
        String cardsKey = RedisKeys.playerCardsIdsKey(gameId, userId);
        return setOps.remove(cardsKey, cardId)
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("Failed to remove player card ID {} for user {} in game {}: {}",
                            cardId, userId, gameId, e.getMessage(), e);
                    return Mono.just(false);
                });
    }


    public Mono<Set<String>> getPlayerCardIds(Long gameId, String userId) {
        String cardsKey = RedisKeys.playerCardsIdsKey(gameId, userId);
        return setOps.members(cardsKey)
                .collect(Collectors.toSet());
    }

    public Mono<Set<String>> getAllSelectedCardsIds(Long gameId) {
        String key = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);
        return setOps.members(key)
                .collect(Collectors.toSet());
    }

    public Mono<Boolean> addToAllPlayersSelectedCardsIds(Long gameId, String cardId) {
        String key = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);
        return setOps.add(key, cardId)
                .flatMap(added -> redis.expire(key, PLAYER_STATE_TTL).thenReturn(added > 0))
                .onErrorResume(e -> {
                    log.error("Failed to add to all players selected cards IDs for game {}: {}", gameId, e.getMessage(), e);
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> removeFromAllPlayersSelectedCardsId(Long gameId, String cardId) {
        String key = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);
        return setOps.remove(key, cardId)
                .map(removedCount -> removedCount > 0)
                .onErrorResume(e -> {
                    log.error("Failed to remove from all players selected cards IDs for game {}: {}", gameId, e.getMessage(), e);
                    return Mono.just(false);
                });
    }


    /**
     * Fetch marked numbers for a specific card.
     */
    public Mono<Set<Integer>> getMarkedNumbers(Long gameId, String userId, String cardId) {
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
        return setOps.members(markedKey)
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * Remove a specific card from player's collection.
     */
    public Mono<Boolean> removePlayerCard(Long gameId, String userId, String cardId) {
        // Only the marked numbers key
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);

        return redis.delete(markedKey)
                .map(deletedCount -> {
                    log.debug("Removed marked numbers for card {} of user {} in game {}. Keys deleted: {}",
                            cardId, userId, gameId, deletedCount);
                    return deletedCount != null && deletedCount > 0;
                })
                .defaultIfEmpty(false)
                .onErrorResume(error -> {
                    log.error("Failed to remove marked numbers for card {} of user {} in game {}: {}",
                            cardId, userId, gameId, error.getMessage(), error);
                    return Mono.just(false);
                });
    }


    public Mono<Set<Integer>> addMarkedNumber(Long gameId, String userId, String cardId, Integer number) {
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
        return setOps.add(markedKey, String.valueOf(number))
                .then(redis.expire(markedKey, PLAYER_STATE_TTL))
                .then(setOps.members(markedKey)
                        .map(Integer::valueOf)
                        .collect(Collectors.toSet())
                );
    }

    public Mono<Set<Integer>> removeMarkedNumber(Long gameId, String userId, String cardId, Integer number) {
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
        return setOps.remove(markedKey, String.valueOf(number))
                .then(setOps.members(markedKey)
                        .map(Integer::valueOf)
                        .collect(Collectors.toSet())
                );
    }


}
