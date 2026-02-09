package com.ebingo.backend.agentgame.dto;

import com.ebingo.backend.agentgame.enums.GameCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for AgentGame response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentGameResponse {

    private Long id;
    private Long agentId;
    private GameCategory gameCategory;
    private String gameTypes;
    private Boolean isEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}
