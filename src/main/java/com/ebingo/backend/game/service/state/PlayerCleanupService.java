package com.ebingo.backend.game.service.state;

import com.ebingo.backend.game.service.CardSelectionService;
import com.ebingo.backend.system.redis.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class PlayerCleanupService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final CardSelectionService cardSelectionService;

    public PlayerCleanupService(@Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, CardSelectionService cardSelectionService) {
        this.redisTemplate = redisTemplate;
        this.cardSelectionService = cardSelectionService;
    }

    public Mono<List<String>> removePlayerFromGame(Long roomId, Long gameId, String userId) {
        String playerCardsIdsKey = RedisKeys.playerCardsIdsKey(gameId, userId);

        ReactiveSetOperations<String, String> setOps = redisTemplate.opsForSet();
        String allPlayersSelectedCardsKey = RedisKeys.allPlayersSelectedCardsIdsKey(gameId);

        return setOps.members(playerCardsIdsKey)
                .collectList()
                .flatMap(cardIds -> {
                    if (cardIds.isEmpty()) {
//                        log.info("No cards to release for user {} in game {}", userId, gameId);
                        return deleteRemainingPlayerKeys(gameId, userId)
                                .then(Mono.just(cardIds));
                    }

                    Mono<Void> removeFromAllSelected = Flux.fromIterable(cardIds)
                            .flatMap(cardId -> setOps.remove(allPlayersSelectedCardsKey, cardId))
                            .then();

                    // 1️⃣ Release each card using CardSelectionService
                    Mono<Void> releaseCards = Flux.fromIterable(cardIds)
                            .flatMap(cardId -> cardSelectionService.releaseCard(roomId, gameId, userId, cardId))
                            .then();

                    // 2️⃣ Delete remaining state keys for the player
                    Mono<Void> deleteKeys = deleteRemainingPlayerKeys(gameId, userId);

                    // 3️⃣ Delete marked numbers for all cards
                    Mono<Void> deleteMarkedNumbers = Flux.fromIterable(cardIds)
                            .flatMap(cardId -> redisTemplate.delete(
                                    RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId)))
                            .then();

                    Mono<Void> removePlayerFromRoom = setOps.remove(RedisKeys.roomPlayersKey(roomId), userId).then();

                    return Mono.when(removeFromAllSelected, releaseCards, deleteKeys, deleteMarkedNumbers, removePlayerFromRoom)
                            .then(Mono.just(cardIds));
                });
    }

    private Mono<Void> deleteRemainingPlayerKeys(Long gameId, String userId) {
        return Mono.when(
//                redisTemplate.delete(RedisKeys.playerCardsKey(gameId, userId)),
                redisTemplate.delete(RedisKeys.playerCardsIdsKey(gameId, userId)),
                redisTemplate.delete(RedisKeys.userOwnedCardsKey(gameId, userId))
        ).then();
    }

}
