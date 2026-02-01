package com.ebingo.backend.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // -------------------
    // Cache a single object (Mono<T>)
    // -------------------
    @Override
    public <T> Mono<T> cacheMono(String key, Mono<T> sourceMono, Duration ttl, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .cast(clazz)
                .switchIfEmpty(
                        sourceMono.flatMap(value ->
                                redisTemplate.opsForValue()
                                        .set(key, value, ttl)
                                        .thenReturn(value)
                        )
                );
    }

    // Overload with default TTL (1 hour)
    @Override
    public <T> Mono<T> cacheMono(String key, Mono<T> sourceMono, Class<T> clazz) {
        return cacheMono(key, sourceMono, Duration.ofMinutes(5), clazz);
    }

    // -------------------
    // Cache a list of objects (Flux<T>)
    // -------------------
    @Override
    public <T> Flux<T> cacheFlux(String key, Flux<T> sourceFlux, Duration ttl, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .cast(List.class)
                .flatMapMany(list -> Flux.fromIterable((List<T>) list))
                .switchIfEmpty(
                        sourceFlux.collectList()
                                .flatMap(list -> redisTemplate.opsForValue()
                                        .set(key, list, ttl)
                                        .thenReturn(list))
                                .flatMapMany(Flux::fromIterable)
                );
    }

    // Overload with default TTL (1 hour)
    @Override
    public <T> Flux<T> cacheFlux(String key, Flux<T> sourceFlux, Class<T> clazz) {
        return cacheFlux(key, sourceFlux, Duration.ofMinutes(5), clazz);
    }

    // -------------------
    // Evict cache by key
    // -------------------
    @Override
    public Mono<Boolean> evict(String key) {
        return redisTemplate.delete(key).map(count -> count > 0);
    }

    // -------------------
    // Check if key exists
    // -------------------
    @Override
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key);
    }
}
