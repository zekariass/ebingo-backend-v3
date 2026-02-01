package com.ebingo.backend.user.service;

import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.system.redis.CacheKeyUtil;
import com.ebingo.backend.system.service.CacheService;
import com.ebingo.backend.user.dto.*;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.mappers.UserProfileMapper;
import com.ebingo.backend.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ReactiveTransactionManager transactionManager;
    private final CacheService cacheService;
    private final PasswordEncoder passwordEncoder;


//    @Override
//    public Mono<UserProfileDto> getUserProfileByPhoneNumber(String phoneNumber) {
//        log.info("Getting user profile by phone number");
//
//
//        return userProfileRepository.findByPhoneNumber(phoneNumber)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
//                .map(UserProfileMapper::toDto);
//    }


    @Override
    public Mono<UserProfileDto> getUserProfileByPhoneNumberAndAgentId(String phoneNumber, Long agentId) {
        log.info("Getting user profile by phone number");

        String userProfileKey = CacheKeyUtil.getUserProfileByPhoneAndAgentIdKey(phoneNumber, agentId);

        Mono<UserProfileDto> userProfile = userProfileRepository.findByPhoneNumberAndAgentId(phoneNumber, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .map(UserProfileMapper::toDto);

        return cacheService.cacheMono(
                userProfileKey,
                userProfile,
                UserProfileDto.class
        );
    }


    @Override
    public Mono<UserProfileDto> createUserProfile(UserProfileCreateDto userProfileDto) {
        log.info("Creating user profile: {}", userProfileDto);

        UserProfile userProfile = UserProfileMapper.toEntity(userProfileDto);

        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        return userProfileRepository.save(userProfile)
                .map(UserProfileMapper::toDto)
                .doOnSuccess(saved -> log.info("User profile created successfully: {}", saved))
                .doOnError(e -> log.error("Error creating user profile: {}", userProfileDto, e))
                .as(operator::transactional);
    }

//    @Override
//    public Mono<UserProfileDto> getUserProfileById(Long receiverId) {
//        return userProfileRepository.findById(receiverId)
//                .map(UserProfileMapper::toDto)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
//                .doOnSubscribe(s -> log.info("Fetching user profile: {}", receiverId))
//                .doOnSuccess(dto -> log.info("Completed fetching user profile: {}", receiverId))
//                .doOnError(e -> log.error("Failed to fetch user profile: {}", e.getMessage(), e));
//    }

    @Override
    public Mono<UserProfileDto> getUserProfileById(Long receiverId) {

        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>: Fetching user profile: {}", receiverId);

        String userProfileKey = CacheKeyUtil.getUserProfileByIdKey(receiverId);

        Mono<UserProfileDto> userProfile = userProfileRepository.findById(receiverId)
                .map(UserProfileMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .doOnSubscribe(s -> log.info("Fetching user profile: {}", receiverId))
                .doOnSuccess(dto -> log.info("Completed fetching user profile: {}", receiverId))
                .doOnError(e -> log.error("Failed to fetch user profile: {}", e.getMessage(), e));

        return cacheService.cacheMono(
                userProfileKey,
                userProfile,
                UserProfileDto.class
        );
    }

//    @Override
//    public Mono<UserProfileDto> getUserByPhoneNumber(String phoneNumber) {
//        return userProfileRepository.findByPhoneNumber(phoneNumber)
//                .map(UserProfileMapper::toDto)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
//                .doOnSubscribe(s -> log.info("Fetching user profile by phone number"))
//                .doOnSuccess(dto -> log.info("Completed fetching user profile by phoneNumber"))
//                .doOnError(e -> log.error("Failed to fetch user profile by email: {}", e.getMessage(), e));
//    }


    @Override
    public Mono<UserProfileDto> getUserByPhoneNumberAndAgentId(String phoneNumber, Long agentId) {

        String userProfileKey = CacheKeyUtil.getUserProfileByPhoneAndAgentIdKey(phoneNumber, agentId);

        Mono<UserProfileDto> userProfile = userProfileRepository.findByPhoneNumberAndAgentId(phoneNumber, agentId)
                .map(UserProfileMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .doOnSubscribe(s -> log.info("Fetching user profile by phone number"))
                .doOnSuccess(dto -> log.info("Completed fetching user profile by phoneNumber"))
                .doOnError(e -> log.error("Failed to fetch user profile by email: {}", e.getMessage(), e));

        return cacheService.cacheMono(
                userProfileKey,
                userProfile,
                UserProfileDto.class
        );
    }


//    @Override
//    public Mono<UserProfileDto> getUserProfileByTelegramId(Long telegramId) {
//
//
//        Mono<UserProfileDto> userProfile = userProfileRepository.findByTelegramId(telegramId)
//                .map(UserProfileMapper::toDto)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found for telegramId: " + telegramId)))
//                .doOnSubscribe(s -> log.info("Fetching user profile for telegramId: {}", telegramId))
//                .doOnSuccess(dto -> {
//                    if (dto != null) {
//                        log.info("Successfully fetched user profile for telegramId: {}", telegramId);
//                    }
//                })
//                .doOnError(e -> log.error("Failed to fetch user profile for telegramId: {} - {}", telegramId, e.getMessage(), e));
//    }


    @Override
    public Mono<UserProfileDto> getUserProfileByTelegramIdAndAgentId(Long telegramId, Long agentId) {

        String userProfileKey = CacheKeyUtil.getUserProfileByTelegramIdAndAgentIdKey(telegramId, agentId);

        Mono<UserProfileDto> userProfile = userProfileRepository.findByTelegramIdAndAgentId(telegramId, agentId)
                .map(UserProfileMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found for telegramId: " + telegramId)))
                .doOnSubscribe(s -> log.info("Fetching user profile for telegramId: {}", telegramId))
                .doOnSuccess(dto -> {
                    if (dto != null) {
                        log.info("Successfully fetched user profile for telegramId: {}", telegramId);
                    }
                })
                .doOnError(e -> log.error("Failed to fetch user profile for telegramId: {} - {}", telegramId, e.getMessage(), e));

        return cacheService.cacheMono(
                userProfileKey,
                userProfile,
                UserProfileDto.class
        );
    }


    @Override
    public Mono<UserProfileDto> changeNameOfAgentUser(Long telegramId, String name, Long agentId) {
        log.info("Changing nickname for telegramId {}: {}", telegramId, name);

        String userProfileByTelegramKey = CacheKeyUtil.getUserProfileByTelegramIdAndAgentIdKey(telegramId, agentId);

        cacheService.evict(userProfileByTelegramKey);

        return userProfileRepository.findByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found for telegramId: " + telegramId)))
                .flatMap(userProfile -> {

                    String userByPhoneKey = CacheKeyUtil.getUserProfileByPhoneAndAgentIdKey(userProfile.getPhoneNumber(), agentId);
                    String userProfileByIdKey = CacheKeyUtil.getUserProfileByIdKey(userProfile.getId());
                    cacheService.evict(userByPhoneKey);
                    cacheService.evict(userProfileByIdKey);

                    userProfile.setNickname(name != null ? name.trim() : null);
                    return userProfileRepository.save(userProfile);
                })
                .map(UserProfileMapper::toDto)
                .doOnSuccess(updated -> log.info("Name changed successfully for telegramId {}: {}", telegramId, updated))
                .doOnError(e -> log.error("Error changing name for telegramId {}: {}", telegramId, e.getMessage(), e));
    }


    @Override
    public Mono<UserIdsResponseDto> getUserTelegramIdsOfAgent(Long agentId) {
        return userProfileRepository.findAllUserTelegramIdsByAgentId(agentId)
                .collectList()
                .map(UserProfileMapper::toUserIdsDto)
                .doOnSubscribe(s -> log.info("Fetching all user IDs"))
                .doOnSuccess(ids -> log.info("Successfully fetched {} user IDs", ids.getIds().size()))
                .doOnError(e -> log.error("Failed to fetch user IDs: {}", e.getMessage(), e));
    }

    @Override
    public Mono<UserProfileDto> createPassword(CreatePasswordRequestDto request) {
        log.info("Creating password for telegramId: {}", request.getTelegramId());

        return userProfileRepository.findByTelegramIdAndAgentId(request.getTelegramId(), request.getAgentId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .flatMap(user -> {
                    // Hash password using BCrypt
                    user.setPassword(passwordEncoder.encode(request.getPassword()));

                    return userProfileRepository.save(user);
                })
                .flatMap(savedUser -> {
                    // Evict caches asynchronously
                    String key1 = CacheKeyUtil.getUserProfileByTelegramIdAndAgentIdKey(request.getTelegramId(), request.getAgentId());
                    String key2 = CacheKeyUtil.getUserProfileByPhoneAndAgentIdKey(savedUser.getPhoneNumber(), savedUser.getAgentId());
                    String key3 = CacheKeyUtil.getUserProfileByIdKey(savedUser.getId());

                    return Mono.when(
                            cacheService.evict(key1),
                            cacheService.evict(key2),
                            cacheService.evict(key3)
                    ).thenReturn(savedUser); // pass savedUser downstream
                })
                .map(UserProfileMapper::toDto);
    }


    @Override
    public Mono<UserProfileDto> updatePassword(UpdatePasswordRequestDto request) {
        log.info("Updating password for telegramId: {}", request.getTelegramId());

        return userProfileRepository.findByTelegramIdAndAgentId(request.getTelegramId(), request.getAgentId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .flatMap(user -> {
                    // Verify old password
                    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                        return Mono.error(new IllegalArgumentException("Old password is incorrect"));
                    }

                    // Hash and set new password
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));

                    return userProfileRepository.save(user);
                })
                .flatMap(savedUser -> {
                    // Evict caches asynchronously
                    String key1 = CacheKeyUtil.getUserProfileByTelegramIdAndAgentIdKey(request.getTelegramId(), request.getAgentId());
                    String key2 = CacheKeyUtil.getUserProfileByPhoneAndAgentIdKey(savedUser.getPhoneNumber(), savedUser.getAgentId());
                    String key3 = CacheKeyUtil.getUserProfileByIdKey(savedUser.getId());

                    return Mono.when(
                            cacheService.evict(key1),
                            cacheService.evict(key2),
                            cacheService.evict(key3)
                    ).thenReturn(savedUser);
                })
                .map(UserProfileMapper::toDto);
    }

    @Override
    public Mono<UserProfileMinimalDto> getUserProfileMinimal(Long userId) {
        return userProfileRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .map(UserProfileMapper::toMinimalDto)
                .doOnSubscribe(s -> log.info("Fetching minimal user profile for userId: {}", userId))
                .doOnSuccess(dto -> log.info("Successfully fetched minimal user profile for userId: {}", userId))
                .doOnError(e -> log.error("Failed to fetch minimal user profile for userId: {} - {}", userId, e.getMessage(), e));
    }


}
