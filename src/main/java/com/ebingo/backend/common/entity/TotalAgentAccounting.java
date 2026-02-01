package com.ebingo.backend.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("total_agent_accounting")
public class TotalAgentAccounting {

    @Id
    private Long id;

    @Column("total_deposit_amount")
    private BigDecimal totalDepositAmount;

    @Column("total_withdrawal_amount")
    private BigDecimal totalWithdrawalAmount;

    @Column("total_bet_amount")
    private BigDecimal totalBetAmount;

    @Column("total_prize_amount")
    private BigDecimal totalPrizeAmount;

    @Column("total_commission_amount")
    private BigDecimal totalCommissionAmount;

    @Column("total_bot_win_amount")
    private BigDecimal totalBotWinAmount;

    @Column("total_bot_loss_amount")
    private BigDecimal totalBotLossAmount;

    @Column("total_promotional_bonus_amount")
    private BigDecimal totalPromotionalBonusAmount;

    @Column("total_welcome_bonus_amount")
    private BigDecimal totalWelcomeBonusAmount;

    @Column("total_referral_bonus_amount")
    private BigDecimal totalReferralBonusAmount;

    @Column("total_deposit_bonus_amount")
    private BigDecimal totalDepositBonusAmount;

    /**
     * Generated column (read-only)
     */
    @ReadOnlyProperty
    @Column("total_net_income")
    private BigDecimal netIncome;

    @Column("agent_id")
    private Long agentId;

    @Column("last_settled_at")
    private LocalDateTime lastSettledAt;

    @Column("last_settled_amount")
    private BigDecimal lastSettledAmount;

    @Column("total_settled_amount")
    private BigDecimal totalSettledAmount;

    @Column("next_settlement_time")
    private LocalDateTime nextSettlementTime;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
