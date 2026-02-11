package com.ebingo.backend.externalgame.service;

import com.ebingo.backend.externalgame.dto.AgentGameSettingDto;
import com.ebingo.backend.externalgame.dto.UpdateAgentGameModesRequest;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service interface for managing agent game settings.
 */
public interface AgentGameSettingService {

    /**
     * Get agent game modes by agent ID.
     * Returns empty list if no settings found.
     */
    Mono<AgentGameSettingDto> getAgentGameModes(Long agentId);

    /**
     * Update agent game modes.
     * Creates new settings if none exist, otherwise updates existing.
     */
    Mono<AgentGameSettingDto> updateAgentGameModes(UpdateAgentGameModesRequest request);

    /**
     * Check if a specific game mode is enabled for an agent.
     */
    Mono<Boolean> isGameModeEnabled(Long agentId, String gameMode);

    /**
     * Get list of enabled game modes for an agent.
     */
    Mono<List<String>> getEnabledGameModes(Long agentId);
}
