package com.ebingo.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalAccountingDto {
    private Long id;
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalBetAmount;
    private BigDecimal totalPrizeAmount;
    private BigDecimal totalCommissionAmount;
    private BigDecimal totalBotWinAmount;
    private BigDecimal totalBotLossAmount;
    private BigDecimal totalPromotionalBonusAmount;
    private BigDecimal totalWelcomeBonusAmount;
    private BigDecimal netIncome;
    private Long agentId;
    private LocalDateTime lastSettledAt;
    private BigDecimal lastSettledAmount;
    private BigDecimal totalSettledAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
