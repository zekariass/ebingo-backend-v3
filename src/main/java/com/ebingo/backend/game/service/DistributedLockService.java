package com.ebingo.backend.game.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonReactiveClient redisson;

    /**
     * Acquire a reactive lock with TTL and wait time
     */
    public Mono<RLockReactive> acquireLock(String key, Duration ttl, Duration waitTime) {
        RLockReactive lock = redisson.getLock(key);
        return lock.tryLock(waitTime.toMillis(), ttl.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .flatMap(acquired -> Boolean.TRUE.equals(acquired)
                        ? Mono.just(lock)
                        : Mono.error(new IllegalStateException("Could not acquire lock: " + key)));
    }

    /**
     * Release lock safely
     */
    public Mono<Void> releaseLock(RLockReactive lock) {
        return lock.unlock();
    }

    /**
     * Wrap a critical section in a reactive lock
     */
    public <T> Mono<T> withLock(String key, Duration ttl, Duration waitTime, Mono<T> criticalSection) {
        return acquireLock(key, ttl, waitTime)
                .flatMap(lock -> criticalSection
                        .doFinally(sig -> releaseLock(lock).subscribe())
                );
    }
}

