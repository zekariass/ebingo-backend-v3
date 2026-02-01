package com.ebingo.backend.common.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotalAccountingUpdateDto {
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalBetAmount;
    private BigDecimal totalPrizeAmount;
    private BigDecimal totalCommissionAmount;
    private BigDecimal totalBotWinAmount;
    private BigDecimal totalBotLossAmount;
    private BigDecimal totalPromotionalBonusAmount;
    private BigDecimal totalWelcomeBonusAmount;
    private BigDecimal totalReferralBonusAmount;
    private BigDecimal totalDepositBonusAmount;
    private LocalDateTime lastSettledAt;
    private BigDecimal settledAmount;
    private LocalDateTime nextSettlementTime;
}
