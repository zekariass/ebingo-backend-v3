//package com.ebingo.backend.game.service.sync;
//
//import com.ebingo.backend.game.service.RedisPublisher;
//import com.ebingo.backend.game.service.state.GameStateService;
//import com.ebingo.backend.game.state.GameState;
//import com.ebingo.backend.system.redis.RedisKeys;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import reactor.core.Disposable;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.atomic.AtomicLong;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class GameStateSyncService implements DisposableBean {
//
//    private final RoomRegistry roomRegistry;
//    private final GameStateService gameStateService;
//    private final RedisPublisher redisPublisher;
//
//    @Value("${game.sync.interval:1000}")
//    private long syncIntervalMs;
//
//    @Value("${game.sync.concurrent:8}")
//    private int concurrency;
//
//    private final AtomicLong cycleCounter = new AtomicLong(0);
//    private Disposable schedulerDisposable;
//
//    @PostConstruct
//    public void startScheduler() {
//        log.info("GameStateSyncService starting with interval={}ms", syncIntervalMs);
//
//        schedulerDisposable =
//                Flux.interval(Duration.ofMillis(syncIntervalMs))
//                        .flatMap(tick -> roomRegistry.getActiveRooms().distinct(), 1)
//                        .flatMap(this::syncRoomSafe, concurrency)
//                        .subscribe(
//                                v -> {
//                                    long count = cycleCounter.incrementAndGet();
//                                    if (count % 60 == 0) {
//                                        log.info("State sync cycles executed: {}", count);
//                                    }
//                                },
//                                err -> log.error("Sync scheduler error: {}", err.getMessage(), err)
//                        );
//    }
//
//    /**
//     * Wraps syncRoom with error handling so one room failure doesn't stop others.
//     */
//    private Mono<Void> syncRoomSafe(Long roomId, Long agentId) {
//        return syncRoom(roomId, agentId)
//                .onErrorResume(e -> {
//                    log.error("Error syncing room {}: {}", roomId, e.getMessage(), e);
//                    return Mono.empty();
//                });
//    }
//
//    /**
//     * Syncs a single room: fetches the game state and publishes it.
//     */
//    private Mono<Void> syncRoom(Long roomId, Long agentId) {
//        return gameStateService.getGameState(roomId, agentId)
//                .filter(Objects::nonNull) // Skip null states
//                .flatMap(state -> publishState(roomId, state)
/// /                        .doOnSuccess(v -> log.info("State sync sent for room {}", roomId))
//                );
////                .switchIfEmpty(Mono.fromRunnable(() ->
//////                        log.info("No game state found for room {}, skipping sync.", roomId)
////                                log.info("")
////                ));
//    }
//
//    /**
//     * Publishes the game state to Redis.
//     */
//    private Mono<Void> publishState(Long roomId, GameState gameState) {
//        Map<String, Object> event = Map.of(
//                "type", "game.stateSync",
//                "payload", Map.of(
//                        "roomId", roomId,
//                        "gameState", gameState
//                )
//        );
//
//        String channel = RedisKeys.roomChannel(roomId);
//
//        return redisPublisher.publishEvent(channel, event)
//                .then()
//                .doOnSuccess(v -> log.debug("state.sync published for room {}", roomId))
//                .onErrorResume(e -> {
//                    log.error("Failed publishing state.sync for room {}: {}", roomId, e.getMessage());
//                    return Mono.empty();
//                });
//    }
//
//    @Override
//    public void destroy() {
//        if (schedulerDisposable != null && !schedulerDisposable.isDisposed()) {
//            schedulerDisposable.dispose();
//            log.info("GameStateSyncService scheduler disposed");
//        }
//    }
//}


package com.ebingo.backend.game.service.sync;

import com.ebingo.backend.game.service.RedisPublisher;
import com.ebingo.backend.game.service.state.GameStateService;
import com.ebingo.backend.game.state.GameState;
import com.ebingo.backend.system.redis.RedisKeys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameStateSyncService implements DisposableBean {

    private final RoomRegistry roomRegistry;
    private final GameStateService gameStateService;
    private final RedisPublisher redisPublisher;

    @Value("${game.sync.interval:1000}")
    private long syncIntervalMs;

    @Value("${game.sync.concurrent:8}")
    private int concurrency;

    private final AtomicLong executionCounter = new AtomicLong(0);
    private Disposable schedulerDisposable;

    @PostConstruct
    public void startScheduler() {
        log.info("GameStateSyncService starting (interval={}ms, concurrency={})",
                syncIntervalMs, concurrency);

        schedulerDisposable =
                Flux.interval(Duration.ofMillis(syncIntervalMs))
                        // Prevent overlapping cycles
                        .concatMap(tick -> {
                            log.debug("Starting game state sync cycle {}", tick);
                            return roomRegistry.getActiveRooms()
                                    .flatMap(entry ->
                                                    syncRoomSafe(entry.getKey(), entry.getValue()),
                                            concurrency
                                    );
                        })
                        .subscribe(
                                v -> {
                                    long count = executionCounter.incrementAndGet();
                                    if (count % 100 == 0) {
                                        log.info("Game state sync executions: {}", count);
                                    }
                                },
                                err -> log.error("Game state sync scheduler error", err)
                        );
    }

    /**
     * Error-isolated wrapper so one room failure doesn't affect others.
     */
    private Mono<Void> syncRoomSafe(Long roomId, Long agentId) {
        return syncRoom(roomId, agentId)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.error(
                            "Failed syncing room {} (agent {}): {}",
                            roomId,
                            agentId,
                            e.getMessage(),
                            e
                    );
                    return Mono.empty();
                });
    }

    /**
     * Fetches and publishes a single room's game state.
     */
    private Mono<Void> syncRoom(Long roomId, Long agentId) {
        return gameStateService.getGameState(roomId, agentId)
                .filter(Objects::nonNull)
                .flatMap(state -> publishState(roomId, state))
                .switchIfEmpty(Mono.empty());
    }

    /**
     * Publishes game state to Redis.
     */
    private Mono<Void> publishState(Long roomId, GameState gameState) {
        Map<String, Object> event = Map.of(
                "type", "game.stateSync",
                "payload", Map.of(
                        "roomId", roomId,
                        "gameState", gameState
                )
        );

        String channel = RedisKeys.roomChannel(roomId);

        return redisPublisher.publishEvent(channel, event)
                .doOnSuccess(v ->
                        log.debug("Published game state sync for room {}", roomId)
                )
                .onErrorResume(e -> {
                    log.error(
                            "Redis publish failed for room {}: {}",
                            roomId,
                            e.getMessage(),
                            e
                    );
                    return Mono.empty();
                })
                .then();
    }

    @Override
    public void destroy() {
        if (schedulerDisposable != null && !schedulerDisposable.isDisposed()) {
            schedulerDisposable.dispose();
            log.info("GameStateSyncService scheduler disposed");
        }
    }
}
