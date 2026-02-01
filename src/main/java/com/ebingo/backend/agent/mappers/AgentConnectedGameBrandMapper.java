package com.ebingo.backend.agent.mappers;

import com.ebingo.backend.agent.dto.connectedbrands.AgentConnectedGameBrandDto;
import com.ebingo.backend.agent.entity.AgentConnectedGameBrand;

public final class AgentConnectedGameBrandMapper {
    public static AgentConnectedGameBrandDto toDto(AgentConnectedGameBrand agentConnectedGameBrand) {
        if (agentConnectedGameBrand == null) {
            return null;
        }
        return AgentConnectedGameBrandDto.builder()
                .id(agentConnectedGameBrand.getId())
                .agentId(agentConnectedGameBrand.getAgentId())
                .gameId(agentConnectedGameBrand.getGameId())
                .isActive(agentConnectedGameBrand.getIsActive())
                .createdAt(agentConnectedGameBrand.getCreatedAt())
                .updatedAt(agentConnectedGameBrand.getUpdatedAt())
                .build();
    }

    public static AgentConnectedGameBrand toEntity(AgentConnectedGameBrandDto dto) {
        if (dto == null) {
            return null;
        }
        AgentConnectedGameBrand entity = new AgentConnectedGameBrand();
        entity.setId(dto.getId());
        entity.setAgentId(dto.getAgentId());
        entity.setGameId(dto.getGameId());
        entity.setIsActive(dto.getIsActive());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
}
