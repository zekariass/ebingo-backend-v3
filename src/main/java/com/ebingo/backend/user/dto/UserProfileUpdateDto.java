package com.ebingo.backend.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateDto {
    @NotNull(message = "id is required")
    private Long id;
    private Long telegramId;
    private Long agentId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
