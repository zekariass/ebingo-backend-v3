//package com.ebingo.backend.game.service.sync;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//@ConditionalOnProperty(name = "active.games.registry", havingValue = "inMemory", matchIfMissing = true)
//@Slf4j
//public class InMemoryRoomRegistry implements RoomRegistry {
//
//    private final Set<Long> activeRooms = ConcurrentHashMap.newKeySet();
//
//    @Override
//    public Mono<Void> addRoom(Long roomId) {
//        activeRooms.add(roomId);
//        log.debug("Room added to in-memory registry: {}", roomId);
//        return Mono.empty();
//    }
//
//    @Override
//    public Mono<Void> removeRoom(Long roomId) {
//        activeRooms.remove(roomId);
//        log.debug("Room removed from in-memory registry: {}", roomId);
//        return Mono.empty();
//    }
//
//    @Override
//    public Flux<Long> getActiveRooms() {
//        // returning snapshot as a Flux
//        return Flux.fromIterable(activeRooms);
//    }
//}


package com.ebingo.backend.game.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        name = "active.games.registry",
        havingValue = "inMemory",
        matchIfMissing = true
)
@Slf4j
public class InMemoryRoomRegistry implements RoomRegistry {

    /**
     * roomId -> agentId
     */
    private final Map<Long, Long> activeRooms = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> addRoom(Long roomId, Long agentId) {
        activeRooms.put(roomId, agentId);
        log.debug("Room {} added for agent {}", roomId, agentId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> removeRoom(Long roomId) {
        Long agentId = activeRooms.remove(roomId);
        log.debug("Room {} removed (agent={})", roomId, agentId);
        return Mono.empty();
    }

    @Override
    public Flux<Map.Entry<Long, Long>> getActiveRooms() {
        // snapshot iteration
        return Flux.fromIterable(activeRooms.entrySet());
    }
}
