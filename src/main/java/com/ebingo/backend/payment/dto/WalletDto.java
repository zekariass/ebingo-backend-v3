package com.ebingo.backend.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WalletDto {
    private Long id;

    private Long userProfileId;
    private Long agentId;

//    private BigDecimal totalDeposit;

    private BigDecimal welcomeBonus;

    private BigDecimal availableWelcomeBonus;

    private BigDecimal referralBonus;

    private BigDecimal availableReferralBonus;

    private BigDecimal totalPrizeAmount;

    private BigDecimal pendingWithdrawal;

    private BigDecimal totalAvailableBalance;

    private BigDecimal availableToWithdraw;

    private BigDecimal lockedAmount;

    private BigDecimal depositBonus;

    private BigDecimal promotionalBonus;

    private String lastPaymentFrom;
}
