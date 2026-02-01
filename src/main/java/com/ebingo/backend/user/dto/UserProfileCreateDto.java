package com.ebingo.backend.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserProfileCreateDto {
    @NotNull(message = "telegramId is required")
    private Long telegramId;

    private Long referrerId;

    @NotNull(message = "firstName is required")
    private String firstName;

    private String lastName;

    @NotNull(message = "phoneNumber is required")
    private String phoneNumber;

    @NotNull(message = "status is required")
    private Long agentId;

}
