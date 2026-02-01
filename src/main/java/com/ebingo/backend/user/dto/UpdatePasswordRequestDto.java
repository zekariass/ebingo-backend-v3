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
public class UpdatePasswordRequestDto {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "Telegram ID is required")
    private Long telegramId;

    @NotNull(message = "Old Password is required")
    private String oldPassword;

    @NotBlank(message = "Password is required")
    @Min(value = 4, message = "Password must be at least 4 characters long")
    private String newPassword;
}
