package com.ebingo.backend.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("daily_agent_accounting")
public class DailyAgentAccounting {

    @Id
    private Long id;

    @Column("accounting_date")
    private LocalDate accountingDate;

    @Column("daily_deposit_amount")
    private BigDecimal dailyDepositAmount;

    @Column("daily_withdrawal_amount")
    private BigDecimal dailyWithdrawalAmount;

    @Column("daily_bet_amount")
    private BigDecimal dailyBetAmount;

    @Column("daily_prize_amount")
    private BigDecimal dailyPrizeAmount;

    @Column("daily_commission_amount")
    private BigDecimal dailyCommissionAmount;

    @Column("daily_bot_win_amount")
    private BigDecimal dailyBotWinAmount;

    @Column("daily_bot_loss_amount")
    private BigDecimal dailyBotLossAmount;

    @Column("daily_promotional_bonus_amount")
    private BigDecimal dailyPromotionalBonusAmount;

    @Column("daily_welcome_bonus_amount")
    private BigDecimal dailyWelcomeBonusAmount;

    @Column("daily_referral_bonus_amount")
    private BigDecimal dailyReferralBonusAmount;

    @Column("daily_deposit_bonus_amount")
    private BigDecimal dailyDepositBonusAmount;

    // ✅ FIXED + read-only (generated column)
    @org.springframework.data.annotation.ReadOnlyProperty
    @Column("daily_net_income")
    private BigDecimal netIncome;

    @Column("agent_id")
    private Long agentId;

    @Column("settled_at")
    private LocalDateTime settledAt;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
