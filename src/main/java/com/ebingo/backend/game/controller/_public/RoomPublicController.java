package com.ebingo.backend.game.controller._public;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.game.dto.RoomDto;
import com.ebingo.backend.game.dto.RoomWithCardPoolDto;
import com.ebingo.backend.game.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/rooms")
@Tag(name = "Room Secured Controller", description = "Room Secured Controller")
@RequireAccessToken
public class RoomPublicController {
    private final RoomService roomService;

    public RoomPublicController(RoomService roomService) {
        this.roomService = roomService;
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Get room by ID")
    public Mono<ResponseEntity<ApiResponse<RoomWithCardPoolDto>>> getRoomById(
            @Parameter(required = true, description = "Room ID") @PathVariable Long id,
            ServerWebExchange exchange) {

        return roomService.getRoomById(id)
                .map(room -> ApiResponse.<RoomWithCardPoolDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Room retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(room)
                        .build()
                )
                .map(ResponseEntity::ok)
                .switchIfEmpty(
                        Mono.just(
                                ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.<RoomWithCardPoolDto>builder()
                                                .statusCode(HttpStatus.NOT_FOUND.value())
                                                .success(false)
                                                .message("Room not found")
                                                .path(exchange.getRequest().getPath().value())
                                                .timestamp(Instant.now())
                                                .data(null)
                                                .build()
                                        )
                        )
                );
    }


    @GetMapping
    @Operation(summary = "Get all rooms", description = "Get all rooms")
    public Mono<ResponseEntity<ApiResponse<List<RoomDto>>>> getAllRooms(
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
}
