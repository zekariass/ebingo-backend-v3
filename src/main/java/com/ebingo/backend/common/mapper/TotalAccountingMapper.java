package com.ebingo.backend.common.mapper;

import com.ebingo.backend.common.dto.TotalAccountingDto;
import com.ebingo.backend.common.dto.TotalAccountingUpdateDto;
import com.ebingo.backend.common.entity.TotalAgentAccounting;

public final class TotalAccountingMapper {

    private TotalAccountingMapper() {
        // Private constructor to prevent instantiation
    }

    public static TotalAccountingDto toDto(TotalAgentAccounting entity) {
        if (entity == null) {
            return null;
        }
        return TotalAccountingDto.builder()
                .id(entity.getId())
                .totalDepositAmount(entity.getTotalDepositAmount())
                .totalWithdrawalAmount(entity.getTotalWithdrawalAmount())
                .totalBetAmount(entity.getTotalBetAmount())
                .totalPrizeAmount(entity.getTotalPrizeAmount())
                .totalCommissionAmount(entity.getTotalCommissionAmount())
                .totalBotWinAmount(entity.getTotalBotWinAmount())
                .totalBotLossAmount(entity.getTotalBotLossAmount())
                .totalPromotionalBonusAmount(entity.getTotalPromotionalBonusAmount())
                .totalWelcomeBonusAmount(entity.getTotalWelcomeBonusAmount())
                .netIncome(entity.getNetIncome())
                .agentId(entity.getAgentId())
                .lastSettledAt(entity.getLastSettledAt())
                .lastSettledAmount(entity.getLastSettledAmount())
                .totalSettledAmount(entity.getTotalSettledAmount())
                .nextSettlementTime(entity.getNextSettlementTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static TotalAgentAccounting toEntity(TotalAccountingUpdateDto updateDto, TotalAgentAccounting existingEntity) {
        if (updateDto == null || existingEntity == null) {
            return existingEntity;
        }

        if (updateDto.getTotalDepositAmount() != null) {
            existingEntity.setTotalDepositAmount(updateDto.getTotalDepositAmount());
        }
        if (updateDto.getTotalWithdrawalAmount() != null) {
            existingEntity.setTotalWithdrawalAmount(updateDto.getTotalWithdrawalAmount());
        }
        if (updateDto.getTotalBetAmount() != null) {
            existingEntity.setTotalBetAmount(updateDto.getTotalBetAmount());
        }
        if (updateDto.getTotalPrizeAmount() != null) {
            existingEntity.setTotalPrizeAmount(updateDto.getTotalPrizeAmount());
        }
        if (updateDto.getTotalCommissionAmount() != null) {
            existingEntity.setTotalCommissionAmount(updateDto.getTotalCommissionAmount());
        }
        if (updateDto.getTotalBotWinAmount() != null) {
            existingEntity.setTotalBotWinAmount(updateDto.getTotalBotWinAmount());
        }
        if (updateDto.getTotalBotLossAmount() != null) {
            existingEntity.setTotalBotLossAmount(updateDto.getTotalBotLossAmount());
        }
        if (updateDto.getTotalPromotionalBonusAmount() != null) {
            existingEntity.setTotalPromotionalBonusAmount(updateDto.getTotalPromotionalBonusAmount());
        }
        if (updateDto.getTotalWelcomeBonusAmount() != null) {
            existingEntity.setTotalWelcomeBonusAmount(updateDto.getTotalWelcomeBonusAmount());
        }
        if (updateDto.getTotalReferralBonusAmount() != null) {
            existingEntity.setTotalReferralBonusAmount(updateDto.getTotalReferralBonusAmount());
        }
        if (updateDto.getTotalDepositBonusAmount() != null) {
            existingEntity.setTotalDepositBonusAmount(updateDto.getTotalDepositBonusAmount());
        }
        if (updateDto.getLastSettledAt() != null) {
            existingEntity.setLastSettledAt(updateDto.getLastSettledAt());
        }
        if (updateDto.getNextSettlementTime() != null) {
            existingEntity.setNextSettlementTime(updateDto.getNextSettlementTime());
        }

        return existingEntity;
    }
}
