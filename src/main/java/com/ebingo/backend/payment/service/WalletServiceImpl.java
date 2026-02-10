package com.ebingo.backend.payment.service;

import com.ebingo.backend.common.Util;
import com.ebingo.backend.common.service.DailyAgentAccountingService;
import com.ebingo.backend.common.service.DailyLeaderboardService;
import com.ebingo.backend.common.service.TotalAgentAccountingService;
import com.ebingo.backend.common.service.TotalLeaderboardServiceImpl;
import com.ebingo.backend.game.service.BingoGameMetricsRedisService;
import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.payment.dto.WalletWithUserProfileDto;
import com.ebingo.backend.payment.entity.Wallet;
import com.ebingo.backend.payment.enums.GameTxnType;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.payment.mappers.WalletMapper;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.system.exceptions.InsufficientBalanceException;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.system.redis.CacheKeyUtil;
import com.ebingo.backend.system.service.CacheService;
import com.ebingo.backend.system.service.SystemConfigService;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.mappers.UserProfileMapper;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final ReactiveTransactionManager transactionManager;
    private final UserProfileService userProfileService;
    private final SystemConfigService systemConfigService;
    private final CacheService cacheService;
    private final DailyLeaderboardService dailyLeaderboardService;
    private final TotalLeaderboardServiceImpl totalLeaderboardService;
    private final BingoGameMetricsRedisService bingoGameMetricsRedisService;
    private final DailyAgentAccountingService dailyAgentAccountingService;
    private final TotalAgentAccountingService totalAgentAccountingService;

    @Value("${deposit.bonusAmount.rate:0.0}")
    private BigDecimal depositBonusAmountRate;

    @Value("${deposit.bonusAmount.fixed:0.0}")
    private BigDecimal depositBonusAmountFixed;

    @Value("${deposit.bonusAmount.max.isCaped:false}")
    private Boolean bonusIsCaped;

    @Value("${deposit.bonusAmount.max.amount:0.0}")
    private BigDecimal maxBonusAmount;

    @Value("${deposit.lockAmount.rate:0.0}")
    private BigDecimal depositLockAmountRate;

    @Value("${deposit.lockAmount.fixed:0.0}")
    private BigDecimal depositLockAmountFixed;

    @Value("${deposit.lockAmount.max.isCaped:false}")
    private Boolean lockIsCaped;

    @Value("${deposit.lockAmount.max.amount:0.0}")
    private BigDecimal maxLockAmount;


    @Override
    public Mono<WalletDto> createWallet(UserProfile userProfile, Boolean welcomeBonusEligible) {
        log.info("Creating wallet for user with profile: {}", UserProfileMapper.toDto(userProfile));

        // Normalize phone number safely
        final String phoneNumber = userProfile.getPhoneNumber() != null
                ? Util.normalizePhoneNumber(userProfile.getPhoneNumber())
                : "Unknown";

        // Create transactional operator
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        // Decide whether to apply welcome bonus
        Mono<BigDecimal> bonusMono;
        if (Boolean.TRUE.equals(welcomeBonusEligible)) {
            bonusMono = systemConfigService.getSystemConfigByNameAndAgentId("WELCOME_BONUS", userProfile.getAgentId())
                    .flatMap(config -> {
                        try {
                            BigDecimal configValue = new BigDecimal(config.getValue());
                            if (phoneNumber.startsWith("251") || phoneNumber.startsWith("+251")) {
                                return Mono.just(configValue);
                            } else {
                                log.info("User phone not in bonus region ({}), skipping bonus", phoneNumber);
                                return Mono.just(BigDecimal.ZERO);
                            }
                        } catch (Exception e) {
                            log.error("Invalid WELCOME_BONUS value in system config: {}", config.getValue(), e);
                            return Mono.just(BigDecimal.ZERO);
                        }
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("System config for WELCOME_BONUS not found, using 0");
                        return Mono.just(BigDecimal.ZERO);
                    }))
                    .onErrorResume(err -> {
                        log.error("Error retrieving welcome bonus config: {}", err.getMessage());
                        return Mono.just(BigDecimal.ZERO);
                    });
        } else {
            // If not eligible, always zero bonus
            bonusMono = Mono.just(BigDecimal.ZERO);
        }

        return bonusMono.flatMap(actualBonus -> {
            // Initialize wallet entity
            Wallet wallet = new Wallet();
            wallet.setUserProfileId(userProfile.getId());
            wallet.setWelcomeBonus(actualBonus);
            wallet.setAvailableWelcomeBonus(actualBonus);
            wallet.setTotalAvailableBalance(actualBonus);
            wallet.setAgentId(userProfile.getAgentId());
            wallet.setAvailableToWithdraw(
                    actualBonus != null && actualBonus.compareTo(BigDecimal.ZERO) > 0
                            ? BigDecimal.ZERO
                            : actualBonus
            );

            log.info("Final wallet initialization: welcomeBonus={}, userId={}",
                    actualBonus, userProfile.getId());

            return walletRepository.save(wallet)
                    .doOnNext(saved -> log.info("Wallet created with ID={} for userProfileId={}", saved.getId(), userProfile.getId()))
                    .map(WalletMapper::toDto)
                    .as(operator::transactional);
        });
    }


//    @Override
//    public Mono<WalletDto> getWalletByUserProfileId(Long userProfileId) {
//
//        String walletCacheKey = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(userProfileId);
//
//        Mono<WalletDto> walletMono = walletRepository.findByUserProfileId(userProfileId)
//                .doOnSubscribe(s -> log.info("Getting wallet by user profile id: {}", userProfileId))
//                .map(WalletMapper::toDto);
//
//        return cacheService.cacheMono(
//                walletCacheKey,
//                walletMono,
//                WalletDto.class
//        );
//    }

    @Override
    public Mono<WalletDto> getWalletByUserProfileId(Long userProfileId) {

        return walletRepository.findByUserProfileId(userProfileId)
                .doOnSubscribe(s ->
                        log.info("Getting wallet by user profile id: {}", userProfileId)
                )
                .flatMap(wallet -> {
                    String cacheKey =
                            CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(
                                    userProfileId,
                                    wallet.getAgentId()
                            );

                    WalletDto dto = WalletMapper.toDto(wallet);

                    return cacheService.cacheMono(
                            cacheKey,
                            Mono.just(dto),
                            WalletDto.class
                    );
                });
    }


    @Override
    public Mono<WalletWithUserProfileDto> getWalletWithUserProfileByUserProfileId(String phoneNumber, Long agentId) {
        log.info("Getting wallet with user profile by phone number");
        return userProfileService.getUserProfileByPhoneNumberAndAgentId(phoneNumber, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "User not found with phone number: " + phoneNumber)))
                .flatMap(userProfile ->
                        walletRepository.findByUserProfileId(userProfile.getId())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                        "Wallet not found for user with phone number: " + phoneNumber)))
                                .map(wallet ->
                                        WalletMapper.toWalletWithUserProfileDto(
                                                wallet,
                                                UserProfileMapper.toMinimalDto(UserProfileMapper.toEntity(userProfile)) // correct mapping
                                        )
                                )
                );
    }


//    @Override
//    public Mono<WalletDto> getWalletByTelegramId(Long telegramId) {
//        log.info("Getting wallet by user telegram id: {}", telegramId);
//
//        return userProfileService.getUserProfileByTelegramId(telegramId)
//                .flatMap(up -> walletRepository.findByUserProfileId(up.getId()))
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                        "Wallet not found for user with telegram id: " + telegramId)))
//                .map(WalletMapper::toDto);
//    }


    @Override
    public Mono<WalletDto> getWalletByTelegramId(Long telegramId, Long agentId) {
        log.info("Getting wallet by user telegram id: {}", telegramId);

        String walletCacheKey = CacheKeyUtil.getWalletByTelegramIdKey(telegramId, agentId);

        Mono<WalletDto> walletMono = userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .flatMap(up -> walletRepository.findByUserProfileId(up.getId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Wallet not found for user with telegram id: " + telegramId)))
                .map(WalletMapper::toDto);

        return cacheService.cacheMono(
                walletCacheKey,
                walletMono,
                WalletDto.class
        );
    }


    @Override
    public Mono<WalletDto> saveWallet(Wallet wallet, Long agentId) {
        log.info("Saving wallet with id: {}", wallet.getId());

        String walletCacheKey = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(wallet.getUserProfileId(), agentId);
        Mono<Boolean> evictByUserId = cacheService.evict(walletCacheKey);

        Mono<UserProfileDto> userProfileMono = userProfileService.getUserProfileById(wallet.getUserProfileId());

        return evictByUserId
                .then(userProfileMono)
                .flatMap(userProfile -> {
                    String walletCacheKeyByTelegram = CacheKeyUtil.getWalletByTelegramIdKey(userProfile.getTelegramId(), userProfile.getAgentId());

                    Mono<Boolean> evictByTelegram = cacheService.evict(walletCacheKeyByTelegram);

                    return evictByTelegram
                            .then(walletRepository.save(wallet))
                            .doOnSuccess(saved -> log.info("Wallet saved with id: {}", saved.getId()))
                            .map(WalletMapper::toDto);
                })
                .onErrorMap(e -> {
                    log.error("Error saving wallet", e);
                    return new RuntimeException("Failed to save wallet", e);
                });
    }


    @Override
    public Mono<WalletDto> debit(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId) {
        log.info("Debiting wallet with id: {} for amount: {}", wallet.getId(), amount);

        // Fetch wallet fresh from DB to ensure R2DBC tracks it as existing entity
        return walletRepository.findById(wallet.getId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found with id: " + wallet.getId())))
                .flatMap(dbWallet -> {
                    String walletCacheKey = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(dbWallet.getUserProfileId(), dbWallet.getAgentId());
                    Mono<Boolean> evictByUserId = cacheService.evict(walletCacheKey);

                    Mono<UserProfileDto> userProfileMono = userProfileService.getUserProfileById(dbWallet.getUserProfileId());

                    return evictByUserId
                            .then(userProfileMono)
                            .flatMap(userProfile -> {
                                String walletCacheKeyByTelegram = CacheKeyUtil.getWalletByTelegramIdKey(userProfile.getTelegramId(), dbWallet.getAgentId());
                                Mono<Boolean> evictByTelegram = cacheService.evict(walletCacheKeyByTelegram);

                                return evictByTelegram.then(
                                        Mono.defer(() -> {
                                            if (GameTxnType.GAME_FEE.equals(gameTxnType) && !userProfile.getIsBot()) {

                                                // 1️⃣ Check total balance first
                                                if (dbWallet.getTotalAvailableBalance().compareTo(amount) < 0) {
                                        return Mono.error(new InsufficientBalanceException("Insufficient balance"));
                                    }

                                    BigDecimal remaining = amount;

                                    BigDecimal usedFromWelcome = BigDecimal.ZERO;
                                    BigDecimal usedFromReferral = BigDecimal.ZERO;
                                    BigDecimal usedFromPromotional;
                                    BigDecimal usedFromLocked = BigDecimal.ZERO;
                                    BigDecimal usedFromDeposit = BigDecimal.ZERO;

                                    String lastPaymentFrom = "";

                                    // To track real money amount for accounting
                                    BigDecimal totalTakenFromBonus = BigDecimal.ZERO;

                                                // 2️⃣ Debit Welcome Bonus
                                                if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                                                        dbWallet.getAvailableWelcomeBonus().compareTo(BigDecimal.ZERO) > 0) {

                                                    usedFromWelcome = dbWallet.getAvailableWelcomeBonus().min(remaining);
                                                    dbWallet.setAvailableWelcomeBonus(dbWallet.getAvailableWelcomeBonus().subtract(usedFromWelcome));
                                                    dbWallet.setWelcomeBonus(dbWallet.getWelcomeBonus().subtract(usedFromWelcome));
                                                    remaining = remaining.subtract(usedFromWelcome);
                                                    totalTakenFromBonus = totalTakenFromBonus.add(usedFromWelcome);
                                                    lastPaymentFrom += "WELCOME_BONUS/" + usedFromWelcome.toPlainString();
                                                }

                                                // 3️⃣ Debit Referral Bonus
                                                if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                                                        dbWallet.getAvailableReferralBonus().compareTo(BigDecimal.ZERO) > 0) {

                                                    usedFromReferral = dbWallet.getAvailableReferralBonus().min(remaining);
                                                    dbWallet.setAvailableReferralBonus(dbWallet.getAvailableReferralBonus().subtract(usedFromReferral));
                                                    dbWallet.setReferralBonus(dbWallet.getReferralBonus().subtract(usedFromReferral));
                                                    remaining = remaining.subtract(usedFromReferral);
                                                    totalTakenFromBonus = totalTakenFromBonus.add(usedFromReferral);
                                                    lastPaymentFrom += "*REFERRAL_BONUS/" + usedFromReferral.toPlainString();
                                                }

                                                // 4️⃣ Debit Promotional Bonus
                                                if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                                                        dbWallet.getPromotionalBonus().compareTo(BigDecimal.ZERO) > 0) {

                                                    usedFromPromotional = dbWallet.getPromotionalBonus().min(remaining);
                                                    dbWallet.setPromotionalBonus(dbWallet.getPromotionalBonus().subtract(usedFromPromotional));
                                                    remaining = remaining.subtract(usedFromPromotional);
                                                    totalTakenFromBonus = totalTakenFromBonus.add(usedFromPromotional);
                                                    lastPaymentFrom += "*PROMOTIONAL_BONUS/" + usedFromPromotional.toPlainString();
                                                }

                                                // 5️⃣ Debit Locked Amount
                                                if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                                                        dbWallet.getLockedAmount().compareTo(BigDecimal.ZERO) > 0) {

                                                    usedFromLocked = dbWallet.getLockedAmount().min(remaining);
                                                    dbWallet.setLockedAmount(dbWallet.getLockedAmount().subtract(usedFromLocked));
                                                    remaining = remaining.subtract(usedFromLocked);
                                                    lastPaymentFrom += "*LOCKED_AMOUNT/" + usedFromLocked.toPlainString();
                                                }

                                                // 6️⃣ Debit Deposit bonus
                                                if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                                                        dbWallet.getDepositBonus().compareTo(BigDecimal.ZERO) > 0) {

                                                    usedFromDeposit = dbWallet.getDepositBonus().min(remaining);
                                                    dbWallet.setDepositBonus(dbWallet.getDepositBonus().subtract(usedFromDeposit));
                                                    remaining = remaining.subtract(usedFromDeposit);
                                                    totalTakenFromBonus = totalTakenFromBonus.add(usedFromDeposit);
                                                    lastPaymentFrom += "*DEPOSIT_BONUS/" + usedFromDeposit.toPlainString();
                                                }

                                                dbWallet.setLastPaymentFrom(lastPaymentFrom.startsWith("*") ?
                                                        lastPaymentFrom.substring(1) : lastPaymentFrom);

                                                // 7️⃣ Deduct full amount from totalAvailableBalance
                                                dbWallet.setTotalAvailableBalance(dbWallet.getTotalAvailableBalance().subtract(amount));

                                                // Update availableToWithdraw
                                                BigDecimal availableToWithdraw = dbWallet.getTotalAvailableBalance()
                                                        .subtract(dbWallet.getAvailableWelcomeBonus())
                                                        .subtract(dbWallet.getAvailableReferralBonus())
                                                        .subtract(dbWallet.getLockedAmount())
                                                        .subtract(dbWallet.getDepositBonus())
                                                        .subtract(dbWallet.getPromotionalBonus());

                                                dbWallet.setAvailableToWithdraw(availableToWithdraw);

                                                log.info(
                                                        "Debit complete for wallet {}: welcome={}, referral={}, locked={}, deposit={}",
                                                        dbWallet.getId(),
                                                        usedFromWelcome, usedFromReferral, usedFromLocked, usedFromDeposit
                                                );

                                                Mono<WalletDto> savedWalletMono = walletRepository.save(dbWallet)
                                                        .map(WalletMapper::toDto);

                                                final BigDecimal realMoneyAmount = amount.subtract(totalTakenFromBonus);

                                                if (realMoneyAmount.compareTo(BigDecimal.ZERO) > 0) {
                                                    return savedWalletMono.flatMap(dto ->

                                                            // 8️⃣ Update Bingo Game Metrics for accounting purposes
                                                            bingoGameMetricsRedisService.addRealMoneyAmount(gameId, realMoneyAmount)
                                                                    .doOnNext(updatedMetrics ->
                                                                            log.info("Updated bingo game metrics after bonus debit: {}", updatedMetrics)
                                                                    )
                                                                    .thenReturn(dto)
                                                    );
                                                }

                                                return savedWalletMono;
                                            }

                                            // Other transaction types
                                            return Mono.just(WalletMapper.toDto(dbWallet));
                                        })
                                );
                            })
                            .onErrorMap(e -> {
                                log.error("Error debiting wallet", e);
                                return new RuntimeException("Failed to debit wallet", e);
                            });
                });
    }


    public Mono<WalletDto> credit(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId) {
        log.info("Crediting wallet with id: {} for amount: {} and txnType: {}", wallet.getId(), amount, gameTxnType);

        // Fetch wallet fresh from DB to ensure R2DBC tracks it as existing entity
        return walletRepository.findById(wallet.getId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found with id: " + wallet.getId())))
                .flatMap(dbWallet -> {
                    // 🧹 Step 1: Evict wallet cache by userId
                    String walletCacheKey = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(dbWallet.getUserProfileId(), dbWallet.getAgentId());
                    Mono<Boolean> evictByUserId = cacheService.evict(walletCacheKey);

                    // Step 2: Fetch user profile to evict Telegram-based cache too
                    Mono<UserProfileDto> userProfileMono = userProfileService.getUserProfileById(dbWallet.getUserProfileId());

                    return evictByUserId
                            .then(userProfileMono)
                            .flatMap(userProfile -> {
                                String walletCacheKeyByTelegram = CacheKeyUtil.getWalletByTelegramIdKey(userProfile.getTelegramId(), dbWallet.getId());
                    Mono<Boolean> evictByTelegram = cacheService.evict(walletCacheKeyByTelegram);

                    // Continue only after both evictions
                                return evictByTelegram.then(
                                        // === Your existing credit logic below ===
                                        Mono.defer(() -> {
                                            if (GameTxnType.REFUND.equals(gameTxnType) || GameTxnType.PRIZE_PAYOUT.equals(gameTxnType)) {

                                                // Recalculate totalAvailableBalance and availableToWithdraw
                                                BigDecimal totalAvailableBalance = dbWallet.getTotalAvailableBalance().add(amount);

                                                if (GameTxnType.PRIZE_PAYOUT.equals(gameTxnType)) {
                                                    dbWallet.setTotalPrizeAmount(dbWallet.getTotalPrizeAmount().add(amount));
                                                }

                                                // Track how much of this REFUND is explained by "lastPaymentFrom" sources (bonus/locked/etc).
                                                // Anything left over is the "real money" portion.
                                                BigDecimal explainedBySources = BigDecimal.ZERO;

                                                // Handle REFUND → restore amounts back to their source
                                                if (GameTxnType.REFUND.equals(gameTxnType)) {

                                                    String lastPaymentFrom = dbWallet.getLastPaymentFrom();

                                        if (lastPaymentFrom != null && !lastPaymentFrom.isBlank()) {

                                            // Format expected:  WELCOME_BONUS/20.00*REFERRAL_BONUS/40.00*LOCKED_AMOUNT/10.50
                                            String[] sources = lastPaymentFrom.split("\\*");

                                            for (String entry : sources) {

                                                if (!entry.contains("/")) {
                                                    log.warn("Skipping malformed entry in lastPaymentFrom: {}", entry);
                                                    continue;
                                                }

                                                String[] parts = entry.split("/");
                                                if (parts.length != 2) {
                                                    log.warn("Skipping invalid bonus entry '{}'", entry);
                                                    continue;
                                                }

                                                String sourceType = parts[0].trim();
                                                String amountStr = parts[1].trim();

                                                BigDecimal refundAmount;

                                                try {
                                                    refundAmount = new BigDecimal(amountStr);
                                                } catch (Exception ex) {
                                                    log.warn("Invalid refund amount '{}' in '{}'", amountStr, entry);
                                                    continue;
                                                }

                                                // accumulate amounts restored by sources
                                                explainedBySources = explainedBySources.add(refundAmount);

                                                                switch (sourceType) {
                                                                    case "WELCOME_BONUS":
                                                                        dbWallet.setAvailableWelcomeBonus(dbWallet.getAvailableWelcomeBonus().add(refundAmount));
                                                                        dbWallet.setWelcomeBonus(dbWallet.getWelcomeBonus().add(refundAmount));
                                                                        break;

                                                                    case "REFERRAL_BONUS":
                                                                        dbWallet.setAvailableReferralBonus(dbWallet.getAvailableReferralBonus().add(refundAmount));
                                                                        dbWallet.setReferralBonus(dbWallet.getReferralBonus().add(refundAmount));
                                                                        break;

                                                                    case "LOCKED_AMOUNT":
                                                                        dbWallet.setLockedAmount(dbWallet.getLockedAmount().add(refundAmount));
                                                                        break;

                                                                    case "DEPOSIT_BONUS":
                                                                        dbWallet.setDepositBonus(dbWallet.getDepositBonus().add(refundAmount));
                                                                        break;

                                                                    case "PROMOTIONAL_BONUS":
                                                                        dbWallet.setPromotionalBonus(dbWallet.getPromotionalBonus().add(refundAmount));
                                                                        break;

                                                                    default:
                                                                        log.warn("Unknown refund source type: {}", sourceType);
                                                                }

                                            }
                                        }
                                    }

                                                BigDecimal availableToWithdraw = totalAvailableBalance
                                                        .subtract(dbWallet.getAvailableWelcomeBonus())
                                                        .subtract(dbWallet.getAvailableReferralBonus())
                                                        .subtract(dbWallet.getDepositBonus())
                                                        .subtract(dbWallet.getPromotionalBonus())
                                                        .subtract(dbWallet.getLockedAmount());

                                                dbWallet.setTotalAvailableBalance(totalAvailableBalance);
                                                dbWallet.setAvailableToWithdraw(availableToWithdraw);

                                                log.info("Credit complete for wallet {} (total available balance: {}, available to withdraw: {})",
                                                        dbWallet.getId(), totalAvailableBalance, availableToWithdraw);

                                                // ----- NEW: deduct real-money portion from bingo metrics on REFUND (without changing existing behavior) -----
                                                // realPortion = amount - explainedBySources, clipped at 0
                                                final BigDecimal realPortion = GameTxnType.REFUND.equals(gameTxnType)
                                                        ? amount.subtract(explainedBySources).max(BigDecimal.ZERO)
                                                        : BigDecimal.ZERO;

                                                Mono<WalletDto> savedWalletMono = walletRepository.save(dbWallet)
                                            .map(WalletMapper::toDto);

                                    // Only apply metrics update if:
                                    // - it’s a REFUND
                                    // - we have a gameId
                                    // - there is a real-money portion > 0
                                    if (GameTxnType.REFUND.equals(gameTxnType)
                                            && gameId != null
                                            && realPortion.compareTo(BigDecimal.ZERO) > 0) {

                                        return savedWalletMono.flatMap(dto ->
                                                bingoGameMetricsRedisService
                                                        .addRealMoneyAmount(gameId, realPortion.negate())
                                                        .doOnNext(updated ->
                                                                log.info("Deducted real-money refund portion {} from bingo metrics for game {}. Updated: {}",
                                                                        realPortion, gameId, updated)
                                                        )
                                                        .thenReturn(dto)
                                        );
                                    }

                                    return savedWalletMono;
                                }

                                return Mono.error(new IllegalArgumentException("Unsupported transaction type for credit: " + gameTxnType));
                            })
                    );
                })
                .onErrorMap(e -> {
                    log.error("Error crediting wallet", e);
                    return new RuntimeException("Failed to credit wallet", e);
                });
                });
    }


    @Override
    public Mono<WalletDto> credit(Long userProfileId,
                                  BigDecimal amount,
                                  String reason,
                                  TransactionType transactionType,
                                  Map<String, Object> metadata) {

        log.info("Crediting wallet | userProfileId={} | amount={} | reason={}",
                userProfileId, amount, reason);

        return userProfileService.getUserProfileById(userProfileId)
                .flatMap(userProfile ->
                        walletRepository.findByUserProfileId(userProfileId)
                                .switchIfEmpty(Mono.error(
                                        new ResourceNotFoundException("Wallet not found for user profile id: " + userProfileId)
                                ))
                                .flatMap(wallet -> {

                                    // === Null-safe initialization ===
                                    wallet.setTotalAvailableBalance(defaultZero(wallet.getTotalAvailableBalance()).add(amount));
                                    wallet.setDepositBonus(defaultZero(wallet.getDepositBonus()));
                                    wallet.setLockedAmount(defaultZero(wallet.getLockedAmount()));
                                    wallet.setReferralBonus(defaultZero(wallet.getReferralBonus()));
                                    wallet.setAvailableReferralBonus(defaultZero(wallet.getAvailableReferralBonus()));
                                    wallet.setWelcomeBonus(defaultZero(wallet.getWelcomeBonus()));
                                    wallet.setAvailableWelcomeBonus(defaultZero(wallet.getAvailableWelcomeBonus()));
                                    wallet.setPromotionalBonus(defaultZero(wallet.getPromotionalBonus()));

                                    // Variables to update accounting
                                    BigDecimal txnAmount = BigDecimal.ZERO;
                                    BigDecimal bonusAmount = BigDecimal.ZERO;

                                    // === Transaction logic ===
                                    if (TransactionType.DEPOSIT.equals(transactionType)) {
                                        BigDecimal depositBonus =
                                                amount.multiply(depositBonusAmountRate).add(depositBonusAmountFixed);

                                        if (Boolean.TRUE.equals(bonusIsCaped) &&
                                                depositBonus.compareTo(maxBonusAmount) > 0) {
                                            depositBonus = maxBonusAmount;
                                        }

                                        BigDecimal depositLockAmount =
                                                amount.add(depositBonus)
                                                        .multiply(depositLockAmountRate)
                                                        .add(depositLockAmountFixed);

                                        if (Boolean.TRUE.equals(lockIsCaped) &&
                                                depositLockAmount.compareTo(maxLockAmount) > 0) {
                                            depositLockAmount = maxLockAmount;
                                        }

                                        wallet.setDepositBonus(wallet.getDepositBonus().add(depositBonus));
                                        wallet.setLockedAmount(wallet.getLockedAmount().add(depositLockAmount));
                                        wallet.setTotalAvailableBalance(
                                                wallet.getTotalAvailableBalance().add(depositBonus)
                                        );

                                        txnAmount = amount;
                                        bonusAmount = depositBonus;
                                    }

                                    if (TransactionType.REFERRAL_BONUS.equals(transactionType)) {
                                        wallet.setReferralBonus(wallet.getReferralBonus().add(amount));
                                        wallet.setAvailableReferralBonus(wallet.getAvailableReferralBonus().add(amount));
                                        bonusAmount = amount;
                                    }

                                    if (TransactionType.WELCOME_BONUS.equals(transactionType)) {
                                        wallet.setWelcomeBonus(wallet.getWelcomeBonus().add(amount));
                                        wallet.setAvailableWelcomeBonus(wallet.getAvailableWelcomeBonus().add(amount));
                                        bonusAmount = amount;
                                    }

                                    if (TransactionType.PROMOTIONAL_BONUS.equals(transactionType)) {
                                        wallet.setPromotionalBonus(wallet.getPromotionalBonus().add(amount));
                                        bonusAmount = amount;
                                    }

                                    // === availableToWithdraw ===
                                    BigDecimal availableToWithdraw = wallet.getTotalAvailableBalance()
                                            .subtract(wallet.getAvailableWelcomeBonus())
                                            .subtract(wallet.getAvailableReferralBonus())
                                            .subtract(wallet.getPromotionalBonus())
                                            .subtract(wallet.getLockedAmount())
                                            .subtract(wallet.getDepositBonus());

                                    wallet.setAvailableToWithdraw(
                                            availableToWithdraw.max(BigDecimal.ZERO)
                                    );

                                    // === Leaderboards ===
                                    Mono<Void> leaderboardUpdate = Mono.empty();
                                    if (TransactionType.DEPOSIT.equals(transactionType)) {
                                        leaderboardUpdate =
                                                dailyLeaderboardService.incrementDailyDeposit(userProfileId, amount, userProfile.getAgentId())
                                                        .then(totalLeaderboardService.incrementTotalDeposit(userProfileId, amount, userProfile.getAgentId()));
                                    }

                                    final BigDecimal txnAmountFinal = txnAmount;
                                    final BigDecimal bonusAmountFinal = bonusAmount;
                                    return walletRepository.save(wallet)
                                            .then(accountingUpdate(wallet.getAgentId(), transactionType, txnAmountFinal, bonusAmountFinal))
                                            .then(leaderboardUpdate)
                                            .thenReturn(wallet);
                                })
                                .flatMap(savedWallet -> {
                                    // === Cache eviction AFTER successful save ===
                                    String byUserId = CacheKeyUtil.getWalletByUserProfileIdAndAgentIdKey(userProfileId, savedWallet.getAgentId());
                                    String byTelegramId = CacheKeyUtil.getWalletByTelegramIdKey(userProfile.getTelegramId(), savedWallet.getAgentId());

                                    return cacheService.evict(byUserId)
                                            .then(cacheService.evict(byTelegramId))
                                            .thenReturn(savedWallet);
                                })
                )
                .map(WalletMapper::toDto)
                .doOnSuccess(dto ->
                        log.info("Wallet credited successfully | userProfileId={} | availableToWithdraw={}",
                                userProfileId, dto.getAvailableToWithdraw())
                )
                .doOnError(e ->
                        log.error("Failed to credit wallet | userProfileId={}", userProfileId, e)
                );
    }

    private Mono<Void> accountingUpdate(Long agentId, TransactionType transactionType, BigDecimal txnAmountFinal, BigDecimal bonusAmountFinal) {
        if (agentId == null) {
            return Mono.empty();
        }

        return switch (transactionType) {
            case DEPOSIT -> totalAgentAccountingService.updateForDeposit(agentId, txnAmountFinal)
                    .then(dailyAgentAccountingService.updateForDeposit(agentId, txnAmountFinal))
                    .then(totalAgentAccountingService.updateForDepositBonus(agentId, bonusAmountFinal)
                            .then(dailyAgentAccountingService.updateForDepositBonus(agentId, bonusAmountFinal)));
            case WELCOME_BONUS -> totalAgentAccountingService.updateForWelcomeBonus(agentId, bonusAmountFinal)
                    .then(dailyAgentAccountingService.updateForWelcomeBonus(agentId, bonusAmountFinal));
            case REFERRAL_BONUS -> totalAgentAccountingService.updateForReferralBonus(agentId, bonusAmountFinal)
                    .then(dailyAgentAccountingService.updateForReferralBonus(agentId, bonusAmountFinal));
            case PROMOTIONAL_BONUS -> totalAgentAccountingService.updateForPromoBonus(agentId, bonusAmountFinal)
                    .then(dailyAgentAccountingService.updateForPromoBonus(agentId, bonusAmountFinal));
            default -> Mono.empty();
        };
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }


}
