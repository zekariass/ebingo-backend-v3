package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.payment.dto.WalletWithUserProfileDto;
import com.ebingo.backend.payment.entity.Wallet;
import com.ebingo.backend.user.dto.UserProfileMinimalDto;

public final class WalletMapper {
    public static WalletDto toDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .agentId(wallet.getAgentId())
                .userProfileId(wallet.getUserProfileId())
                .welcomeBonus(wallet.getWelcomeBonus())
                .availableWelcomeBonus(wallet.getAvailableWelcomeBonus())
                .referralBonus(wallet.getReferralBonus())
                .availableReferralBonus(wallet.getAvailableReferralBonus())
                .totalPrizeAmount(wallet.getTotalPrizeAmount())
                .pendingWithdrawal(wallet.getPendingWithdrawal())
                .totalAvailableBalance(wallet.getTotalAvailableBalance())
                .availableToWithdraw(wallet.getAvailableToWithdraw())
                .lockedAmount(wallet.getLockedAmount())
                .depositBonus(wallet.getDepositBonus())
                .promotionalBonus(wallet.getPromotionalBonus())
                .lastPaymentFrom(wallet.getLastPaymentFrom())
                .build();
    }

    public static Wallet toEntity(WalletDto walletDto) {
        Wallet wallet = new Wallet();
        wallet.setId(walletDto.getId());
        wallet.setAgentId(walletDto.getAgentId());
        wallet.setUserProfileId(walletDto.getUserProfileId());
        wallet.setWelcomeBonus(walletDto.getWelcomeBonus());
        wallet.setAvailableWelcomeBonus(walletDto.getAvailableWelcomeBonus());
        wallet.setReferralBonus(walletDto.getReferralBonus());
        wallet.setAvailableReferralBonus(walletDto.getAvailableReferralBonus());
        wallet.setTotalPrizeAmount(walletDto.getTotalPrizeAmount());
        wallet.setPendingWithdrawal(walletDto.getPendingWithdrawal());
        wallet.setTotalAvailableBalance(walletDto.getTotalAvailableBalance());
        wallet.setAvailableToWithdraw(walletDto.getAvailableToWithdraw());
        wallet.setLockedAmount(walletDto.getLockedAmount());
        wallet.setDepositBonus(walletDto.getDepositBonus());
        wallet.setPromotionalBonus(walletDto.getPromotionalBonus());
        wallet.setLastPaymentFrom(walletDto.getLastPaymentFrom());
        return wallet;
    }

    public static WalletWithUserProfileDto toWalletWithUserProfileDto(Wallet wallet, UserProfileMinimalDto userProfileDto) {
        return WalletWithUserProfileDto.builder()
                .id(wallet.getId())
                .agentId(wallet.getAgentId())
                .userProfileId(wallet.getUserProfileId())
                .welcomeBonus(wallet.getWelcomeBonus())
                .availableWelcomeBonus(wallet.getAvailableWelcomeBonus())
                .referralBonus(wallet.getReferralBonus())
                .availableReferralBonus(wallet.getAvailableReferralBonus())
                .totalPrizeAmount(wallet.getTotalPrizeAmount())
                .pendingWithdrawal(wallet.getPendingWithdrawal())
                .totalAvailableBalance(wallet.getTotalAvailableBalance())
                .availableToWithdraw(wallet.getAvailableToWithdraw())
                .lockedAmount(wallet.getLockedAmount())
                .depositBonus(wallet.getDepositBonus())
                .promotionalBonus(wallet.getPromotionalBonus())
                .lastPaymentFrom(wallet.getLastPaymentFrom())
                .userProfile(userProfileDto)
                .build();
    }
}
