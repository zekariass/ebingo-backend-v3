package com.ebingo.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileMinimalDto {
    private Long id;
    private Long telegramId;
    private Long agentId;
    private String firstName;
    private String lastName;
    private String nickname;
    private String phoneNumber;
    private Boolean isBot;
}
