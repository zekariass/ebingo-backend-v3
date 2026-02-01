package com.ebingo.backend.agent.service;

import com.ebingo.backend.agent.dto.agent.AgentDto;
import com.ebingo.backend.agent.dto.agent.AgentUpdateDto;
import com.ebingo.backend.common.dto.PageResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AgentService {
    Mono<PageResponse<AgentDto>> getAllAgents(int page, int size, String sortBy);

    Mono<AgentDto> getAgentById(Long agentId);

    Flux<AgentDto> getAllActiveAgents();

    Mono<AgentDto> updateAgentById(Long agentId, AgentUpdateDto agentUpdateDto);

    Flux<AgentDto> searchAgents(String searchTerm);
}
