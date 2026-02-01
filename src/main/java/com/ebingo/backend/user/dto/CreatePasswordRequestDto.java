package com.ebingo.backend.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePasswordRequestDto {

    @NotNull(message = "Telegram ID is required")
    private Long telegramId;

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotBlank(message = "Password is required")
    @Min(value = 4, message = "Password must be at least 4 characters long")
    private String password;
}
