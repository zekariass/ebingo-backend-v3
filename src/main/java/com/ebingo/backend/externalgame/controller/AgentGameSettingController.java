package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.externalgame.dto.AgentGameSettingDto;
import com.ebingo.backend.externalgame.dto.UpdateAgentGameModesRequest;
import com.ebingo.backend.externalgame.service.AgentGameSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Controller for managing agent game settings.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/external-games/game-settings")
@RequireAccessToken
@Tag(name = "Agent Game Settings", description = "Manage agent-specific game configurations")
public class AgentGameSettingController {

    private final AgentGameSettingService settingService;

    @GetMapping
    @Operation(
            summary = "Get agent game modes",
            description = "Retrieve the list of enabled game modes for a specific agent"
    )
    public Mono<ResponseEntity<ApiResponse<AgentGameSettingDto>>> getAgentGameModes(
            @Parameter(description = "Agent ID", required = true)
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {
        log.info("GET request to fetch game modes for agent: {}", agentId);

        return settingService.getAgentGameModes(agentId)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<AgentGameSettingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Agent game modes retrieved successfully")
                                .data(dto)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ))
                .onErrorResume(e -> {
                    log.error("Error fetching game modes for agent {}: {}", agentId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.<AgentGameSettingDto>builder()
                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .success(false)
                                    .message("Failed to retrieve agent game modes: " + e.getMessage())
                                    .path(exchange.getRequest().getPath().value())
                                    .timestamp(Instant.now())
                                    .build()
                    ));
                });
    }

    @GetMapping("/{agentId}")
    @Operation(
            summary = "Get agent game modes (path variant)",
            description = "Retrieve the list of enabled game modes for a specific agent"
    )
    public Mono<ResponseEntity<ApiResponse<AgentGameSettingDto>>> getAgentGameModesByPath(
            @Parameter(description = "Agent ID", required = true)
            @PathVariable Long agentId,
            ServerWebExchange exchange
    ) {
        log.info("GET request to fetch game modes for agent (path): {}", agentId);

        return settingService.getAgentGameModes(agentId)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<AgentGameSettingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Agent game modes retrieved successfully")
                                .data(dto)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ))
                .onErrorResume(e -> {
                    log.error("Error fetching game modes for agent {}: {}", agentId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.<AgentGameSettingDto>builder()
                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .success(false)
                                    .message("Failed to retrieve agent game modes: " + e.getMessage())
                                    .path(exchange.getRequest().getPath().value())
                                    .timestamp(Instant.now())
                                    .build()
                    ));
                });
    }

    @PutMapping
    @Operation(
            summary = "Update agent game modes",
            description = "Update the list of enabled game modes for a specific agent. Creates new settings if none exist."
    )
    public Mono<ResponseEntity<ApiResponse<AgentGameSettingDto>>> updateAgentGameModes(
            @Parameter(description = "Update request containing agent ID and game modes", required = true)
            @Valid @RequestBody UpdateAgentGameModesRequest request,
            ServerWebExchange exchange
    ) {
        log.info("PUT request to update game modes for agent {}: {}", request.getAgentId(), request.getGameModes());

        return settingService.updateAgentGameModes(request)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<AgentGameSettingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Agent game modes updated successfully")
                                .data(dto)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Invalid request to update game modes for agent {}: {}", request.getAgentId(), e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                            ApiResponse.<AgentGameSettingDto>builder()
                                    .statusCode(HttpStatus.BAD_REQUEST.value())
                                    .success(false)
                                    .message(e.getMessage())
                                    .path(exchange.getRequest().getPath().value())
                                    .timestamp(Instant.now())
                                    .build()
                    ));
                })
                .onErrorResume(e -> {
                    log.error("Error updating game modes for agent {}: {}", request.getAgentId(), e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.<AgentGameSettingDto>builder()
                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .success(false)
                                    .message("Failed to update agent game modes: " + e.getMessage())
                                    .path(exchange.getRequest().getPath().value())
                                    .timestamp(Instant.now())
                                    .build()
                    ));
                });
    }

    @GetMapping("/check")
    @Operation(
            summary = "Check if game mode is enabled",
            description = "Check if a specific game mode is enabled for an agent"
    )
    public Mono<ResponseEntity<ApiResponse<Boolean>>> isGameModeEnabled(
            @Parameter(description = "Agent ID", required = true)
            @RequestParam Long agentId,
            @Parameter(description = "Game mode to check", required = true)
            @RequestParam String gameMode,
            ServerWebExchange exchange
    ) {
        log.info("Checking if game mode '{}' is enabled for agent: {}", gameMode, agentId);

        return settingService.isGameModeEnabled(agentId, gameMode)
                .map(enabled -> ResponseEntity.ok(
                        ApiResponse.<Boolean>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Game mode check completed")
                                .data(enabled)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ));
    }
}
