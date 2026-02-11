package com.ebingo.backend.externalgame.mapper;

import com.ebingo.backend.externalgame.dto.AgentGameSettingDto;
import com.ebingo.backend.externalgame.entity.AgentGameSetting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for AgentGameSetting entity and DTO.
 */
public class AgentGameSettingMapper {

    private AgentGameSettingMapper() {
        // Utility class
    }

    /**
     * Convert entity to DTO.
     */
    public static AgentGameSettingDto toDto(AgentGameSetting entity) {
        if (entity == null) {
            return null;
        }

        return AgentGameSettingDto.builder()
                .id(entity.getId())
                .agentId(entity.getAgentId())
                .gameModes(parseGameModes(entity.getGameModes()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert DTO to entity.
     */
    public static AgentGameSetting toEntity(AgentGameSettingDto dto) {
        if (dto == null) {
            return null;
        }

        AgentGameSetting entity = new AgentGameSetting();
        entity.setId(dto.getId());
        entity.setAgentId(dto.getAgentId());
        entity.setGameModes(formatGameModes(dto.getGameModes()));
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());

        return entity;
    }

    /**
     * Parse comma-separated game modes string to list.
     */
    private static List<String> parseGameModes(String gameModes) {
        if (gameModes == null || gameModes.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(gameModes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Format list of game modes to comma-separated string.
     */
    private static String formatGameModes(List<String> gameModes) {
        if (gameModes == null || gameModes.isEmpty()) {
            return "";
        }

        return gameModes.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
