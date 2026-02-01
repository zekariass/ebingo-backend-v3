//package com.ebingo.backend.game.service.sync;
//
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//public interface RoomRegistry {
//    Mono<Void> addRoom(Long roomId);
//
//    Mono<Void> removeRoom(Long roomId);
//
//    Flux<Long> getActiveRooms();
//}


package com.ebingo.backend.game.service.sync;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface RoomRegistry {

    /**
     * Register a room with its owning agent
     */
    Mono<Void> addRoom(Long roomId, Long agentId);

    /**
     * Remove a room from registry
     */
    Mono<Void> removeRoom(Long roomId);

    /**
     * Get all active rooms as roomId -> agentId
     */
    Flux<Map.Entry<Long, Long>> getActiveRooms();
}
