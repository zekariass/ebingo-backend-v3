package com.ebingo.backend.user.mappers;


import com.ebingo.backend.common.Util;
import com.ebingo.backend.user.dto.*;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.enums.UserStatus;

import java.util.List;

public final class UserProfileMapper {

    public static UserProfileDto toDto(UserProfile userProfile) {
        if (userProfile == null) return null;
        return UserProfileDto.builder()
                .id(userProfile.getId())
                .telegramId(userProfile.getTelegramId())
                .referrerId(userProfile.getReferrerId())
                .agentId(userProfile.getAgentId())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .nickname(userProfile.getNickname())
                .phoneNumber(userProfile.getPhoneNumber())
                .status(userProfile.getStatus())
                .role(userProfile.getRole())
                .isBot(userProfile.getIsBot())
                .botRoomId(userProfile.getBotRoomId())
                .password(userProfile.getPassword())
                .hasPassword(userProfile.getPassword() != null && !userProfile.getPassword().isEmpty())
                .createdAt(userProfile.getCreatedAt())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }

    public static UserProfile toEntity(UserProfileCreateDto userProfileDto) {
        if (userProfileDto == null) return null;
        UserProfile userProfile = new UserProfile();
        userProfile.setTelegramId(userProfileDto.getTelegramId());
        userProfile.setAgentId(userProfileDto.getAgentId());
        userProfile.setReferrerId(userProfileDto.getReferrerId());
        userProfile.setFirstName(userProfileDto.getFirstName());
        userProfile.setLastName(userProfileDto.getLastName());
        userProfile.setPhoneNumber(Util.normalizePhoneNumber(userProfileDto.getPhoneNumber()));
        userProfile.setStatus(UserStatus.ACTIVE); // Default status
        userProfile.setRole(UserRole.PLAYER);
        userProfile.setIsBot(false);
        userProfile.setBotRoomId(null);

        String firstName = userProfileDto.getFirstName() != null ? userProfileDto.getFirstName() : "";
        String lastName = userProfileDto.getLastName() != null ? userProfileDto.getLastName() : "";

        String nickName = (firstName + " " + lastName).trim();
        userProfile.setNickname(nickName);

        return userProfile;
    }

    public static UserProfileMinimalDto toMinimalDto(UserProfile userProfile) {
        if (userProfile == null) return null;
        return UserProfileMinimalDto.builder()
                .id(userProfile.getId())
                .agentId(userProfile.getAgentId())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .nickname(userProfile.getNickname())
                .phoneNumber(userProfile.getPhoneNumber())
                .isBot(userProfile.getIsBot())
                .telegramId(userProfile.getTelegramId())
                .build();
    }

    public static UserProfile toEntity(UserProfileDto userProfileDto) {
        if (userProfileDto == null) return null;
        UserProfile userProfile = new UserProfile();
        userProfile.setId(userProfileDto.getId());
        userProfile.setAgentId(userProfileDto.getAgentId());
        userProfile.setTelegramId(userProfileDto.getTelegramId());
        userProfile.setReferrerId(userProfileDto.getReferrerId());
        userProfile.setFirstName(userProfileDto.getFirstName());
        userProfile.setLastName(userProfileDto.getLastName());
        userProfile.setIsBot(userProfileDto.getIsBot());
        userProfile.setBotRoomId(userProfileDto.getBotRoomId());
        userProfile.setPhoneNumber(Util.normalizePhoneNumber(userProfileDto.getPhoneNumber()));
        userProfile.setStatus(UserStatus.ACTIVE); // Default status
        userProfile.setRole(userProfileDto.getRole() != null ? userProfileDto.getRole() : UserRole.PLAYER);
        return userProfile;
    }

    public static UserProfile toEntity(UserProfileUpdateDto userProfileDto, UserProfile existingUserProfile) {

        if (userProfileDto == null) return null;
        if (existingUserProfile == null) return null;
        if (userProfileDto.getFirstName() != null) {
            existingUserProfile.setFirstName(userProfileDto.getFirstName());
        }
        if (userProfileDto.getLastName() != null) {
            existingUserProfile.setLastName(userProfileDto.getLastName());
        }
        return existingUserProfile;
    }


    public static UserIdsResponseDto toUserIdsDto(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return null;
        return UserIdsResponseDto.builder()
                .ids(userIds)
                .build();
    }


}
