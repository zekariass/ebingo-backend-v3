package com.ebingo.backend.externalgame.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating agent game modes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAgentGameModesRequest {

    @NotNull(message = "Agent ID is required")
    @JsonProperty("agentId")
    private Long agentId;

    @NotEmpty(message = "At least one game mode must be specified")
    @JsonProperty("gameModes")
    private List<String> gameModes;
}
