package com.ebingo.backend.game.repository;

import com.ebingo.backend.game.entity.Room;
import com.ebingo.backend.game.enums.RoomStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RoomRepository extends ReactiveCrudRepository<Room, Long> {

    Flux<Room> findByStatusAndAgentIdOrderByEntryFee(RoomStatus roomStatus, Long agentId);

    Flux<Room> findByStatusOrderByEntryFee(RoomStatus roomStatus);
}
