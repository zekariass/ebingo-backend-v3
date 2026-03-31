package com.ebingo.backend.user.controller._public;

import com.ebingo.backend.common.Util;
import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.payment.service.WalletService;
import com.ebingo.backend.system.service.SystemConfigService;
import com.ebingo.backend.user.dto.CreatePasswordRequestDto;
import com.ebingo.backend.user.dto.UpdatePasswordRequestDto;
import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.entity.ReferralHistory;
import com.ebingo.backend.user.enums.ReferralStatus;
import com.ebingo.backend.user.mappers.UserProfileMapper;
import com.ebingo.backend.user.service.ReferralHistoryService;
import com.ebingo.backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "User Profile Public Controller", description = "User Profile Public Controller")
@RequestMapping("/api/v1/public/user-profile")
@RequireAccessToken
@Slf4j
public class UserProfilePublicController {

    private final UserProfileService userProfileService;
    private final WalletService walletService;
    private final ReactiveTransactionManager transactionManager;
    private final ReferralHistoryService referralHistoryService;

    private final SystemConfigService systemConfigService;


    public UserProfilePublicController(UserProfileService userProfileService, WalletService walletService, ReactiveTransactionManager transactionManager, ReferralHistoryService referralHistoryService, SystemConfigService systemConfigService) {
        this.userProfileService = userProfileService;
        this.walletService = walletService;
        this.transactionManager = transactionManager;
        this.referralHistoryService = referralHistoryService;
        this.systemConfigService = systemConfigService;
    }


    @PostMapping("/register")
    @Operation(summary = "Create user profile", description = "Create user profile")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> createUserProfile(
            @Parameter(required = true, description = "User profile")
            @Valid @RequestBody UserProfileCreateDto userProfileDto,
            ServerWebExchange exchange
    ) {
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        // Normalize phone number
        final String phoneNumber = userProfileDto.getPhoneNumber() != null
                ? Util.normalizePhoneNumber(userProfileDto.getPhoneNumber())
                : null;
        userProfileDto.setPhoneNumber(phoneNumber);

        final Long referrerId = userProfileDto.getReferrerId();

        // Region-based welcome bonus
//        final BigDecimal welcomeBonus =
//                (phoneNumber != null && (phoneNumber.startsWith("251") || phoneNumber.startsWith("+251")))
//                        ? BigDecimal.valueOf(10)
//                        : BigDecimal.ZERO;

        Mono<UserProfileDto> registrationFlow = userProfileService.createUserProfile(userProfileDto)
                .flatMap(userProfile ->
                        walletService.createWallet(UserProfileMapper.toEntity(userProfile), true)
                                .thenReturn(userProfile)
                )
                .flatMap(createdUser -> {
                    if (referrerId == null) {
                        log.info("No referrer provided for user: {}", createdUser.getTelegramId());
                        return Mono.just(createdUser);
                    }

                    return userProfileService.getUserProfileByTelegramIdAndAgentId(referrerId, userProfileDto.getAgentId())
                            .flatMap(referrerProfile ->
                                    systemConfigService.getSystemConfigByNameAndAgentId("REFERRAL_BONUS", referrerProfile.getAgentId())
                                            .flatMap(config -> {
                                                // Safe parse with fallback
                                                BigDecimal parsedReferralBonus;
                                                try {
                                                    parsedReferralBonus = new BigDecimal(config.getValue());
                                                } catch (Exception e) {
                                                    parsedReferralBonus = BigDecimal.ZERO;
                                                }

                                                String referrerPhoneNumber = Util.normalizePhoneNumber(referrerProfile.getPhoneNumber());

                                                final BigDecimal referralBonus =
                                                        (referrerPhoneNumber != null && (referrerPhoneNumber.startsWith("251") || referrerPhoneNumber.startsWith("+251")))
                                                                ? parsedReferralBonus
                                                                : BigDecimal.ZERO;

                                                // Validate referral bonus
                                                if (referralBonus.compareTo(BigDecimal.ZERO) <= 0) {
                                                    return saveReferralHistory(
                                                            referrerId,
                                                            createdUser.getTelegramId(),
                                                            referralBonus,
                                                            ReferralStatus.FAILED,
                                                            "Referral bonus must be greater than zero"
                                                    ).thenReturn(createdUser);
                                                }

                                                // Check if referee has been referred before
                                                return referralHistoryService.existsByRefereeId(createdUser.getTelegramId())
                                                        .flatMap(alreadyReferred -> {
                                                            if (Boolean.TRUE.equals(alreadyReferred)) {
                                                                log.warn("User {} already referred — skipping bonus", createdUser.getId());
                                                                return saveReferralHistory(
                                                                        referrerId,
                                                                        createdUser.getTelegramId(),
                                                                        referralBonus,
                                                                        ReferralStatus.FAILED,
                                                                        "User has already been referred before"
                                                                ).thenReturn(createdUser);
                                                            }

                                                            // Metadata for audit
                                                            final Map<String, Object> metadata = Map.of(
                                                                    "refereeId", createdUser.getId(),
                                                                    "refereeTelegramId", createdUser.getTelegramId(),
                                                                    "referrerTelegramId", referrerId,
                                                                    "country", (phoneNumber != null && phoneNumber.startsWith("251")) ? "Ethiopia" : "Unknown",
                                                                    "timestamp", Instant.now().toString()
                                                            );

                                                            // Credit referral bonus to referrer wallet
                                                            return walletService.credit(
                                                                            referrerProfile.getId(),
                                                                            referralBonus,
                                                                            "Referral bonus for inviting " + createdUser.getFirstName(),
                                                                            TransactionType.REFERRAL_BONUS,
                                                                            metadata
                                                                    )
                                                                    .flatMap(walletDto ->
                                                                            saveReferralHistory(
                                                                                    referrerId,
                                                                                    createdUser.getTelegramId(),
                                                                                    referralBonus,
                                                                                    ReferralStatus.COMPLETED,
                                                                                    null
                                                                            )
                                                                    )
                                                                    .doOnSuccess(v -> log.info(
                                                                            "Referral bonus applied: referrerId={} refereeId={}",
                                                                            referrerId, createdUser.getTelegramId()
                                                                    ))
                                                                    .onErrorResume(err -> {
                                                                        log.error("Referral credit failed for referrerId={} refereeId={}: {}",
                                                                                referrerId, createdUser.getTelegramId(), err.getMessage());
                                                                        return saveReferralHistory(
                                                                                referrerId,
                                                                                createdUser.getTelegramId(),
                                                                                referralBonus,
                                                                                ReferralStatus.FAILED,
                                                                                "Referral bonus credit failed: " + err.getMessage()
                                                                        );
                                                                    })
                                                                    .thenReturn(createdUser);
                                                        });
                                            })
                            )
                            .switchIfEmpty(
                                    saveReferralHistory(
                                            referrerId,
                                            createdUser.getTelegramId(),
                                            BigDecimal.ZERO,
                                            ReferralStatus.FAILED,
                                            "Referrer not found"
                                    ).thenReturn(createdUser)
                            )
                            .onErrorResume(err -> {
                                log.error("Referral handling failed for referrerId={} refereeId={}: {}",
                                        referrerId, createdUser.getTelegramId(), err.getMessage());
                                return saveReferralHistory(
                                        referrerId,
                                        createdUser.getTelegramId(),
                                        BigDecimal.ZERO,
                                        ReferralStatus.FAILED,
                                        "Unexpected error: " + err.getMessage()
                                ).thenReturn(createdUser);
                            });
                });

        return operator.transactional(registrationFlow)
                .map(userProfile -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("User profile created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfile)
                        .build())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSubscribe(s -> log.info("Creating user profile with wallet and referral logic"))
                .doOnError(e -> log.error("User registration failed: {}", e.getMessage(), e));
    }

    /**
     * Records referral history safely and consistently.
     */
    private Mono<ReferralHistory> saveReferralHistory(
            Long referrerId,
            Long refereeId,
            BigDecimal amount,
            ReferralStatus status,
            String failReason
    ) {
        ReferralHistory history = new ReferralHistory();
        history.setReferrerId(referrerId);
        history.setRefereeId(refereeId);
        history.setAmount(amount != null ? amount : BigDecimal.ZERO);
        history.setStatus(status);
        history.setFailureReason(failReason);
        history.setCreatedAt(Instant.now());
        history.setUpdatedAt(Instant.now());

        return referralHistoryService.createHistory(history)
                .doOnSuccess(h -> log.info("Referral history recorded: referrer={} referee={} status={} reason={}",
                        referrerId, refereeId, status, failReason))
                .doOnError(e -> log.error("Failed to save referral history for refereeId={} due to: {}", refereeId, e.getMessage()));
    }


    @PostMapping("/initData")
    public Mono<Map<String, Object>> me(@RequestBody String initData) {
        return Mono.just(Map.of("status", "ok"));
    }


    @PostMapping("/create-password")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> createPassword(
            @RequestBody CreatePasswordRequestDto createPasswordRequest,
//            @AuthenticatedTelegramUser TelegramUser admin,
            ServerWebExchange exchange
    ) {
        return userProfileService.createPassword(createPasswordRequest)
                .map(userProfile -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Password created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfile)
                        .build())
                .map(ResponseEntity::ok);
    }

    @PutMapping("/update-password")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> updatePassword(
            @RequestBody UpdatePasswordRequestDto updatePasswordRequestDto,
//            @AuthenticatedTelegramUser TelegramUser admin,
            ServerWebExchange exchange
    ) {
        return userProfileService.updatePassword(updatePasswordRequestDto)
                .map(userProfile -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Password updated successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfile)
                        .build())
                .map(ResponseEntity::ok);
    }
}
