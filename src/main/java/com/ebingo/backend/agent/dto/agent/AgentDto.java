package com.ebingo.backend.agent.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDto {
    private Long id;
    private String name;
    private String code;
    private String phoneNumber;
    private String email;
    private String contactName;
    private Boolean isMaster;
    private Boolean isActive;
    private BigDecimal commissionRate;
    private String botToken;
    private String botUsername;
    private String contactAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
