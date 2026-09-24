package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.Wallet;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

public interface WalletRepository extends ReactiveCrudRepository<Wallet, Long> {

    @Query("SELECT * FROM wallet WHERE user_profile_id = :userProfileId")
    Mono<Wallet> findByUserProfileId(Long userProfileId);

    /**
     * Explicit INSERT for wallets with a caller-supplied id.
     * Required because R2DBC treats entities with a non-null @Id as updates.
     */
    @Modifying
    @Query("INSERT INTO wallet (id, user_profile_id, agent_id, welcome_bonus, available_welcome_bonus, " +
            "referral_bonus, available_referral_bonus, total_prize_amount, pending_withdrawal, " +
            "total_available_balance, available_to_withdraw, locked_amount, deposit_bonus, promotional_bonus, " +
            "last_payment_from, created_by, updated_by, created_at, updated_at, \"version\") " +
            "VALUES (:id, :userProfileId, :agentId, 0, 0, 0, 0, 0, 0, :balance, 0, 0, 0, 0, NULL, NULL, NULL, " +
            ":createdAt, :updatedAt, 1)")
    Mono<Integer> insertWallet(@Param("id") Long id,
                               @Param("userProfileId") Long userProfileId,
                               @Param("agentId") Long agentId,
                               @Param("balance") BigDecimal balance,
                               @Param("createdAt") Instant createdAt,
                               @Param("updatedAt") Instant updatedAt);

    /**
     * Counts existing wallets that would collide with a bulk insert:
     * id or user_profile_id in [startId, endId].
     */
    @Query("SELECT COUNT(*) FROM wallet WHERE " +
            "(id BETWEEN :startId AND :endId) OR (user_profile_id BETWEEN :startId AND :endId)")
    Mono<Long> countConflictingWallets(@Param("startId") Long startId, @Param("endId") Long endId);
}

