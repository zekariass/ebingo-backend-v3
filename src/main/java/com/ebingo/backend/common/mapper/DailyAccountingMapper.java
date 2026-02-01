package com.ebingo.backend.common.mapper;

import com.ebingo.backend.common.dto.DailyAccountingDto;
import com.ebingo.backend.common.dto.DailyAccountingUpdateDto;
import com.ebingo.backend.common.entity.DailyAgentAccounting;

public final class DailyAccountingMapper {

    private DailyAccountingMapper() {
        // Private constructor to prevent instantiation
    }

    public static DailyAccountingDto toDto(DailyAgentAccounting entity) {
        if (entity == null) {
            return null;
        }
        return DailyAccountingDto.builder()
                .id(entity.getId())
                .accountingDate(entity.getAccountingDate())
                .dailyDepositAmount(entity.getDailyDepositAmount())
                .dailyWithdrawalAmount(entity.getDailyWithdrawalAmount())
                .dailyBetAmount(entity.getDailyBetAmount())
                .dailyPrizeAmount(entity.getDailyPrizeAmount())
                .dailyCommissionAmount(entity.getDailyCommissionAmount())
                .dailyBotWinAmount(entity.getDailyBotWinAmount())
                .dailyBotLossAmount(entity.getDailyBotLossAmount())
                .netIncome(entity.getNetIncome())
                .dailyPromotionalBonusAmount(entity.getDailyPromotionalBonusAmount())
                .dailyWelcomeBonusAmount(entity.getDailyWelcomeBonusAmount())
                .agentId(entity.getAgentId())
                .settledAt(entity.getSettledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static DailyAgentAccounting toEntity(DailyAccountingUpdateDto updateDto, DailyAgentAccounting existingEntity) {
        if (updateDto == null || existingEntity == null) {
            return existingEntity;
        }

        if (updateDto.getAccountingDate() != null) {
            existingEntity.setAccountingDate(updateDto.getAccountingDate());
        }
        if (updateDto.getDailyDepositAmount() != null) {
            existingEntity.setDailyDepositAmount(updateDto.getDailyDepositAmount());
        }
        if (updateDto.getDailyWithdrawalAmount() != null) {
            existingEntity.setDailyWithdrawalAmount(updateDto.getDailyWithdrawalAmount());
        }
        if (updateDto.getDailyBetAmount() != null) {
            existingEntity.setDailyBetAmount(updateDto.getDailyBetAmount());
        }
        if (updateDto.getDailyPrizeAmount() != null) {
            existingEntity.setDailyPrizeAmount(updateDto.getDailyPrizeAmount());
        }
        if (updateDto.getDailyCommissionAmount() != null) {
            existingEntity.setDailyCommissionAmount(updateDto.getDailyCommissionAmount());
        }
        if (updateDto.getDailyBotWinAmount() != null) {
            existingEntity.setDailyBotWinAmount(updateDto.getDailyBotWinAmount());
        }
        if (updateDto.getDailyBotLossAmount() != null) {
            existingEntity.setDailyBotLossAmount(updateDto.getDailyBotLossAmount());
        }
        if (updateDto.getDailyPromotionalBonusAmount() != null) {
            existingEntity.setDailyPromotionalBonusAmount(updateDto.getDailyPromotionalBonusAmount());
        }
        if (updateDto.getDailyWelcomeBonusAmount() != null) {
            existingEntity.setDailyWelcomeBonusAmount(updateDto.getDailyWelcomeBonusAmount());
        }
        if (updateDto.getDailyReferralBonusAmount() != null) {
            existingEntity.setDailyReferralBonusAmount(updateDto.getDailyReferralBonusAmount());
        }
        if (updateDto.getDailyDepositBonusAmount() != null) {
            existingEntity.setDailyDepositBonusAmount(updateDto.getDailyDepositBonusAmount());
        }
        if (updateDto.getSettledAt() != null) {
            existingEntity.setSettledAt(updateDto.getSettledAt());
        }

        return existingEntity;
    }
}
