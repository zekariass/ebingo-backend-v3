package com.ebingo.backend.common.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface TotalAgentAccountingService {
    Mono<Void> updateForDeposit(Long agentId, BigDecimal depositAmount);

    Mono<Void> updateForWithdrawal(Long agentId, BigDecimal withdrawalAmount);

    Mono<Void> updateForPrizePayout(Long agentId, BigDecimal betAmount, BigDecimal prizeAmount, BigDecimal commissionAmount, BigDecimal botWinAmount, BigDecimal botLossAmount);

    Mono<Void> updateForPromoBonus(Long agentId, BigDecimal promoBonusAmount);

    Mono<Void> updateForWelcomeBonus(Long agentId, BigDecimal welcomeBonusAmount);

    Mono<Void> updateForReferralBonus(Long agentId, BigDecimal referralBonusAmount);

    Mono<Void> updateForDepositBonus(Long agentId, BigDecimal depositBonusAmount);
}
