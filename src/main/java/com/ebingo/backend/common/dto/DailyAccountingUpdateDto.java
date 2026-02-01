package com.ebingo.backend.common.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAccountingUpdateDto {
    private LocalDate accountingDate;
    private BigDecimal dailyDepositAmount;
    private BigDecimal dailyWithdrawalAmount;
    private BigDecimal dailyBetAmount;
    private BigDecimal dailyPrizeAmount;
    private BigDecimal dailyCommissionAmount;
    private BigDecimal dailyBotWinAmount;
    private BigDecimal dailyBotLossAmount;
    private BigDecimal dailyPromotionalBonusAmount;
    private BigDecimal dailyWelcomeBonusAmount;
    private BigDecimal dailyReferralBonusAmount;
    private BigDecimal dailyDepositBonusAmount;
    private LocalDateTime settledAt;
}
