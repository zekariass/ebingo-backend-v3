package com.ebingo.backend.common.service;


import com.ebingo.backend.common.repository.DailyAgentAccountingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyAgentAccountingServiceImpl implements DailyAgentAccountingService {

    private final DailyAgentAccountingRepository dailyAgentAccountingRepository;

    @Override
    public Mono<Void> updateForDeposit(Long agentId, BigDecimal depositAmount) {
        BigDecimal add = nz(depositAmount);
        if (agentId == null || add.signum() == 0) return Mono.empty();

        return dailyAgentAccountingRepository.upsertDeposit(agentId, add).then();
    }

    @Override
    public Mono<Void> updateForWithdrawal(Long agentId, BigDecimal withdrawalAmount) {
        BigDecimal add = nz(withdrawalAmount);
        if (agentId == null || add.signum() == 0) return Mono.empty();

        return dailyAgentAccountingRepository.upsertWithdrawal(agentId, add).then();
    }

    @Override
    public Mono<Void> updateForPrizePayout(Long agentId, BigDecimal betAmount, BigDecimal prizeAmount,
                                           BigDecimal commissionAmount, BigDecimal botWinAmount, BigDecimal botLossAmount) {

        BigDecimal bet = nz(betAmount);
        BigDecimal prize = nz(prizeAmount);
        BigDecimal commission = nz(commissionAmount);
        BigDecimal botWin = nz(botWinAmount);
        BigDecimal botLoss = nz(botLossAmount);

        if (agentId == null) return Mono.empty();
        if (bet.signum() == 0 && prize.signum() == 0 && commission.signum() == 0 && botWin.signum() == 0 && botLoss.signum() == 0) {
            return Mono.empty();
        }

        return dailyAgentAccountingRepository
                .upsertPrizePayout(agentId, bet, prize, commission, botWin, botLoss)
                .then();
    }

    @Override
    public Mono<Void> updateForPromoBonus(Long agentId, BigDecimal promoBonusAmount) {
        BigDecimal add = nz(promoBonusAmount);
        if (agentId == null || add.signum() == 0) return Mono.empty();

        return dailyAgentAccountingRepository.upsertPromoBonus(agentId, add).then();
    }

    @Override
    public Mono<Void> updateForWelcomeBonus(Long agentId, BigDecimal welcomeBonusAmount) {
        BigDecimal add = nz(welcomeBonusAmount);
        if (agentId == null || add.signum() == 0) return Mono.empty();

        return dailyAgentAccountingRepository.upsertWelcomeBonus(agentId, add).then();
    }

    @Override
    public Mono<Void> updateForReferralBonus(Long agentId, BigDecimal referralBonusAmount) {
        BigDecimal add = nz(referralBonusAmount);
        if (agentId == null || add.signum() == 0) return Mono.empty();

        return dailyAgentAccountingRepository.upsertReferralBonus(agentId, add).then();
    }


    @Override
    public Mono<Void> updateForDepositBonus(Long agentId, BigDecimal depositBonusAmount) {
        BigDecimal add = nz(depositBonusAmount);
        if (agentId == null || add.signum() == 0) return Mono.empty();

        return dailyAgentAccountingRepository.upsertDepositBonus(agentId, add).then();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

}
