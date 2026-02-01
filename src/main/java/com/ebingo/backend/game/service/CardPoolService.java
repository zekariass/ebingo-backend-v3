package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.repository.GameRepository;
import com.ebingo.backend.game.utils.BingoCardGenerator;
import com.ebingo.backend.system.redis.RedisKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPoolService {

    private final ReactiveStringRedisTemplate redis;
    private final ReactiveSetOperations<String, String> setOps;
    private final ObjectMapper mapper;
    private final GameRepository gameRepo;

//    public Mono<Set<CardInfo>> ensurePool(Long roomId, int capacity) {
//        return redis.opsForValue().get(RedisKeys.currentCardPoolKey(roomId))
//                .flatMap(json -> {
//                    try {
//                        Set<CardInfo> cards = mapper.readValue(json, new TypeReference<Set<CardInfo>>() {
//                        });
//                        return Mono.just(cards);
//                    } catch (Exception e) {
//                        return Mono.error(e);
//                    }
//                })
//                .switchIfEmpty(generateAndStore(roomId, capacity));
//    }

//    public Mono<Set<CardInfo>> regenerate(Long roomId, int capacity) {
//        return generateAndStore(roomId, capacity);
//    }


//    public Mono<List<CardInfo>> generateAndStoreCurrentPool(Long roomId, int capacity) {
//
//        List<CardInfo> cards = BingoCardGenerator.generateCardPool(capacity)
//                .stream()
//                .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new HashSet<>(), false))
//                .collect(Collectors.toList()); // <Character, List<Integer>>

    /// /        List<CardInfo> cards = IntStream.range(0, capacity)
    /// /                .mapToObj(i -> new CardInfo(UUID.randomUUID().toString(), generateCardNumbers(), new HashSet<>(), false))
    /// /                .collect(Collectors.toSet());
//        try {
//            String json = mapper.writeValueAsString(cards);
//            return redis.opsForValue().set(RedisKeys.currentCardPoolKey(roomId), json)
//                    .flatMap(ok -> {
//                        // store each card record individually (helpful lookups)
//                        return Flux.fromIterable(cards)
//                                .flatMap(card -> {
//                                    try {
//                                        String cj = mapper.writeValueAsString(card);
//                                        return redis.opsForValue().set(RedisKeys.roomCardKey(roomId, card.getCardId()), cj)
//                                                .then(redis.opsForSet().add(RedisKeys.roomCardsSetKey(roomId), card.getCardId()));
//                                    } catch (Exception e) {
//                                        return Mono.error(e);
//                                    }
//                                }).then(Mono.just(cards));
//                    });
//        } catch (Exception e) {
//            return Mono.error(e);
//        }
//    }
//    public Mono<List<CardInfo>> generateAndStoreCurrentPool(Long roomId, int capacity) {
//        // 1. Generate the card pool synchronously
//
//        List<CardInfo> cards = BingoCardGenerator.generateCardPool(capacity)
//                .stream()
//                .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new HashSet<>()))
//                .collect(Collectors.toList());
//
//        // 2. Serialize and store the entire pool
//        Mono<Void> storePoolMono = Mono.fromCallable(() -> mapper.writeValueAsString(cards))
//                .subscribeOn(Schedulers.boundedElastic())
//                .flatMap(json -> redis.opsForValue().set(RedisKeys.currentCardPoolKey(roomId), json))
//                .then();
//
//
//        // 3. Serialize and store each card individually in parallel
//        Flux<Void> storeCardsFlux = Flux.fromIterable(cards)
//                .flatMap(card ->
//                                Mono.fromCallable(() -> mapper.writeValueAsString(card))
//                                        .subscribeOn(Schedulers.boundedElastic())
//                                        .flatMap(cj ->
//                                                        redis.opsForValue().set(RedisKeys.roomCardKey(roomId, card.getCardId()), cj)
////                                                        .then(redis.expire(RedisKeys.roomCardKey(roomId, card.getCardId()), Duration.ofDays(1)))
//                                                                .then(redis.opsForSet().add(RedisKeys.roomCardsSetKey(roomId), card.getCardId()))
//                                        )

    /// /                                        .then(redis.expire(RedisKeys.roomCardsSetKey(roomId), Duration.ofDays(1)))
//                        , 10) // parallelism = 10, adjust as needed
//                .thenMany(Flux.empty()); // we only care about completion
//
//        // 4. Combine pool storage and individual card storage
//        return storePoolMono
//                .then(storeCardsFlux.then(Mono.just(cards))); // return list of cards after all stored
//    }
    public Mono<List<CardInfo>> generateAndStoreCurrentPool(Long roomId, int capacity) {

        List<CardInfo> cards = BingoCardGenerator.generateCardPool(capacity)
                .stream()
                .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new HashSet<>()))
                .collect(Collectors.toList());

        Mono<Void> deleteMono = redis.delete(
                RedisKeys.currentCardPoolKey(roomId),
                RedisKeys.roomCardsSetKey(roomId)
        ).then();

        Mono<Void> storePoolMono = Mono.fromCallable(() -> mapper.writeValueAsString(cards))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(json -> redis.opsForValue().set(RedisKeys.currentCardPoolKey(roomId), json))
                .then();

        Mono<Void> storeCardsMono = Flux.fromIterable(cards)
                .flatMap(card ->
                                Mono.fromCallable(() -> mapper.writeValueAsString(card))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(cj ->
                                                redis.opsForValue().set(RedisKeys.roomCardKey(roomId, card.getCardId()), cj)
                                                        .then(redis.opsForSet().add(RedisKeys.roomCardsSetKey(roomId), card.getCardId()))
                                        )
                        , 10
                )
                .then();

        return deleteMono
                .then(storePoolMono)
                .then(storeCardsMono)
                .then(Mono.just(cards));
    }


    public Mono<CardInfo> getCard(Long roomId, String cardId) {
        return redis.opsForValue().get(RedisKeys.roomCardKey(roomId, cardId))
                .flatMap(json -> {
                    try {
                        CardInfo card = mapper.readValue(json, CardInfo.class);
                        return Mono.just(card);
                    } catch (Exception e) {
                        return Mono.empty(); // swallow parse error -> no card
                    }
                })
                .switchIfEmpty(Mono.empty()); // no value in Redis
    }


    public Mono<List<CardInfo>> getCurrentPool(Long roomId) {
        return redis.opsForValue().get(RedisKeys.currentCardPoolKey(roomId))
                .flatMap(json -> {
                    try {
                        List<CardInfo> cards = mapper.readValue(json, new TypeReference<List<CardInfo>>() {
                        });
                        return Mono.just(cards);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .switchIfEmpty(Mono.just(List.of())); // no pool yet
    }


    public Mono<Void> deleteCurrentCardPool(Long roomId) {
        return redis.opsForValue().delete(RedisKeys.currentCardPoolKey(roomId)).then();
    }

    public Mono<Boolean> cardExistInRoom(String cardId, Long roomId) {
        String roomCardsSetKey = RedisKeys.roomCardsSetKey(roomId);
        return redis.opsForSet().isMember(roomCardsSetKey, cardId);
    }


    public Mono<Set<String>> addSelectedCard(Long gameId, String cardId) {
        String selectedCardKey = RedisKeys.selectedCardsKey(gameId);
        return setOps.add(selectedCardKey, cardId)
                .then(setOps.members(selectedCardKey).map(String::valueOf).collect(Collectors.toSet()));
    }


    public Mono<Set<String>> removeSelectedCard(Long gameId, String cardId) {
        String selectedCardKey = RedisKeys.selectedCardsKey(gameId);
        return setOps.remove(selectedCardKey, cardId) // remove the card
                .then(
                        setOps.members(selectedCardKey)
                                .map(Object::toString)
                                .collect(Collectors.toSet())
                );
    }


    public Mono<Set<String>> getSelectedCards(Long gameId) {
        String selectedCardKey = RedisKeys.selectedCardsKey(gameId);
        return setOps.members(selectedCardKey)
                .map(Object::toString)
                .collect(Collectors.toCollection(HashSet::new));
    }


    public Mono<Set<String>> getAllCardIds(Long roomId) {
        String roomCardsSetKey = RedisKeys.roomCardsSetKey(roomId);
        return setOps.members(roomCardsSetKey)
                .map(Object::toString)
                .collect(Collectors.toCollection(HashSet::new));
    }

//    public Mono<Set<String>> getPlayerCardsIds(Long gameId, String userId) {
//        String playerCardsKey = RedisKeys.playerCardsIdsKey(gameId, userId);
//        return setOps.members(playerCardsKey)
//                .map(Object::toString)
//                .collect(Collectors.toCollection(HashSet::new));
//    }
}
