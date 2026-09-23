package com.ebingo.backend.agent.service;

import com.ebingo.backend.agent.dto.agent.AgentConfigDto;
import com.ebingo.backend.agent.dto.agent.AgentConfigUpdateDto;
import com.ebingo.backend.agent.entity.AgentConfig;
import com.ebingo.backend.agent.repository.AgentConfigRepository;
import com.ebingo.backend.agent.repository.AgentRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentConfigServiceImpl implements AgentConfigService {

    private final AgentConfigRepository agentConfigRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<AgentConfigDto> getConfig(Long agentId) {
        return agentRepository.existsById(agentId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Agent not found with ID: " + agentId));
                    }
                    return agentConfigRepository.findById(agentId)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                    "Agent config not found for agent ID: " + agentId)))
                            .map(this::toDto);
                })
                .doOnSubscribe(s -> log.debug("Fetching bot config for agent ID: {}", agentId))
                .doOnError(e -> log.warn("Failed to fetch bot config for agent ID {}: {}", agentId, e.getMessage()));
    }

    @Override
    public Mono<AgentConfigDto> upsertConfig(Long agentId, AgentConfigUpdateDto dto) {
        return agentRepository.existsById(agentId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Agent not found with ID: " + agentId));
                    }
                    return agentConfigRepository.upsert(
                                    agentId,
                                    dto.getName(),
                                    dto.getAdminIds(),
                                    dto.getLogoName(),
                                    dto.getSupportContact(),
                                    dto.getSupportUsername(),
                                    dto.getSupportChannel(),
                                    toJsonString(dto.getBankDetails()))
                            .then(agentConfigRepository.findById(agentId))
                            .map(this::toDto);
                })
                .doOnSubscribe(s -> log.info("Upserting bot config for agent ID: {}", agentId))
                .doOnSuccess(cfg -> log.info("Upserted bot config for agent ID: {}", agentId))
                .doOnError(e -> log.error("Failed to upsert bot config for agent ID: {}", agentId, e));
    }

    @Override
    public Mono<Void> createEmptyConfig(Long agentId) {
        return agentConfigRepository.upsert(agentId, null, null, null, null, null, null, null)
                .doOnSubscribe(s -> log.info("Creating empty bot config for agent ID: {}", agentId))
                .doOnSuccess(v -> log.info("Created empty bot config for agent ID: {}", agentId))
                .doOnError(e -> log.error("Failed to create empty bot config for agent ID: {}", agentId, e));
    }

    private AgentConfigDto toDto(AgentConfig entity) {
        return AgentConfigDto.builder()
                .agentId(entity.getAgentId())
                .name(entity.getBrandName())
                .adminIds(entity.getAdminIds())
                .logoName(entity.getLogoName())
                .supportContact(entity.getSupportContact())
                .supportUsername(entity.getSupportUsername())
                .supportChannel(entity.getSupportChannel())
                .bankDetails(toMap(entity.getBankDetails()))
                .build();
    }

    private Map<String, Object> toMap(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse agent_config.bank_details JSON", e);
        }
    }

    private String toJsonString(Map<String, Object> bankDetails) {
        if (bankDetails == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(bankDetails);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("bankDetails must be a valid JSON object", e);
        }
    }
}
