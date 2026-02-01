package com.ebingo.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAccountingDto {
    private Long id;
    private LocalDate accountingDate;
    private BigDecimal dailyDepositAmount;
    private BigDecimal dailyWithdrawalAmount;
    private BigDecimal dailyBetAmount;
    private BigDecimal dailyPrizeAmount;
    private BigDecimal dailyCommissionAmount;
    private BigDecimal dailyBotWinAmount;
    private BigDecimal dailyBotLossAmount;
    private BigDecimal netIncome;
    private BigDecimal dailyPromotionalBonusAmount;
    private BigDecimal dailyWelcomeBonusAmount;
    private Long agentId;
    private LocalDateTime settledAt;
    private BigDecimal settledAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
