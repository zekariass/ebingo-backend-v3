package com.ebingo.backend.agent.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Admin request body for POST /api/v1/admin/agents.
 * Creates the agent and an empty agent_config row in the same transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCreateDto {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;

    @NotBlank(message = "email is required")
    private String email;

    private String contactName;
    private Boolean isMaster;
    private Boolean isActive;
    private BigDecimal commissionRate;
    private String botToken;
    private String botUsername;
    private String contactAddress;

    /** Optional UI theme palette key; null/absent = client "default" palette. */
    @Size(max = 64, message = "themeKey must be at most 64 characters")
    private String themeKey;
}
