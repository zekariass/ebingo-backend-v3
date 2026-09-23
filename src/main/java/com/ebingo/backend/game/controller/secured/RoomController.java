package com.ebingo.backend.game.controller.secured;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.telegram.TelegramAuthVerifier;
import com.ebingo.backend.game.dto.RoomCreateDto;
import com.ebingo.backend.game.dto.RoomDto;
import com.ebingo.backend.game.dto.RoomUpdateDto;
import com.ebingo.backend.game.dto.RoomWithCardPoolDto;
import com.ebingo.backend.game.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/secured/rooms")
@Tag(name = "Room Secured Controller", description = "Room Secured Controller")
@RequireAccessToken
public class RoomController {
    private final RoomService roomService;
    private final TelegramAuthVerifier telegramAuthVerifier;
    private final ObjectMapper objectMapper;

    public RoomController(RoomService roomService, TelegramAuthVerifier telegramAuthVerifier, ObjectMapper objectMapper) {
        this.roomService = roomService;
        this.telegramAuthVerifier = telegramAuthVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Create room", description = "Create room")
    public Mono<ResponseEntity<ApiResponse<RoomDto>>> createRoom(
            @Valid @RequestBody RoomCreateDto roomDto,
            @RequestParam Long telegramId,
//            @RequestHeader(value = "x-init-data", required = true) String telegramInitData,
            ServerWebExchange exchange
    ) {
        return roomService.createRoom(roomDto, telegramId)
                .map(createdRoom -> ApiResponse.<RoomDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Room created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(createdRoom)
                        .build()
                )
                .map(response -> ResponseEntity.status(201).body(response));
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Get room by ID")
    public Mono<ResponseEntity<ApiResponse<RoomWithCardPoolDto>>> getRoomById(
            @Parameter(required = true, description = "Room ID") @PathVariable Long id,
            @Parameter(description = "Agent ID") @RequestParam(required = false) Long agentId,
//            @RequestHeader(value = "x-init-data", required = true) String telegramInitData,
            ServerWebExchange exchange) {

        return roomService.getRoomById(id, agentId)
                .map(room -> ApiResponse.<RoomWithCardPoolDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(room)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @GetMapping
    @Operation(summary = "Get all rooms", description = "Get all rooms")
    public Mono<ResponseEntity<ApiResponse<List<RoomDto>>>> getAllRooms(
//            @RequestHeader(value = "x-init-data", required = true) String telegramInitData,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {

        return roomService.getAllRooms(agentId)
                .collectList()
                .map(rooms -> ApiResponse.<List<RoomDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Rooms retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(rooms)
                        .build())
                .map(ResponseEntity::ok);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update room by ID", description = "Update room by ID")
    public Mono<ResponseEntity<ApiResponse<RoomDto>>> updateRoomById(
            @Parameter(required = true, description = "Room ID") @PathVariable Long id,
            @Parameter(required = true, description = "Agent ID") @RequestParam Long agentId,
            @Parameter(required = true, description = "Room") @Valid @RequestBody RoomUpdateDto roomDto,
            ServerWebExchange exchange) {

        return roomService.updateRoomById(id, roomDto, agentId)
                .map(updatedRoom -> ApiResponse.<RoomDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room updated successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(updatedRoom)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room by ID", description = "Delete room by ID")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteRoomById(
            @Parameter(required = true, description = "Room ID") @PathVariable Long id,
            @RequestParam Long agentId,
//            @RequestHeader(value = "x-init-data", required = true) String telegramInitData,
            ServerWebExchange exchange
    ) {
        return roomService.deleteRoomById(id, agentId)
                .then(Mono.fromSupplier(() -> ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room deleted successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .build()
                ))
                .map(ResponseEntity::ok);
    }

}
