package com.ebingo.backend.agent.service;

import com.ebingo.backend.agent.dto.agent.AgentConfigDto;
import com.ebingo.backend.agent.dto.agent.AgentConfigUpdateDto;
import reactor.core.publisher.Mono;

public interface AgentConfigService {

    /**
     * Fetch the bot config for an agent.
     * Errors with ResourceNotFoundException when the agent or its config does not exist.
     */
    Mono<AgentConfigDto> getConfig(Long agentId);

    /**
     * Upsert the config for an agent (creates the row if missing).
     * Errors with ResourceNotFoundException when the agent does not exist.
     */
    Mono<AgentConfigDto> upsertConfig(Long agentId, AgentConfigUpdateDto dto);

    /**
     * Insert an empty config row for a newly created agent (no-op if one exists).
     */
    Mono<Void> createEmptyConfig(Long agentId);
}
