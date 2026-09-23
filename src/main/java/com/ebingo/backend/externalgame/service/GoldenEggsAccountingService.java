package com.ebingo.backend.externalgame.service;

import com.ebingo.backend.externalgame.entity.GoldenEggsDailyAccounting;
import com.ebingo.backend.externalgame.entity.GoldenEggsTotalAccounting;
import com.ebingo.backend.externalgame.repository.GoldenEggsDailyAccountingRepository;
import com.ebingo.backend.externalgame.repository.GoldenEggsTotalAccountingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Service for managing Golden Eggs accounting
 * Tracks bets, wins, losses, and profit for both daily and total accounting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoldenEggsAccountingService {

    private final GoldenEggsTotalAccountingRepository totalAccountingRepository;
    private final GoldenEggsDailyAccountingRepository dailyAccountingRepository;

    /**
     * Record a bet transaction
     * Updates both daily and total accounting
     */
    public Mono<Void> recordBet(BigDecimal betAmount, String currency, Long agentId) {
        log.info("Recording bet: amount={}, currency={}, agentId={}", betAmount, currency, agentId);
        
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        
        return Mono.zip(
                updateDailyAccountingForBet(today, betAmount, agentId),
                updateTotalAccountingForBet(betAmount, agentId)
        ).then();
    }

    /**
     * Record a win (withdrawal) transaction
     * Updates both daily and total accounting
     */
    public Mono<Void> recordWin(BigDecimal winAmount, String currency, Long agentId) {
        log.info("Recording win: amount={}, currency={}, agentId={}", winAmount, currency, agentId);
        
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        
        return Mono.zip(
                updateDailyAccountingForWin(today, winAmount, agentId),
                updateTotalAccountingForWin(winAmount, agentId)
        ).then();
    }

    /**
     * Record a rollback transaction
     * Updates both daily and total accounting
     */
    public Mono<Void> recordRollback(BigDecimal rollbackAmount, String currency, Long agentId) {
        log.info("Recording rollback: amount={}, currency={}, agentId={}", rollbackAmount, currency, agentId);
        
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        
        return Mono.zip(
                updateDailyAccountingForRollback(today, rollbackAmount, agentId),
                updateTotalAccountingForRollback(rollbackAmount, agentId)
        ).then();
    }

    /**
     * Update daily accounting for a bet
     */
    private Mono<GoldenEggsDailyAccounting> updateDailyAccountingForBet(LocalDate date, BigDecimal betAmount, Long agentId) {
        return dailyAccountingRepository.findByAgentIdAndAccountingDate(agentId, date)
                .switchIfEmpty(Mono.defer(() -> Mono.just(newDailyAccounting(date, agentId))))
                .flatMap(accounting -> {
                    accounting.setDailyBetsCount(accounting.getDailyBetsCount() + 1);
                    accounting.setDailyBetsAmount(accounting.getDailyBetsAmount().add(betAmount));
                    
                    // Recalculate loss and net profit
                    // Net Profit = Bets - Wins (platform perspective, positive = platform profit, negative = platform loss)
                    BigDecimal netProfit = accounting.getDailyBetsAmount().subtract(accounting.getDailyWinsAmount());
                    accounting.setDailyNetProfitAmount(netProfit);
                    
                    // Loss = max(0, Wins - Bets) - only when we paid out more than we received
                    BigDecimal loss = accounting.getDailyWinsAmount().subtract(accounting.getDailyBetsAmount());
                    accounting.setDailyLossAmount(loss.max(BigDecimal.ZERO));
                    
                    accounting.setUpdatedAt(Instant.now());
                    
                    return dailyAccountingRepository.save(accounting);
                })
                // Concurrent create hits the unique (agent_id, accounting_date) constraint;
                // concurrent updates hit the optimistic version check - retry the whole read-modify-write
                .retryWhen(Retry.max(3)
                        .filter(t -> t instanceof DuplicateKeyException || t instanceof OptimisticLockingFailureException));
    }

    /**
     * Update daily accounting for a win
     */
    private Mono<GoldenEggsDailyAccounting> updateDailyAccountingForWin(LocalDate date, BigDecimal winAmount, Long agentId) {
        return dailyAccountingRepository.findByAgentIdAndAccountingDate(agentId, date)
                .switchIfEmpty(Mono.defer(() -> Mono.just(newDailyAccounting(date, agentId))))
                .flatMap(accounting -> {
                    accounting.setDailyWinsAmount(accounting.getDailyWinsAmount().add(winAmount));
                    
                    // Recalculate loss and net profit
                    // Net Profit = Bets - Wins (platform perspective, positive = platform profit, negative = platform loss)
                    BigDecimal netProfit = accounting.getDailyBetsAmount().subtract(accounting.getDailyWinsAmount());
                    accounting.setDailyNetProfitAmount(netProfit);
                    
                    // Loss = max(0, Wins - Bets) - only when we paid out more than we received
                    BigDecimal loss = accounting.getDailyWinsAmount().subtract(accounting.getDailyBetsAmount());
                    accounting.setDailyLossAmount(loss.max(BigDecimal.ZERO));
                    
                    accounting.setUpdatedAt(Instant.now());
                    
                    return dailyAccountingRepository.save(accounting);
                })
                .retryWhen(Retry.max(3)
                        .filter(t -> t instanceof DuplicateKeyException || t instanceof OptimisticLockingFailureException));
    }

    /**
     * Update daily accounting for a rollback
     */
    private Mono<GoldenEggsDailyAccounting> updateDailyAccountingForRollback(LocalDate date, BigDecimal rollbackAmount, Long agentId) {
        return dailyAccountingRepository.findByAgentIdAndAccountingDate(agentId, date)
                .switchIfEmpty(Mono.defer(() -> Mono.just(newDailyAccounting(date, agentId))))
                .flatMap(accounting -> {
                    accounting.setDailyRollbackCount(accounting.getDailyRollbackCount() + 1);
                    accounting.setDailyRollbackAmount(accounting.getDailyRollbackAmount().add(rollbackAmount));
                    
                    // Rollback reduces bets amount
                    accounting.setDailyBetsAmount(accounting.getDailyBetsAmount().subtract(rollbackAmount));
                    accounting.setDailyBetsCount(Math.max(0, accounting.getDailyBetsCount() - 1));
                    
                    // Recalculate loss and net profit
                    // Net Profit = Bets - Wins (platform perspective, positive = platform profit, negative = platform loss)
                    BigDecimal netProfit = accounting.getDailyBetsAmount().subtract(accounting.getDailyWinsAmount());
                    accounting.setDailyNetProfitAmount(netProfit);
                    
                    // Loss = max(0, Wins - Bets) - only when we paid out more than we received
                    BigDecimal loss = accounting.getDailyWinsAmount().subtract(accounting.getDailyBetsAmount());
                    accounting.setDailyLossAmount(loss.max(BigDecimal.ZERO));
                    
                    accounting.setUpdatedAt(Instant.now());
                    
                    return dailyAccountingRepository.save(accounting);
                })
                .retryWhen(Retry.max(3)
                        .filter(t -> t instanceof DuplicateKeyException || t instanceof OptimisticLockingFailureException));
    }

    /**
     * Update total accounting for a bet
     */
    private Mono<GoldenEggsTotalAccounting> updateTotalAccountingForBet(BigDecimal betAmount, Long agentId) {
        return getTotalAccounting(agentId)
                .flatMap(accounting -> {
                    accounting.setTotalBetsCount(accounting.getTotalBetsCount() + 1);
                    accounting.setTotalBetsAmount(accounting.getTotalBetsAmount().add(betAmount));
                    
                    // Recalculate loss and net profit
                    // Net Profit = Bets - Wins (platform perspective, positive = platform profit, negative = platform loss)
                    BigDecimal netProfit = accounting.getTotalBetsAmount().subtract(accounting.getTotalWinsAmount());
                    accounting.setTotalNetProfitAmount(netProfit);
                    
                    // Loss = max(0, Wins - Bets) - only when we paid out more than we received
                    BigDecimal loss = accounting.getTotalWinsAmount().subtract(accounting.getTotalBetsAmount());
                    accounting.setTotalLossAmount(loss.max(BigDecimal.ZERO));
                    
                    accounting.setUpdatedAt(Instant.now());
                    
                    return totalAccountingRepository.save(accounting);
                })
                .retryWhen(Retry.max(3)
                        .filter(t -> t instanceof DuplicateKeyException || t instanceof OptimisticLockingFailureException));
    }

    /**
     * Update total accounting for a win
     */
    private Mono<GoldenEggsTotalAccounting> updateTotalAccountingForWin(BigDecimal winAmount, Long agentId) {
        return getTotalAccounting(agentId)
                .flatMap(accounting -> {
                    accounting.setTotalWinsAmount(accounting.getTotalWinsAmount().add(winAmount));
                    
                    // Recalculate loss and net profit
                    // Net Profit = Bets - Wins (platform perspective, positive = platform profit, negative = platform loss)
                    BigDecimal netProfit = accounting.getTotalBetsAmount().subtract(accounting.getTotalWinsAmount());
                    accounting.setTotalNetProfitAmount(netProfit);
                    
                    // Loss = max(0, Wins - Bets) - only when we paid out more than we received
                    BigDecimal loss = accounting.getTotalWinsAmount().subtract(accounting.getTotalBetsAmount());
                    accounting.setTotalLossAmount(loss.max(BigDecimal.ZERO));
                    
                    accounting.setUpdatedAt(Instant.now());
                    
                    return totalAccountingRepository.save(accounting);
                })
                .retryWhen(Retry.max(3)
                        .filter(t -> t instanceof DuplicateKeyException || t instanceof OptimisticLockingFailureException));
    }

    /**
     * Update total accounting for a rollback
     */
    private Mono<GoldenEggsTotalAccounting> updateTotalAccountingForRollback(BigDecimal rollbackAmount, Long agentId) {
        return getTotalAccounting(agentId)
                .flatMap(accounting -> {
                    accounting.setTotalRollbackCount(accounting.getTotalRollbackCount() + 1);
                    accounting.setTotalRollbackAmount(accounting.getTotalRollbackAmount().add(rollbackAmount));
                    
                    // Rollback reduces bets amount
                    accounting.setTotalBetsAmount(accounting.getTotalBetsAmount().subtract(rollbackAmount));
                    accounting.setTotalBetsCount(Math.max(0, accounting.getTotalBetsCount() - 1));
                    
                    // Recalculate loss and net profit
                    // Net Profit = Bets - Wins (platform perspective, positive = platform profit, negative = platform loss)
                    BigDecimal netProfit = accounting.getTotalBetsAmount().subtract(accounting.getTotalWinsAmount());
                    accounting.setTotalNetProfitAmount(netProfit);
                    
                    // Loss = max(0, Wins - Bets) - only when we paid out more than we received
                    BigDecimal loss = accounting.getTotalWinsAmount().subtract(accounting.getTotalBetsAmount());
                    accounting.setTotalLossAmount(loss.max(BigDecimal.ZERO));
                    
                    accounting.setUpdatedAt(Instant.now());
                    
                    return totalAccountingRepository.save(accounting);
                })
                .retryWhen(Retry.max(3)
                        .filter(t -> t instanceof DuplicateKeyException || t instanceof OptimisticLockingFailureException));
    }

    /**
     * Get or create total accounting record
     */
    private Mono<GoldenEggsTotalAccounting> getTotalAccounting(Long agentId) {
        return totalAccountingRepository.findByAgentId(agentId)
                .switchIfEmpty(Mono.defer(() -> Mono.just(newTotalAccounting(agentId))));
    }

    /**
     * Build a new (unsaved) daily accounting record.
     * The caller mutates and saves it exactly once - saving here as well would
     * double-insert and race on the unique (agent_id, accounting_date) constraint.
     */
    private GoldenEggsDailyAccounting newDailyAccounting(LocalDate date, Long agentId) {
        return GoldenEggsDailyAccounting.builder()
                .agentId(agentId)
                .accountingDate(date)
                .dailyBetsCount(0L)
                .dailyBetsAmount(BigDecimal.ZERO)
                .dailyWinsAmount(BigDecimal.ZERO)
                .dailyLossAmount(BigDecimal.ZERO)
                .dailyNetProfitAmount(BigDecimal.ZERO)
                .dailyRollbackCount(0L)
                .dailyRollbackAmount(BigDecimal.ZERO)
                .isSettled(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Build a new (unsaved) total accounting record.
     */
    private GoldenEggsTotalAccounting newTotalAccounting(Long agentId) {
        return GoldenEggsTotalAccounting.builder()
                .agentId(agentId)
                .totalBetsCount(0L)
                .totalBetsAmount(BigDecimal.ZERO)
                .totalWinsAmount(BigDecimal.ZERO)
                .totalLossAmount(BigDecimal.ZERO)
                .totalNetProfitAmount(BigDecimal.ZERO)
                .totalRollbackCount(0L)
                .totalRollbackAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Get total accounting for a specific agent
     */
    public Mono<GoldenEggsTotalAccounting> getTotalAccountingSummary(Long agentId) {
        return getTotalAccounting(agentId);
    }

    /**
     * Get daily accounting for a specific agent and date
     */
    public Mono<GoldenEggsDailyAccounting> getDailyAccounting(Long agentId, LocalDate date) {
        return dailyAccountingRepository.findByAgentIdAndAccountingDate(agentId, date);
    }

    /**
     * Get daily accounting by ID, scoped to an agent
     */
    public Mono<GoldenEggsDailyAccounting> getDailyAccounting(Long agentId, Long id) {
        return dailyAccountingRepository.findById(id)
                .filter(accounting -> accounting.getAgentId() != null && accounting.getAgentId().equals(agentId));
    }

    /**
     * Get all daily accounting records for a specific agent
     */
    public Flux<GoldenEggsDailyAccounting> getAllDailyAccounting(Long agentId) {
        return dailyAccountingRepository.findByAgentIdOrderByAccountingDateDesc(agentId);
    }

    /**
     * Get daily accounting within date range for a specific agent
     */
    public Flux<GoldenEggsDailyAccounting> getDailyAccountingByDateRange(Long agentId, LocalDate startDate, LocalDate endDate) {
        return dailyAccountingRepository.findByAgentIdAndAccountingDateBetween(agentId, startDate, endDate);
    }

    /**
     * Get unsettled daily accounting records for a specific agent
     */
    public Flux<GoldenEggsDailyAccounting> getUnsettledDailyAccounting(Long agentId) {
        return dailyAccountingRepository.findByAgentIdAndIsSettledFalseOrderByAccountingDateDesc(agentId);
    }

    /**
     * Mark daily accounting as settled, scoped to an agent
     */
    public Mono<GoldenEggsDailyAccounting> settleDailyAccounting(Long agentId, Long id) {
        return dailyAccountingRepository.findById(id)
                .filter(accounting -> accounting.getAgentId() != null && accounting.getAgentId().equals(agentId))
                .flatMap(accounting -> {
                    accounting.setIsSettled(true);
                    accounting.setUpdatedAt(Instant.now());
                    log.info("Settling daily accounting: id={}, date={}", id, accounting.getAccountingDate());
                    return dailyAccountingRepository.save(accounting);
                });
    }

    /**
     * Mark daily accounting as unsettled, scoped to an agent
     */
    public Mono<GoldenEggsDailyAccounting> unsettleDailyAccounting(Long agentId, Long id) {
        return dailyAccountingRepository.findById(id)
                .filter(accounting -> accounting.getAgentId() != null && accounting.getAgentId().equals(agentId))
                .flatMap(accounting -> {
                    accounting.setIsSettled(false);
                    accounting.setUpdatedAt(Instant.now());
                    log.info("Unsettling daily accounting: id={}, date={}", id, accounting.getAccountingDate());
                    return dailyAccountingRepository.save(accounting);
                });
    }
}
