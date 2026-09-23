package com.ebingo.backend.agent.service;

import com.ebingo.backend.agent.dto.agent.AgentDepositConfigDto;
import com.ebingo.backend.agent.entity.AgentDepositConfig;
import reactor.core.publisher.Mono;

public interface AgentDepositConfigService {

    /**
     * Fetch the effective deposit config for an agent: the stored row when present,
     * otherwise the global `deposit.*` defaults from application.yml.
     * Errors with ResourceNotFoundException when the agent does not exist.
     */
    Mono<AgentDepositConfigDto> getConfig(Long agentId);

    /**
     * Upsert the deposit config for an agent (creates the row if missing).
     * Errors with ResourceNotFoundException when the agent does not exist.
     */
    Mono<AgentDepositConfigDto> upsertConfig(Long agentId, AgentDepositConfigDto dto);

    /**
     * Delete the deposit config row for an agent, reverting it to the global defaults.
     * Errors with ResourceNotFoundException when the agent does not exist.
     */
    Mono<Void> deleteConfig(Long agentId);

    /**
     * Internal use: effective config for wallet crediting.
     * Returns the stored row, or the global defaults when no row exists or agentId is null.
     * Does NOT validate agent existence — used on the hot deposit path.
     */
    Mono<AgentDepositConfig> getEffectiveConfig(Long agentId);
}
