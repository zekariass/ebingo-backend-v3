package com.ebingo.backend.agent.dto.gamebrand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameBrandDto {

    private Long id;

    private String name;

    private String code;

    private Boolean isActive;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

