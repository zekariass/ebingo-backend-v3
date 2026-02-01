//package com.ebingo.backend.system.redis;
//
//import com.ebingo.backend.game.entity.Room;
//import com.ebingo.backend.game.repository.RoomRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.data.redis.core.ScanOptions;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class RedisCleanupService {
//
//    private final ReactiveStringRedisTemplate redis;
//    private final RoomRepository roomRepository;
//
//    // ----------------------------
//    // Redis key patterns
//    // ----------------------------
//    private String activeBotsKey(Long roomId) {
//        return "room:" + roomId + ":activeBots";
//    }
//
//    private Flux<Long> deleteKeysByPattern(String pattern) {
//        return redis.scan(ScanOptions.scanOptions().match(pattern).count(500).build())
//                .collectList()
//                .flatMapMany(keys -> {
//                    if (keys.isEmpty()) return Flux.just(0L);
//                    return redis.unlink(keys.toArray(new String[0])).flux();
//                })
//                .doOnNext(count -> log.info("Deleted {} keys matching pattern '{}'", count, pattern))
//                .onErrorResume(e -> {
//                    log.error("Failed deleting keys for pattern '{}': {}", pattern, e.getMessage());
//                    return Flux.just(0L);
//                });
//    }
//
//    /**
//     * Cleanup all game-related keys
//     */
//    public Flux<Long> deleteAllGameKeys() {
//        return deleteKeysByPattern("game:*");
//    }
//
//    /**
//     * Cleanup all room-related keys
//     */
//    public Flux<Long> deleteAllRoomKeys() {
//        return deleteKeysByPattern("room:*");
//    }
//
//    /**
//     * Cleanup all active bot sets
//     */
//    public Flux<Long> deleteAllActiveBots() {
//        return roomRepository.findAll()
//                .filter(Room::getBotAllowed)
//                .flatMap(room -> redis.delete(activeBotsKey(room.getId())))
//                .doOnNext(count -> log.info("Cleared active bots for a room"))
//                .onErrorResume(e -> {
//                    log.warn("Failed to clear active bots: {}", e.getMessage());
//                    return Flux.just(0L);
//                });
//    }
//
//    /**
//     * Cleanup everything: game keys, room keys, active bots
//     */
//    public Mono<Void> deleteAllGameAndRoomKeys() {
//        return Flux.merge(deleteAllGameKeys(), deleteAllRoomKeys(), deleteAllActiveBots())
//                .then()
//                .doOnSuccess(v -> log.info("Global cleanup of games, rooms, and active bots completed"));
//    }
//
//    // -------------------------------------------------------
//    // Startup cleanup
//    // -------------------------------------------------------
//    @PostConstruct
//    public void cleanUpOnStartup() {
//        log.info("Starting full global Redis cleanup on startup...");
//
//        deleteAllGameAndRoomKeys()
//                .subscribeOn(Schedulers.boundedElastic())
//                .subscribe(
//                        null,
//                        err -> log.error("Error during full Redis cleanup: {}", err.getMessage()),
//                        () -> log.info("Full Redis cleanup completed successfully on startup")
//                );
//    }
//}
