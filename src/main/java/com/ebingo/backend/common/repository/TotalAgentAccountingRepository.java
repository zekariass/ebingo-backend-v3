package com.ebingo.backend.common.repository;

import com.ebingo.backend.common.entity.TotalAgentAccounting;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface TotalAgentAccountingRepository extends ReactiveCrudRepository<TotalAgentAccounting, Long> {

    Mono<TotalAgentAccounting> findByAgentId(Long agentId);

    /**
     * Atomically apply a settlement to the agent's total accounting.
     * Returns empty if no total accounting row exists for the agent.
     */
    @Query("""
              UPDATE total_agent_accounting
              SET last_settled_at = NOW(),
                  last_settled_amount = :amount,
                  total_settled_amount = total_settled_amount + :amount,
                  updated_at = NOW()
              WHERE agent_id = :agentId
              RETURNING *
            """)
    Mono<TotalAgentAccounting> applySettlement(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id, total_deposit_amount, created_at, updated_at
              )
              VALUES (:agentId, :amount, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_deposit_amount = total_agent_accounting.total_deposit_amount + EXCLUDED.total_deposit_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertDeposit(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id, total_withdrawal_amount, created_at, updated_at
              )
              VALUES (:agentId, :amount, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_withdrawal_amount = total_agent_accounting.total_withdrawal_amount + EXCLUDED.total_withdrawal_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertWithdrawal(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id,
                total_bet_amount, total_prize_amount, total_commission_amount,
                total_bot_win_amount, total_bot_loss_amount,
                created_at, updated_at
              )
              VALUES (:agentId, :bet, :prize, :commission, :botWin, :botLoss, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_bet_amount = total_agent_accounting.total_bet_amount + EXCLUDED.total_bet_amount,
                total_prize_amount = total_agent_accounting.total_prize_amount + EXCLUDED.total_prize_amount,
                total_commission_amount = total_agent_accounting.total_commission_amount + EXCLUDED.total_commission_amount,
                total_bot_win_amount = total_agent_accounting.total_bot_win_amount + EXCLUDED.total_bot_win_amount,
                total_bot_loss_amount = total_agent_accounting.total_bot_loss_amount + EXCLUDED.total_bot_loss_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertPrizePayout(
            Long agentId,
            BigDecimal bet,
            BigDecimal prize,
            BigDecimal commission,
            BigDecimal botWin,
            BigDecimal botLoss
    );

    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id, total_promotional_bonus_amount, created_at, updated_at
              )
              VALUES (:agentId, :amount, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_promotional_bonus_amount = total_agent_accounting.total_promotional_bonus_amount + EXCLUDED.total_promotional_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertPromoBonus(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id, total_welcome_bonus_amount, created_at, updated_at
              )
              VALUES (:agentId, :amount, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_welcome_bonus_amount = total_agent_accounting.total_welcome_bonus_amount + EXCLUDED.total_welcome_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertWelcomeBonus(Long agentId, BigDecimal amount);

    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id, total_referral_bonus_amount, created_at, updated_at
              )
              VALUES (:agentId, :amount, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_referral_bonus_amount = total_agent_accounting.total_referral_bonus_amount + EXCLUDED.total_referral_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertReferralBonus(Long agentId, BigDecimal amount);

    
    @Query("""
              INSERT INTO total_agent_accounting(
                agent_id, total_deposit_bonus_amount, created_at, updated_at
              )
              VALUES (:agentId, :add, NOW(), NOW())
              ON CONFLICT (agent_id)
              DO UPDATE SET
                total_deposit_bonus_amount = total_agent_accounting.total_deposit_bonus_amount + EXCLUDED.total_deposit_bonus_amount,
                updated_at = NOW()
              RETURNING *
            """)
    Mono<TotalAgentAccounting> upsertDepositBonus(Long agentId, BigDecimal add);

    // Pagination queries
    @Query("SELECT * FROM total_agent_accounting ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<TotalAgentAccounting> findAllPaged(int limit, long offset);

    @Query("SELECT * FROM total_agent_accounting ORDER BY total_net_income DESC LIMIT :limit OFFSET :offset")
    Flux<TotalAgentAccounting> findAllPagedSortedByNetIncome(int limit, long offset);

    @Query("SELECT * FROM total_agent_accounting ORDER BY last_settled_at DESC LIMIT :limit OFFSET :offset")
    Flux<TotalAgentAccounting> findAllPagedSortedByLastSettledAt(int limit, long offset);

    @Query("SELECT * FROM total_agent_accounting ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<TotalAgentAccounting> findAllPagedSortedByCreatedAt(int limit, long offset);

    @Query("SELECT * FROM total_agent_accounting ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<TotalAgentAccounting> findAllPagedSortedByUpdatedAt(int limit, long offset);

    @Query("SELECT COUNT(*) FROM total_agent_accounting")
    Mono<Long> countAll();
}
