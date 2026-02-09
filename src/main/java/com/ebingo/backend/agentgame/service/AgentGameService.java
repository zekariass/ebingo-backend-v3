package com.ebingo.backend.agentgame.service;

import com.ebingo.backend.agentgame.dto.AgentGameResponse;
import com.ebingo.backend.agentgame.dto.CreateAgentGameRequest;
import com.ebingo.backend.agentgame.entity.AgentGame;
import com.ebingo.backend.agentgame.enums.GameCategory;
import com.ebingo.backend.agentgame.repository.AgentGameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Service for managing agent-game associations
 * Handles business logic for agent game configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentGameService {

    private final AgentGameRepository agentGameRepository;

    /**
     * Get all games for a specific agent
     *
     * @param agentId the agent ID
     * @return flux of agent game responses
     */
    public Flux<AgentGameResponse> getAgentGames(Long agentId) {
        log.info("Fetching all games for agent: {}", agentId);
        return agentGameRepository.findByAgentId(agentId)
                .map(this::toResponse)
                .doOnComplete(() -> log.info("Successfully fetched games for agent: {}", agentId));
    }

    /**
     * Get all enabled games for a specific agent
     *
     * @param agentId the agent ID
     * @return flux of agent game responses
     */
    public Flux<AgentGameResponse> getEnabledAgentGames(Long agentId) {
        log.info("Fetching enabled games for agent: {}", agentId);
        return agentGameRepository.findByAgentIdAndIsEnabled(agentId, true)
                .map(this::toResponse)
                .doOnComplete(() -> log.info("Successfully fetched enabled games for agent: {}", agentId));
    }

    /**
     * Get a specific agent game by ID
     *
     * @param id the agent game ID
     * @return mono of agent game response
     */
    public Mono<AgentGameResponse> getAgentGameById(Long id) {
        log.info("Fetching agent game by ID: {}", id);
        return agentGameRepository.findById(id)
                .map(this::toResponse)
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.info("Successfully fetched agent game: {}", id);
                    } else {
                        log.warn("Agent game not found: {}", id);
                    }
                });
    }

    /**
     * Create a new agent-game association
     *
     * @param request the create request
     * @return mono of created agent game response
     */
    public Mono<AgentGameResponse> createAgentGame(CreateAgentGameRequest request) {
        log.info("Creating agent game: agentId={}, gameCategory={}", request.getAgentId(), request.getGameCategory());
        
        // Check if association already exists
        return agentGameRepository.findByAgentIdAndGameCategory(request.getAgentId(), request.getGameCategory())
                .flatMap(existing -> {
                    log.warn("Agent game association already exists: agentId={}, gameCategory={}", 
                            request.getAgentId(), request.getGameCategory());
                    return Mono.error(new IllegalArgumentException(
                            "Agent game association already exists for this agent and game category"));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Instant now = Instant.now();
                    AgentGame agentGame = AgentGame.builder()
                            .agentId(request.getAgentId())
                            .gameCategory(request.getGameCategory())
                            .gameTypes(request.getGameTypes())
                            .isEnabled(request.getIsEnabled())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    
                    return agentGameRepository.save(agentGame)
                            .map(this::toResponse)
                            .doOnSuccess(response -> log.info("Successfully created agent game: {}", response.getId()));
                }))
                .cast(AgentGameResponse.class);
    }

    /**
     * Update agent game enabled status
     *
     * @param id the agent game ID
     * @param isEnabled the new enabled status
     * @return mono of updated agent game response
     */
    public Mono<AgentGameResponse> updateAgentGameStatus(Long id, Boolean isEnabled) {
        log.info("Updating agent game status: id={}, isEnabled={}", id, isEnabled);
        
        return agentGameRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agent game not found with ID: " + id)))
                .flatMap(agentGame -> {
                    agentGame.setIsEnabled(isEnabled);
                    agentGame.setUpdatedAt(Instant.now());
                    return agentGameRepository.save(agentGame);
                })
                .map(this::toResponse)
                .doOnSuccess(response -> log.info("Successfully updated agent game status: {}", id));
    }

    /**
     * Delete an agent-game association
     *
     * @param id the agent game ID
     * @return mono of void
     */
    public Mono<Void> deleteAgentGame(Long id) {
        log.info("Deleting agent game: {}", id);
        return agentGameRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agent game not found with ID: " + id)))
                .flatMap(agentGame -> agentGameRepository.deleteById(id))
                .doOnSuccess(v -> log.info("Successfully deleted agent game: {}", id));
    }

    /**
     * Check if an agent has a specific game category enabled
     *
     * @param agentId the agent ID
     * @param gameCategory the game category
     * @return mono of boolean
     */
    public Mono<Boolean> isGameCategoryEnabledForAgent(Long agentId, GameCategory gameCategory) {
        log.debug("Checking if game category is enabled: agentId={}, gameCategory={}", agentId, gameCategory);
        return agentGameRepository.existsByAgentIdAndGameCategoryAndIsEnabled(agentId, gameCategory, true);
    }

    /**
     * Convert entity to response DTO
     */
    private AgentGameResponse toResponse(AgentGame agentGame) {
        return AgentGameResponse.builder()
                .id(agentGame.getId())
                .agentId(agentGame.getAgentId())
                .gameCategory(agentGame.getGameCategory())
                .gameTypes(agentGame.getGameTypes())
                .isEnabled(agentGame.getIsEnabled())
                .createdAt(agentGame.getCreatedAt())
                .updatedAt(agentGame.getUpdatedAt())
                .build();
    }
}
