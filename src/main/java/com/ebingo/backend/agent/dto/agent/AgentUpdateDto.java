package com.ebingo.backend.agent.dto.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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

    /**
     * UI theme palette key. Presence-tracked so partial updates work:
     * absent field = leave unchanged, explicit null = reset to the client
     * "default" palette. Unknown keys are stored as-is (open set).
     */
    @Size(max = 64, message = "themeKey must be at most 64 characters")
    @Setter(AccessLevel.NONE)
    private String themeKey;

    /** Internal flag: true when themeKey was present in the request payload. */
    @JsonIgnore
    private boolean themeKeyPresent;

    public void setThemeKey(String themeKey) {
        this.themeKey = themeKey;
        this.themeKeyPresent = true;
    }
}
