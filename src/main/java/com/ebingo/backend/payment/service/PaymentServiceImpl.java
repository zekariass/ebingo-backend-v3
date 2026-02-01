package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.enums.GameTxnType;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;


@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final UserProfileService userProfileService;
    private final GameTransactionService gameTxnService;

    public PaymentServiceImpl(WalletRepository walletRepository, WalletService walletService, UserProfileService userProfileService, GameTransactionService gameTxnService) {
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.userProfileService = userProfileService;
        this.gameTxnService = gameTxnService;
    }


    @Override
    public Mono<Boolean> processPayment(Long telegramId, BigDecimal amount, Long gameId, Long agentId) {
        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                .flatMap(userProfile -> gameTxnService.createGameTransaction(
                                userProfile.getId(),
                                amount,
                                GameTxnType.GAME_FEE,
                                gameId,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                agentId)
                        .thenReturn(true)
                )
                .onErrorResume(ex -> {
                    log.error("Payment processing failed for gameId={}: {}", gameId, ex.getMessage());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Boolean> processRefund(Long telegramId, Long gameId, Long agentId) {
        log.info("Processing refund for telegramId={}, gameId={} agentId={}", telegramId, gameId, agentId);
        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                .flatMap(userProfile -> {
                    Long userId = userProfile.getId();

                    // Step 1: Get the original GAME_FEE transaction
                    return gameTxnService.getTransactionByUserIdAndGameId(userId, gameId, GameTxnType.GAME_FEE)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Original game transaction not found")))
                            .flatMap(originalTxn -> {
                                Long originalTxnId = originalTxn.getId();

                                // Step 2: Check if a refund already exists
                                return gameTxnService.getRefundByOriginalTransactionId(originalTxnId)
                                        .flatMap(existingRefund -> {
                                            log.info("Refund already exists for originalTxnId={}", originalTxnId);
                                            return Mono.just(true); // idempotent: refund already done
                                        })
                                        .switchIfEmpty(
                                                // Step 3: Create refund if none exists
                                                Mono.defer(() ->
                                                        gameTxnService.createGameTransaction(
                                                                        userId,
                                                                        originalTxn.getTxnAmount(),
                                                                        GameTxnType.REFUND,
                                                                        gameId,
                                                                        BigDecimal.ZERO,
                                                                        BigDecimal.ZERO,
                                                                        agentId)
                                                                .doOnSuccess(txn -> log.info("Refund created for originalTxnId={}", originalTxnId))
                                                                .thenReturn(true)
                                                )
                                        );
                            });
                })
                .onErrorResume(ex -> {
                    log.error("Refund processing failed for telegramId={}, gameId={}", telegramId, gameId, ex);
                    return Mono.just(false); // unexpected errors return false
                });
    }


}
