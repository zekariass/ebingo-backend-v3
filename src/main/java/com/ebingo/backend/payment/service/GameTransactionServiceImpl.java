package com.ebingo.backend.payment.service;

import com.ebingo.backend.common.service.DailyAgentAccountingService;
import com.ebingo.backend.common.service.DailyLeaderboardService;
import com.ebingo.backend.common.service.TotalAgentAccountingService;
import com.ebingo.backend.common.service.TotalLeaderboardService;
import com.ebingo.backend.game.repository.RoomRepository;
import com.ebingo.backend.game.service.BingoGameMetricsRedisService;
import com.ebingo.backend.game.state.GameState;
import com.ebingo.backend.payment.dto.GameTransactionDto;
import com.ebingo.backend.payment.entity.DailyCommission;
import com.ebingo.backend.payment.entity.GameTransaction;
import com.ebingo.backend.payment.entity.TotalCommission;
import com.ebingo.backend.payment.enums.GameTxnStatus;
import com.ebingo.backend.payment.enums.GameTxnType;
import com.ebingo.backend.payment.mappers.GameTransactionMapper;
import com.ebingo.backend.payment.mappers.WalletMapper;
import com.ebingo.backend.payment.repository.DailyCommissionRepository;
import com.ebingo.backend.payment.repository.GameTransactionRepository;
import com.ebingo.backend.payment.repository.TotalCommissionRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameTransactionServiceImpl implements GameTransactionService {

    private final GameTransactionRepository gameTransactionRepository;
    private final UserProfileService userProfileService;
    private final RoomRepository roomRepository;
    private final WalletService walletService;
    private final TotalCommissionRepository totalCommissionRepository;
    private final DailyCommissionRepository dailyCommissionRepository;
    private final TransactionalOperator transactionalOperator;

    private final DailyLeaderboardService dailyLeaderboardService;
    private final TotalLeaderboardService totalLeaderboardService;
    private final DailyAgentAccountingService dailyAgentAccountingService;
    private final TotalAgentAccountingService totalAgentAccountingService;
    private final BingoGameMetricsRedisService bingoGameMetricsRedisService;

//    @Override
//    public Mono<GameTransactionDto> createGameTransaction(
//            Long userProfileId,
//            BigDecimal amount,
//            GameTxnType gameTxnType,
//            Long gameId,
//            BigDecimal commissionAmount,
//            BigDecimal singleGameFee) {
//
//        // Defensive null-handling
//        final BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
//        final BigDecimal safeCommission = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
//        final BigDecimal safeSingleFee = singleGameFee != null ? singleGameFee : BigDecimal.ZERO;
//
//        GameTransaction gameTransaction = new GameTransaction();
//        gameTransaction.setGameId(gameId);
//        gameTransaction.setPlayerId(userProfileId);
//        gameTransaction.setTxnAmount(safeAmount);
//        gameTransaction.setTxnType(gameTxnType);
//        gameTransaction.setTxnStatus(GameTxnStatus.SUCCESS);
//        gameTransaction.setCommissionAmount(safeCommission);
//        gameTransaction.setSingleGameFee(safeSingleFee);
//
//        // Core save logic
//        Mono<GameTransaction> txFlow = gameTransactionRepository.save(gameTransaction)
//                .flatMap(savedTxn -> {

    /// /                    // If transaction amount is zero or negative, skip further processing
    /// /                    if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
    /// /                        log.warn("Transaction amount is zero or negative for txn id: {}", savedTxn.getId());
    /// /                        return Mono.empty();
    /// /                    }
//
//                    if (GameTxnType.PRIZE_PAYOUT.equals(gameTxnType)) {
//
//                        // --- Update total commission ---
//                        Mono<TotalCommission> totalCommissionMono = totalCommissionRepository.findAll()
//                                .next()
//                                .map(tc -> {
//                                    if (tc.getTotalCommission() == null) tc.setTotalCommission(BigDecimal.ZERO);
//                                    if (tc.getTotalPrize() == null) tc.setTotalPrize(BigDecimal.ZERO);
//                                    tc.setTotalCommission(tc.getTotalCommission().add(safeCommission));
//                                    tc.setTotalPrize(tc.getTotalPrize().add(safeAmount));
//                                    return tc;
//                                })
//                                .switchIfEmpty(Mono.defer(() -> {
//                                    TotalCommission newTC = new TotalCommission();
//                                    newTC.setTotalCommission(safeCommission);
//                                    newTC.setTotalPrize(safeAmount);
//                                    return Mono.just(newTC);
//                                }))
//                                .flatMap(totalCommissionRepository::save);
//
//                        // --- Update daily commission ---
//                        Mono<DailyCommission> dailyCommissionMono = dailyCommissionRepository.findByCommissionDate(LocalDate.now())
//                                .map(dc -> {
//                                    if (dc.getCommissionCollected() == null) dc.setCommissionCollected(BigDecimal.ZERO);
//                                    if (dc.getTotalPrizeAmount() == null) dc.setTotalPrizeAmount(BigDecimal.ZERO);
//                                    dc.setCommissionCollected(dc.getCommissionCollected().add(safeCommission));
//                                    dc.setTotalPrizeAmount(dc.getTotalPrizeAmount().add(safeAmount));
//                                    dc.setGameCount(dc.getGameCount() + 1);
//                                    return dc;
//                                })
//                                .switchIfEmpty(Mono.defer(() -> {
//                                    DailyCommission newDC = new DailyCommission();
//                                    newDC.setCommissionDate(LocalDate.now());
//                                    newDC.setCommissionCollected(safeCommission);
//                                    newDC.setGameCount(1);
//                                    newDC.setTotalPrizeAmount(safeAmount);
//                                    return Mono.just(newDC);
//                                }))
//                                .flatMap(dailyCommissionRepository::save);
//
//                        // Both updates must succeed before returning
//                        return Mono.when(totalCommissionMono, dailyCommissionMono)
//                                .thenReturn(savedTxn);
//                    } else {
//                        return Mono.just(savedTxn);
//                    }
//                })
//                // Wallet update in same chain (so in transaction)
//                .flatMap(txn -> handleWalletOperation(userProfileId, safeAmount, gameTxnType)
//                        .thenReturn(txn)
//                );
//
//        // Wrap all in transactional operator
//        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            log.warn("Transaction amount is zero or negative for userProfileId: {}. Skipping transaction creation.", userProfileId);
//            return Mono.empty();
//        }
//        return transactionalOperator.transactional(txFlow)
//                .map(GameTransactionMapper::toDto)
//                .doOnSuccess(dto -> log.info("✅ Game transaction created: {}", dto))
//                .doOnError(err -> log.error("❌ Failed to create game transaction", err));
//    }
    @Override
    public Mono<GameTransactionDto> createGameTransaction(
            Long userProfileId,
            BigDecimal amount,
            GameTxnType gameTxnType,
            Long gameId,
            BigDecimal commissionAmount,
            BigDecimal singleGameFee, Long agentId) {

        // Defensive null-handling
        final BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        final BigDecimal safeCommission = commissionAmount != null ? commissionAmount : BigDecimal.ZERO;
        final BigDecimal safeSingleFee = singleGameFee != null ? singleGameFee : BigDecimal.ZERO;

        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Transaction amount is zero or negative for userProfileId: {}. Skipping transaction creation.", userProfileId);
            return Mono.empty();
        }

        return userProfileService.getUserProfileById(userProfileId)
                .flatMap(userProfile -> {
                    if (Boolean.TRUE.equals(userProfile.getIsBot())) {
                        // BOT FLOW: Only wallet operation, return a minimal DTO or leave it
//                        log.info("Handling wallet operation for bot userProfileId: {}", userProfileId);
//                        return handleWalletOperation(userProfileId, safeAmount, gameTxnType, gameId)
//                                .then
                        return Mono.just(GameTransactionDto.builder()
                                .playerId(userProfileId)
                                .agentId(agentId)
                                .txnAmount(safeAmount)
                                .txnType(gameTxnType)
                                .build());
                    } else {
                        // REAL USER FLOW: transaction + commissions + wallet
                        log.info("Handling real user flow for userProfileId: {}", userProfileId);
                        return transactionalOperator.transactional(
                                createAndSaveGameTransaction(userProfileId, gameId, gameTxnType, safeAmount, safeCommission, safeSingleFee, agentId)
                                        .flatMap(txn -> handleWalletOperation(userProfileId, safeAmount, gameTxnType, gameId)
                                                .thenReturn(txn))
                        ).map(GameTransactionMapper::toDto);
                    }
                })
//                .doOnSuccess(dto -> log.info("Game transaction processed: {}", dto))
                .doOnError(err -> log.error("Failed to create game transaction", err));
    }

    // Helper method for real user flow
    private Mono<GameTransaction> createAndSaveGameTransaction(
            Long userProfileId,
            Long gameId,
            GameTxnType gameTxnType,
            BigDecimal amount,
            BigDecimal commissionAmount,
            BigDecimal singleGameFee, Long agentId) {

        GameTransaction gameTransaction = new GameTransaction();
        gameTransaction.setAgentId(agentId);
        gameTransaction.setGameId(gameId);
        gameTransaction.setPlayerId(userProfileId);
        gameTransaction.setTxnAmount(amount);
        gameTransaction.setTxnType(gameTxnType);
        gameTransaction.setTxnStatus(GameTxnStatus.SUCCESS);
        gameTransaction.setCommissionAmount(commissionAmount);
        gameTransaction.setSingleGameFee(singleGameFee);

        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>> Game transaction: {}", amount);

        return gameTransactionRepository.save(gameTransaction)
                .flatMap(savedTxn -> {
                    if (GameTxnType.PRIZE_PAYOUT.equals(gameTxnType)) {

                        Mono<TotalCommission> totalCommissionMono = totalCommissionRepository.findAll()
                                .next()
                                .map(tc -> {
                                    if (tc.getTotalCommission() == null) tc.setTotalCommission(BigDecimal.ZERO);
                                    if (tc.getTotalPrize() == null) tc.setTotalPrize(BigDecimal.ZERO);
                                    tc.setTotalCommission(tc.getTotalCommission().add(commissionAmount));
                                    tc.setTotalPrize(tc.getTotalPrize().add(amount));
                                    return tc;
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    TotalCommission newTC = new TotalCommission();
                                    newTC.setTotalCommission(commissionAmount);
                                    newTC.setTotalPrize(amount);
                                    return Mono.just(newTC);
                                }))
                                .flatMap(totalCommissionRepository::save);

                        Mono<DailyCommission> dailyCommissionMono = dailyCommissionRepository.findByCommissionDate(LocalDate.now())
                                .map(dc -> {
                                    if (dc.getCommissionCollected() == null) dc.setCommissionCollected(BigDecimal.ZERO);
                                    if (dc.getTotalPrizeAmount() == null) dc.setTotalPrizeAmount(BigDecimal.ZERO);
                                    dc.setCommissionCollected(dc.getCommissionCollected().add(commissionAmount));
                                    dc.setTotalPrizeAmount(dc.getTotalPrizeAmount().add(amount));
                                    dc.setGameCount(dc.getGameCount() + 1);
                                    return dc;
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    DailyCommission newDC = new DailyCommission();
                                    newDC.setCommissionDate(LocalDate.now());
                                    newDC.setCommissionCollected(commissionAmount);
                                    newDC.setGameCount(1);
                                    newDC.setTotalPrizeAmount(amount);
                                    return Mono.just(newDC);
                                }))
                                .flatMap(dailyCommissionRepository::save);

                        return Mono.when(totalCommissionMono, dailyCommissionMono)
                                .thenReturn(savedTxn);
                    } else {
                        return Mono.just(savedTxn);
                    }
                });
    }


    /**
     * Handles wallet credit or debit based on transaction type.
     */
    private Mono<Void> handleWalletOperation(Long userProfileId, BigDecimal amount, GameTxnType txnType, Long gameId) {
        return walletService.getWalletByUserProfileId(userProfileId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Wallet not found for userProfileId: " + userProfileId)))
                .flatMap(walletDto -> {
                    if (GameTxnType.PRIZE_PAYOUT.equals(txnType) || GameTxnType.REFUND.equals(txnType)) {
                        log.info("Handling wallet operation for userProfileId : {}, {}, {}",
                                userProfileId, walletDto.getTotalAvailableBalance(), txnType);

                        return walletService.credit(WalletMapper.toEntity(walletDto), amount, txnType, gameId)
                                .doOnSuccess(w -> log.info(" Credited wallet {} with {}", walletDto.getId(), amount))
                                .then();
                    } else if (GameTxnType.GAME_FEE.equals(txnType)) {
                        log.info("Handling wallet operation for userProfileId: {}, {}, {}",
                                userProfileId, walletDto.getTotalAvailableBalance(), txnType);

                        return walletService.debit(WalletMapper.toEntity(walletDto), amount, txnType, gameId)
                                .doOnSuccess(w -> log.info("Debited wallet {} with {}", walletDto.getId(), amount))
                                .then();
                    }
                    return Mono.empty();
                });
    }


    @Override
    public Mono<GameTransactionDto> getTransactionByUserIdAndGameId(Long userId, Long gameId, GameTxnType gameTxnType) {
        return gameTransactionRepository
                .findFirstByPlayerIdAndGameIdAndTxnTypeAndTxnStatusOrderByCreatedAtDesc(userId, gameId, gameTxnType, GameTxnStatus.SUCCESS)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        String.format("Game transaction not found for userId=%d, gameId=%d, type=%s",
                                userId, gameId, gameTxnType)
                )))
                .map(GameTransactionMapper::toDto);
    }

    @Override
    public Flux<GameTransactionDto> getPaginatedGameTransactions(Long telegramId, Long agentId, Integer page, Integer size, String sortBy) {
        int pageNumber = (page != null && page >= 1) ? page : 1;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 10;
        long offset = (long) (pageNumber - 1) * pageSize;

        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                .flatMapMany(up -> {
                    String sortKey = (sortBy != null) ? sortBy.toLowerCase() : "id";
                    return switch (sortKey) {
                        case "txnamount" -> gameTransactionRepository
                                .findByPlayerIdOrderByTxnAmountDesc(up.getId(), pageSize, offset);
                        case "createdat" -> gameTransactionRepository
                                .findByPlayerIdOrderByCreatedAtDesc(up.getId(), pageSize, offset);
                        default -> gameTransactionRepository
                                .findByPlayerIdOrderByIdDesc(up.getId(), pageSize, offset);
                    };
                })
                .map(GameTransactionMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching game transactions - Page: {}, Size: {}, SortBy: {}", page, size, sortBy))
                .doOnComplete(() -> log.info("Completed fetching game transactions for user: {}", telegramId))
                .doOnError(e -> log.error("Failed to fetch game transactions: {}", e.getMessage(), e));
    }

    @Override
    public Mono<GameTransactionDto> getTransactionById(Long txnId, Long agentId, Long telegramId) {
        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .flatMap(up ->
                        gameTransactionRepository.findByIdAndPlayerId(txnId, up.getId())
                                .switchIfEmpty(Mono.error(
                                        new RuntimeException("Game transaction not found with id: " + txnId + " for user: " + up.getId())
                                ))
                                .map(GameTransactionMapper::toDto)
                ).doOnSubscribe(s -> log.info("Fetching game transaction by id: {}", txnId))
                .doOnSuccess(s -> log.info("Game transaction fetched by id: {}", txnId))
                .doOnError(e -> log.error("Failed to fetch game transaction by id: {}", txnId, e));
    }

    @Override
    public Mono<GameTransactionDto> createGameTransactionForPrizePayout(GameState state, Long dbUserId, GameTxnType gameTxnType, Long gameId, Long agentId) {

        return userProfileService.getUserProfileById(dbUserId)
                .flatMap(up -> bingoGameMetricsRedisService.getMetrics(gameId)
                                .flatMap(metrics -> {

                                    final BigDecimal rate = BigDecimal.valueOf(state.getCommissionRate());
                                    final BigDecimal entryFee = BigDecimal.valueOf(state.getEntryFee());

                                    final int totalCards = state.getAllSelectedCardsIds().size();
                                    final BigDecimal totalPot = BigDecimal.valueOf(totalCards).multiply(entryFee); // real + bots (virtual)
                                    final BigDecimal realPot = metrics.realMoneyAmount();                          // real only

                                    // Commissions (two different concepts)
                                    final BigDecimal potCommission = totalPot.multiply(rate);      // on total pot
                                    final BigDecimal realCommission = realPot.multiply(rate);      // on real money only

                                    // Payout depends on winner type (your rule)
                                    final boolean isBotWinner = Boolean.TRUE.equals(up.getIsBot());

                                    final BigDecimal payout = isBotWinner
                                            ? realPot.subtract(realCommission)     // bot wins => platform keeps real after commission
                                            : totalPot.subtract(potCommission);    // real player wins => gets total pot after pot commission

                                    // Choose what "commission" means in the transaction record.
                                    // Usually you'd want REAL commission because that's actual revenue.
                                    final BigDecimal commissionForTxn = realCommission;

                                    // Bot accounting amounts
                                    final BigDecimal botWinAmount = isBotWinner ? payout : BigDecimal.ZERO;

                                    // Prize paid to real players only — bot wins are tracked
                                    // exclusively via botWinAmount, not as prize payout
                                    final BigDecimal prizeForAccounting = isBotWinner ? BigDecimal.ZERO : payout;

                                    final BigDecimal botLossAmount;
                                    if (isBotWinner) {
                                        botLossAmount = BigDecimal.ZERO;
                                    } else {
                                        final BigDecimal botsTotalPot =
                                                BigDecimal.valueOf(metrics.botCardsCount()).multiply(entryFee);

                                        botLossAmount = botsTotalPot.subtract(botsTotalPot.multiply(rate));
                                    }
//                            log.info("<><><><><><><><><><><><><><><><> METRICS: {}", metrics);
//                            log.info("<><><><><><><><><><><><><><><><> " +
//                                            "ALl values for prize payout txn: {}, {}, {}, {}, {}, {}, {}, {}",
//                                    totalCards, totalPot, realPot, payout, potCommission,
//                                    realCommission, botWinAmount, botLossAmount
//                            );

                                    return createGameTransaction(dbUserId, payout, gameTxnType, gameId, commissionForTxn, entryFee, agentId)
                                            .flatMap(txnDto ->
                                                    Mono.when(
                                                                    dailyAgentAccountingService.updateForPrizePayout(
                                                                            agentId,
                                                                            realPot,
                                                                            prizeForAccounting,
                                                                            realCommission,
                                                                            botWinAmount,
                                                                            botLossAmount
                                                                    ),
                                                                    totalAgentAccountingService.updateForPrizePayout(
                                                                            agentId,
                                                                            realPot,
                                                                            prizeForAccounting,
                                                                            realCommission,
                                                                            botWinAmount,
                                                                            botLossAmount
                                                                    )
                                                            )
                                                            .thenReturn(txnDto)
                                            )
                                            // Game txn + accounting upserts commit or roll back together
                                            .as(transactionalOperator::transactional)
                                            .flatMap(txnDto ->
                                                    updateLeaderboardsOnPrizePayout(dbUserId, payout, entryFee, agentId)
                                                            .thenReturn(txnDto)
                                            );
                                })
                );


    }

    private Mono<Void> updateLeaderboardsOnPrizePayout(Long userId, BigDecimal payout, BigDecimal singleGameFee, Long agentId) {

        if (payout == null || payout.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.empty();
        }

        return dailyLeaderboardService.incrementDailyWinsAndPrize(userId, payout, singleGameFee, agentId)
                .then(totalLeaderboardService.incrementTotalWinsAndPrize(userId, payout, singleGameFee, agentId));
    }

    @Override
    public Mono<GameTransactionDto> getRefundByOriginalTransactionId(Long originalTransactionId) {
        return gameTransactionRepository
                .findFirstByIdAndTxnTypeAndTxnStatusOrderByCreatedAtDesc(
                        originalTransactionId, GameTxnType.REFUND, GameTxnStatus.SUCCESS)
                .map(GameTransactionMapper::toDto);
    }

}
