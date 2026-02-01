package com.ebingo.backend.agent.dto.agent;

import jakarta.validation.constraints.Email;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentUpdateDto {
    private String name;
    private String phoneNumber;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private String contactName;
    private Boolean isActive;
    private BigDecimal commissionRate;
    private String botToken;
    private String botUsername;
    private String contactAddress;
}
