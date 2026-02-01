package com.ebingo.backend.system.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface CacheService {
    // -------------------
    // Cache a single object (Mono<T>)
    // -------------------
    <T> Mono<T> cacheMono(String key, Mono<T> sourceMono, Duration ttl, Class<T> clazz);

    // Overload with default TTL (1 hour)
    <T> Mono<T> cacheMono(String key, Mono<T> sourceMono, Class<T> clazz);

    // -------------------
    // Cache a list of objects (Flux<T>)
    // -------------------
    <T> Flux<T> cacheFlux(String key, Flux<T> sourceFlux, Duration ttl, Class<T> clazz);

    // Overload with default TTL (1 hour)
    <T> Flux<T> cacheFlux(String key, Flux<T> sourceFlux, Class<T> clazz);

    // -------------------
    // Evict cache by key
    // -------------------
    Mono<Boolean> evict(String key);

    // -------------------
    // Check if key exists
    // -------------------
    Mono<Boolean> exists(String key);
}
