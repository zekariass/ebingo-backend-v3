package com.ebingo.backend.externalgame.service;

import com.ebingo.backend.externalgame.dto.AgentGameSettingDto;
import com.ebingo.backend.externalgame.dto.UpdateAgentGameModesRequest;
import com.ebingo.backend.externalgame.entity.AgentGameSetting;
import com.ebingo.backend.externalgame.mapper.AgentGameSettingMapper;
import com.ebingo.backend.externalgame.repository.AgentGameSettingRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of AgentGameSettingService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentGameSettingServiceImpl implements AgentGameSettingService {

    private final AgentGameSettingRepository repository;

    @Override
    public Mono<AgentGameSettingDto> getAgentGameModes(Long agentId) {
        log.info("Fetching game modes for agent: {}", agentId);

        return repository.findByAgentId(agentId)
                .map(AgentGameSettingMapper::toDto)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No game modes configured for agent: {}, returning empty list", agentId);
                    return Mono.just(AgentGameSettingDto.builder()
                            .agentId(agentId)
                            .gameModes(Collections.emptyList())
                            .build());
                }))
                .doOnSuccess(dto -> log.info("Retrieved game modes for agent {}: {}", agentId, dto.getGameModes()));
    }

    @Override
    public Mono<AgentGameSettingDto> updateAgentGameModes(UpdateAgentGameModesRequest request) {
        log.info("Updating game modes for agent {}: {}", request.getAgentId(), request.getGameModes());

        // Validate and normalize game modes (preserve original case)
        List<String> normalizedGameModes = request.getGameModes().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (normalizedGameModes.isEmpty()) {
            return Mono.error(new IllegalArgumentException("At least one valid game mode must be specified"));
        }

        return repository.findByAgentId(request.getAgentId())
                .flatMap(existing -> {
                    // Update existing settings
                    existing.setGameModes(String.join(",", normalizedGameModes));
                    existing.setUpdatedAt(LocalDateTime.now());
                    return repository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create new settings
                    AgentGameSetting newSetting = new AgentGameSetting();
                    newSetting.setAgentId(request.getAgentId());
                    newSetting.setGameModes(String.join(",", normalizedGameModes));
                    newSetting.setCreatedAt(LocalDateTime.now());
                    newSetting.setUpdatedAt(LocalDateTime.now());
                    return repository.save(newSetting);
                }))
                .map(AgentGameSettingMapper::toDto)
                .doOnSuccess(dto -> log.info("Successfully updated game modes for agent {}: {}", 
                        request.getAgentId(), dto.getGameModes()))
                .doOnError(e -> log.error("Failed to update game modes for agent {}: {}", 
                        request.getAgentId(), e.getMessage()));
    }

    @Override
    public Mono<Boolean> isGameModeEnabled(Long agentId, String gameMode) {
        if (gameMode == null || gameMode.trim().isEmpty()) {
            return Mono.just(false);
        }

        String normalizedGameMode = gameMode.trim();

        return repository.findByAgentId(agentId)
                .map(setting -> {
                    if (setting.getGameModes() == null || setting.getGameModes().isEmpty()) {
                        return false;
                    }
                    List<String> enabledModes = AgentGameSettingMapper.toDto(setting).getGameModes();
                    return enabledModes.stream()
                            .anyMatch(mode -> mode.equalsIgnoreCase(normalizedGameMode));
                })
                .defaultIfEmpty(false)
                .doOnSuccess(enabled -> log.debug("Game mode '{}' enabled for agent {}: {}", 
                        normalizedGameMode, agentId, enabled));
    }

    @Override
    public Mono<List<String>> getEnabledGameModes(Long agentId) {
        return repository.findByAgentId(agentId)
                .map(AgentGameSettingMapper::toDto)
                .map(AgentGameSettingDto::getGameModes)
                .defaultIfEmpty(Collections.emptyList());
    }
}
