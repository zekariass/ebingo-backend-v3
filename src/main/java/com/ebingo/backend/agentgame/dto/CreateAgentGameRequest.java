package com.ebingo.backend.agentgame.dto;

import com.ebingo.backend.agentgame.enums.GameCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new AgentGame association
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAgentGameRequest {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Game category is required")
    private GameCategory gameCategory;

    private String gameTypes;

    @Builder.Default
    private Boolean isEnabled = true;
}
