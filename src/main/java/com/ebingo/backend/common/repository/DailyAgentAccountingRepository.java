package com.ebingo.backend.common.repository;

import com.ebingo.backend.common.entity.DailyAgentAccounting;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyAgentAccountingRepository extends ReactiveCrudRepository<DailyAgentAccounting, Long> {
    Mono<DailyAgentAccounting> findByAgentIdAndAccountingDate(Long agentId, LocalDate today);

    @Query("SELECT * FROM daily_agent_accounting WHERE agent_id = :agentId AND accounting_date = CURRENT_DATE")
    Mono<DailyAgentAccounting> findTodayByAgentId(Long agentId);

    @Query("SELECT CURRENT_DATE")
    Mono<LocalDate> getCurrentDate();

    /**
     * Atomically mark a past, unsettled record as settled.
     * Returns empty if the record doesn't exist, is already settled, or is today's record.
     */
    @Query("""
              UPDATE daily_agent_accounting
              SET settled_at = NOW(), updated_at = NOW()
              WHERE id = :id
                AND settled_at IS NULL
                AND accounting_date < CURRENT_DATE
              RETURNING *
            """)
    Mono<DailyAgentAccounting> applySettlement(Long id);

    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_deposit_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :amount, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_deposit_amount = daily_agent_accounting.daily_deposit_amount + EXCLUDED.daily_deposit_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertDeposit(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_withdrawal_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :amount, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_withdrawal_amount = daily_agent_accounting.daily_withdrawal_amount + EXCLUDED.daily_withdrawal_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertWithdrawal(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_bet_amount, daily_prize_amount, daily_commission_amount,
                daily_bot_win_amount, daily_bot_loss_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :bet, :prize, :commission, :botWin, :botLoss, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_bet_amount = daily_agent_accounting.daily_bet_amount + EXCLUDED.daily_bet_amount,
                daily_prize_amount = daily_agent_accounting.daily_prize_amount + EXCLUDED.daily_prize_amount,
                daily_commission_amount = daily_agent_accounting.daily_commission_amount + EXCLUDED.daily_commission_amount,
                daily_bot_win_amount = daily_agent_accounting.daily_bot_win_amount + EXCLUDED.daily_bot_win_amount,
                daily_bot_loss_amount = daily_agent_accounting.daily_bot_loss_amount + EXCLUDED.daily_bot_loss_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertPrizePayout(
            Long agentId,
            BigDecimal bet, BigDecimal prize, BigDecimal commission,
            BigDecimal botWin, BigDecimal botLoss
    );

    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_promotional_bonus_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :amount, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_promotional_bonus_amount = daily_agent_accounting.daily_promotional_bonus_amount + EXCLUDED.daily_promotional_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertPromoBonus(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_welcome_bonus_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :amount, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_welcome_bonus_amount = daily_agent_accounting.daily_welcome_bonus_amount + EXCLUDED.daily_welcome_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertWelcomeBonus(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_referral_bonus_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :amount, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_referral_bonus_amount = daily_agent_accounting.daily_referral_bonus_amount + EXCLUDED.daily_referral_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertReferralBonus(Long agentId, BigDecimal amount);


    @Query("""
              INSERT INTO daily_agent_accounting(
                agent_id, accounting_date,
                daily_deposit_bonus_amount,
                created_at, updated_at
              )
              VALUES (:agentId, CURRENT_DATE, :add, NOW(), NOW())
              ON CONFLICT (accounting_date, agent_id)
              DO UPDATE SET
                daily_deposit_bonus_amount = daily_agent_accounting.daily_deposit_bonus_amount + EXCLUDED.daily_deposit_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<DailyAgentAccounting> upsertDepositBonus(Long agentId, BigDecimal add);

    // Pagination queries
    @Query("SELECT * FROM daily_agent_accounting ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findAllPaged(int limit, long offset);

    @Query("SELECT * FROM daily_agent_accounting ORDER BY accounting_date DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findAllPagedSortedByAccountingDate(int limit, long offset);

    @Query("SELECT * FROM daily_agent_accounting ORDER BY daily_net_income DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findAllPagedSortedByNetIncome(int limit, long offset);

    @Query("SELECT * FROM daily_agent_accounting ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findAllPagedSortedByCreatedAt(int limit, long offset);

    @Query("SELECT * FROM daily_agent_accounting ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findAllPagedSortedByUpdatedAt(int limit, long offset);

    @Query("SELECT * FROM daily_agent_accounting WHERE agent_id = :agentId ORDER BY accounting_date DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findByAgentIdPaged(Long agentId, int limit, long offset);

    @Query("SELECT COUNT(*) FROM daily_agent_accounting")
    Mono<Long> countAll();

    @Query("SELECT COUNT(*) FROM daily_agent_accounting WHERE agent_id = :agentId")
    Mono<Long> countByAgentId(Long agentId);

    // Date range queries for agent
    @Query("SELECT * FROM daily_agent_accounting WHERE agent_id = :agentId " +
            "AND accounting_date >= :startDate AND accounting_date <= :endDate " +
            "ORDER BY accounting_date DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findByAgentIdAndDateRangePaged(
            Long agentId, LocalDate startDate, LocalDate endDate, int limit, long offset);

    @Query("SELECT COUNT(*) FROM daily_agent_accounting WHERE agent_id = :agentId " +
            "AND accounting_date >= :startDate AND accounting_date <= :endDate")
    Mono<Long> countByAgentIdAndDateRange(Long agentId, LocalDate startDate, LocalDate endDate);

    // Today's records for all agents (admin) — DB-side CURRENT_DATE keeps
    // accounting_date consistent with the upserts regardless of JVM timezone
    @Query("SELECT * FROM daily_agent_accounting WHERE accounting_date = CURRENT_DATE " +
            "ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<DailyAgentAccounting> findTodayPaged(int limit, long offset);

    @Query("SELECT COUNT(*) FROM daily_agent_accounting WHERE accounting_date = CURRENT_DATE")
    Mono<Long> countToday();
}

