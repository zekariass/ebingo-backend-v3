package com.ebingo.backend.user.service;


import com.ebingo.backend.user.dto.*;
import reactor.core.publisher.Mono;

public interface UserProfileService {

    Mono<UserProfileDto> getUserProfileByPhoneNumberAndAgentId(String PhoneNumber, Long agentId);

    Mono<UserProfileDto> createUserProfile(UserProfileCreateDto userProfileDto);

    Mono<UserProfileDto> getUserProfileById(Long receiverId);

    Mono<UserProfileDto> getUserByPhoneNumberAndAgentId(String phoneNumber, Long agentId);

    Mono<UserProfileDto> getUserProfileByTelegramIdAndAgentId(Long telegramId, Long agentId);

    Mono<UserProfileDto> changeNameOfAgentUser(Long telegramId, String name, Long agentId);

    Mono<UserIdsResponseDto> getUserTelegramIdsOfAgent(Long agentId);

    Mono<UserProfileDto> createPassword(CreatePasswordRequestDto createPasswordRequest);

    Mono<UserProfileDto> updatePassword(UpdatePasswordRequestDto updatePasswordRequestDto);

    Mono<UserProfileMinimalDto> getUserProfileMinimal(Long userId);
}
