package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.system.redis.RedisKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardPoolService {

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper;

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
}
