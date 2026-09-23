

package com.ebingo.backend.game.service.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "active.games.registry", havingValue = "redis")
@RequiredArgsConstructor
@Slf4j
public class RedisRoomRegistry implements RoomRegistry {

    private static final String ACTIVE_ROOMS_KEY = "active:rooms";

    private final ReactiveStringRedisTemplate redis;

    @Override
    public Mono<Void> addRoom(Long roomId, Long agentId) {
        return redis.opsForHash()
                .put(
                        ACTIVE_ROOMS_KEY,
                        roomId.toString(),
                        agentId.toString()
                )
                .doOnSuccess(ok ->
                        log.debug("Room {} added for agent {} in redis", roomId, agentId))
                .then();
    }

    @Override
    public Mono<Void> removeRoom(Long roomId) {
        return redis.opsForHash()
                .remove(ACTIVE_ROOMS_KEY, roomId.toString())
                .doOnSuccess(count ->
                        log.debug("Room {} removed from redis", roomId))
                .then();
    }

    @Override
    public Flux<Map.Entry<Long, Long>> getActiveRooms() {
        return redis.opsForHash()
                .entries(ACTIVE_ROOMS_KEY)
                .doOnSubscribe(s ->
                        log.debug("Fetching active rooms from redis"))
                .map(entry ->
                        Map.entry(
                                Long.valueOf(entry.getKey().toString()),
                                Long.valueOf(entry.getValue().toString())
                        )
                );
    }
}
