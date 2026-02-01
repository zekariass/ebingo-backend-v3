package com.ebingo.backend.agent.mappers;

import com.ebingo.backend.agent.dto.gamebrand.GameBrandDto;
import com.ebingo.backend.agent.entity.GameBrand;

public final class GameBrandMapper {
    public static String toDto(GameBrand gameBrand) {
        if (gameBrand == null) {
            return null;
        }
        return GameBrandDto.builder()
                .id(gameBrand.getId())
                .name(gameBrand.getName())
                .code(gameBrand.getCode())
                .isActive(gameBrand.getIsActive())
                .description(gameBrand.getDescription())
                .createdAt(gameBrand.getCreatedAt())
                .updatedAt(gameBrand.getUpdatedAt())
                .build().getName();
    }


    public static GameBrand toEntity(GameBrandDto dto) {
        if (dto == null) {
            return null;
        }
        GameBrand entity = new GameBrand();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setIsActive(dto.getIsActive());
        entity.setDescription(dto.getDescription());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
}
