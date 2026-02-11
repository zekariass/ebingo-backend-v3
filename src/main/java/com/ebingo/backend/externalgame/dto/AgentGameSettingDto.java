package com.ebingo.backend.externalgame.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for agent game settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentGameSettingDto {

    private Long id;

    @JsonProperty("agentId")
    private Long agentId;

    /**
     * List of enabled game modes.
     */
    @JsonProperty("gameModes")
    private List<String> gameModes;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
