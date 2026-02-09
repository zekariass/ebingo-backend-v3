package com.ebingo.backend.externalgame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaunchRequest {
    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotBlank(message = "Game mode is required")
    private String gameMode;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Telegram initData is required")
    private String initData;

    private String subId;
    private String lobbyUrl;
    private String brandName;
    private String lang;
    private Boolean adaptive;
    private Boolean isDemoPlay;
    private String userCountryCode; // ISO 3166-1 alpha-2 country code (e.g., TR, US)
}
