package com.ebingo.backend.agent.service;

import com.ebingo.backend.agent.dto.agent.AgentCreateDto;
import com.ebingo.backend.agent.dto.agent.AgentDto;
import com.ebingo.backend.agent.dto.agent.AgentUpdateDto;
import com.ebingo.backend.agent.entity.Agent;
import com.ebingo.backend.agent.mappers.AgentMapper;
import com.ebingo.backend.agent.repository.AgentRepository;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final AgentRepository repository;
    private final AgentConfigService agentConfigService;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<PageResponse<AgentDto>> getAllAgents(int page, int size, String sortBy) {
        long offset = (long) page * size;
        
        return repository.countAll()
                .flatMap(totalElements -> {
                    var dataFlux = switch (sortBy != null ? sortBy.toLowerCase() : "id") {
                        case "name" -> repository.findAllPagedSortedByName(size, offset);
                        case "createdat" -> repository.findAllPagedSortedByCreatedAt(size, offset);
                        default -> repository.findAllPaged(size, offset);
                    };
                    
                    return dataFlux
                            .map(AgentMapper::toDto)
                            .collectList()
                            .map(content -> new PageResponse<>(content, page, size, totalElements));
                })
                .doOnSubscribe(s -> log.info("Fetching all agents, page: {}, size: {}, sortBy: {}", page, size, sortBy))
                .doOnSuccess(response -> log.info("Fetched {} agents", response.getContent().size()))
                .doOnError(e -> log.error("Failed to fetch agents", e));
    }

    @Override
    public Mono<AgentDto> createAgent(AgentCreateDto dto) {
        LocalDateTime now = LocalDateTime.now();
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setCode(dto.getCode());
        agent.setPhoneNumber(dto.getPhoneNumber());
        agent.setEmail(dto.getEmail());
        agent.setContactName(dto.getContactName());
        agent.setIsMaster(dto.getIsMaster() != null ? dto.getIsMaster() : false);
        agent.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false);
        agent.setCommissionRate(dto.getCommissionRate());
        agent.setBotToken(dto.getBotToken());
        agent.setBotUsername(dto.getBotUsername());
        agent.setContactAddress(dto.getContactAddress());
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);

        return repository.save(agent)
                .flatMap(saved -> agentConfigService.createEmptyConfig(saved.getId())
                        .thenReturn(saved))
                .map(AgentMapper::toDto)
                .as(transactionalOperator::transactional)
                .doOnSubscribe(s -> log.info("Creating agent: {}", dto.getName()))
                .doOnSuccess(saved -> log.info("Created agent with ID: {}", saved.getId()))
                .doOnError(e -> log.error("Failed to create agent: {}", dto.getName(), e));
    }

    @Override
    public Mono<AgentDto> getAgentById(Long agentId) {
        return repository.findById(agentId)
                .map(AgentMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching agent with ID: {}", agentId))
                .doOnSuccess(agent -> log.info("Fetched agent with ID: {}", agentId))
                .doOnError(e -> log.error("Failed to fetch agent with ID: {}", agentId, e));
    }

    @Override
    public Flux<AgentDto> getAllActiveAgents() {
        return repository.findByIsActiveTrue()
                .map(AgentMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching all active agents"))
                .doOnComplete(() -> log.info("Fetched all active agents"))
                .doOnError(e -> log.error("Failed to fetch active agents", e));
    }

    @Override
    public Mono<AgentDto> updateAgentById(Long agentId, AgentUpdateDto agentUpdateDto) {
        return repository.findById(agentId)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Agent not found with ID: " + agentId)))
                .flatMap(existingAgent -> {
                    AgentMapper.toEntity(agentUpdateDto, existingAgent);
                    existingAgent.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existingAgent);
                })
                .map(AgentMapper::toDto)
                .doOnSubscribe(s -> log.info("Updating agent with ID: {}", agentId))
                .doOnSuccess(agent -> log.info("Updated agent with ID: {}", agentId))
                .doOnError(e -> log.error("Failed to update agent with ID: {}", agentId, e));
    }

    @Override
    public Flux<AgentDto> searchAgents(String searchTerm) {
        return repository.searchAgents(searchTerm)
                .map(AgentMapper::toDto)
                .doOnSubscribe(s -> log.info("Searching agents with term: {}", searchTerm))
                .doOnComplete(() -> log.info("Completed search for agents with term: {}", searchTerm))
                .doOnError(e -> log.error("Failed to search agents with term: {}", searchTerm, e));
    }

}
