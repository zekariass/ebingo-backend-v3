package com.ebingo.backend.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("wallet")
public class Wallet {

    @Id
    private Long id;

    // Reference to UserProfile by ID (no lazy loading in R2DBC)
    @Column("user_profile_id")
    private Long userProfileId;

    @Column("agent_id")
    private Long agentId;

    @Column("welcome_bonus")
    private BigDecimal welcomeBonus = BigDecimal.ZERO;

    @Column("available_welcome_bonus")
    private BigDecimal availableWelcomeBonus = BigDecimal.ZERO;

    @Column("referral_bonus")
    private BigDecimal referralBonus = BigDecimal.ZERO;

    @Column("available_referral_bonus")
    private BigDecimal availableReferralBonus = BigDecimal.ZERO;

    @Column("total_prize_amount")
    private BigDecimal totalPrizeAmount = BigDecimal.ZERO;

    @Column("pending_withdrawal")
    private BigDecimal pendingWithdrawal = BigDecimal.ZERO;

    @Column("total_available_balance") // The sum of all available balances
    private BigDecimal totalAvailableBalance;

    @Column("available_to_withdraw") // The amount available for withdrawal
    private BigDecimal availableToWithdraw = BigDecimal.ZERO;

    @Column("locked_amount")
    private BigDecimal lockedAmount = BigDecimal.ZERO;

    @Column("deposit_bonus")
    private BigDecimal depositBonus = BigDecimal.ZERO;

    @Column("promotional_bonus")
    private BigDecimal promotionalBonus = BigDecimal.ZERO;

    @Column("last_payment_from")
    private String lastPaymentFrom;

    @Column("created_by")
    private Long createdBy;

    @LastModifiedBy
    @Column("updated_by")
    private Long updatedBy;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}




