package com.ebingo.backend.user.dto;

import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserProfileDto {
    private Long id;
    private Long telegramId;
    private Long referrerId;
    private Long agentId;
    private String firstName;
    private String lastName;
    private String nickname;
    private String phoneNumber;
    private UserStatus status;
    private UserRole role;
    private Boolean isBot;
    private Long botRoomId;
    private Boolean hasPassword;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
