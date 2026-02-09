package com.ebingo.backend.agentgame.controller;

import com.ebingo.backend.agentgame.dto.AgentGameResponse;
import com.ebingo.backend.agentgame.dto.CreateAgentGameRequest;
import com.ebingo.backend.agentgame.service.AgentGameService;
import com.ebingo.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller for managing agent-game associations
 * Provides endpoints for CRUD operations on agent games
 */
@RestController
@RequestMapping("/api/agent-games")
@RequiredArgsConstructor
@Slf4j
public class AgentGameController {

    private final AgentGameService agentGameService;

    /**
     * Get all games for a specific agent
     *
     * @param agentId the agent ID
     * @param enabledOnly optional filter for enabled games only
     * @return mono of response entity with list of agent game responses
     */
    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<AgentGameResponse>>>> getAgentGames(
            @RequestParam Long agentId,
            @RequestParam(required = false, defaultValue = "false") Boolean enabledOnly) {
        log.info("GET /api/agent-games - agentId: {}, enabledOnly: {}", agentId, enabledOnly);
        
        Flux<AgentGameResponse> gamesFlux = Boolean.TRUE.equals(enabledOnly)
                ? agentGameService.getEnabledAgentGames(agentId)
                : agentGameService.getAgentGames(agentId);
        
        return gamesFlux.collectList()
                .map(games -> ResponseEntity.ok(
                        ApiResponse.<List<AgentGameResponse>>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Agent games retrieved successfully")
                                .data(games)
                                .build()
                ));
    }

    /**
     * Get a specific agent game by ID
     *
     * @param id the agent game ID
     * @return mono of response entity with agent game response
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<AgentGameResponse>>> getAgentGameById(@PathVariable Long id) {
        log.info("GET /api/agent-games/{}", id);
        
        return agentGameService.getAgentGameById(id)
                .map(game -> ResponseEntity.ok(
                        ApiResponse.<AgentGameResponse>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Agent game retrieved successfully")
                                .data(game)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<AgentGameResponse>builder()
                                .success(false)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("Agent game not found")
                                .error("NOT_FOUND")
                                .build()
                        ));
    }

    /**
     * Create a new agent-game association
     *
     * @param request the create request
     * @return mono of created agent game response
     */
    @PostMapping
    public Mono<ResponseEntity<Object>> createAgentGame(@Valid @RequestBody CreateAgentGameRequest request) {
        log.info("POST /api/agent-games - request: {}", request);
        
        return agentGameService.createAgentGame(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body((Object) response))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Bad request: {}", e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body((Object) e.getMessage()));
                })
                .onErrorResume(e -> {
                    log.error("Error creating agent game", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) "Internal server error"));
                });
    }

    /**
     * Update agent game enabled status
     *
     * @param id the agent game ID
     * @param isEnabled the new enabled status
     * @return mono of updated agent game response
     */
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<Object>> updateAgentGameStatus(
            @PathVariable Long id,
            @RequestParam Boolean isEnabled) {
        log.info("PATCH /api/agent-games/{}/status - isEnabled: {}", id, isEnabled);
        
        return agentGameService.updateAgentGameStatus(id, isEnabled)
                .map(response -> ResponseEntity.ok((Object) response))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Not found: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(e -> {
                    log.error("Error updating agent game status", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) "Internal server error"));
                });
    }

    /**
     * Delete an agent-game association
     *
     * @param id the agent game ID
     * @return mono of response entity
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAgentGame(@PathVariable Long id) {
        log.info("DELETE /api/agent-games/{}", id);
        
        return agentGameService.deleteAgentGame(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Not found: {}", e.getMessage());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(e -> {
                    log.error("Error deleting agent game", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
