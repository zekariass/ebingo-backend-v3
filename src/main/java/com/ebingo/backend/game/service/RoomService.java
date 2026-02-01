package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoomService {
    Mono<RoomDto> createRoom(RoomCreateDto roomDto, Long telegramId);

    Mono<RoomWithCardPoolDto> getRoomById(Long id);

    Mono<RoomInternalDto> getRoomWithCardPoolById(Long id);

    Flux<RoomDto> getAllRooms(Long agentId);

    Flux<RoomInternalDto> getAllRoomsWIthCardPool(Long agentId);

    Mono<RoomDto> updateRoomById(Long id, RoomUpdateDto roomDto, Long agentId);

    Mono<Void> deleteRoomById(Long id, Long agentId);

    Flux<RoomInternalDto> getAllRoomsWIthCardPoolForAutoService();
}
