package com.ebingo.backend.externalgame.service;

import com.ebingo.backend.externalgame.dto.WalletResult;
import com.ebingo.backend.payment.entity.Wallet;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.system.exceptions.InsufficientBalanceException;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalGameWalletService {

    private final WalletRepository walletRepository;
    private final TransactionalOperator transactionalOperator;

    /**
     * Debit funds for external game bet
     */
    public Mono<WalletResult> debitExternalGame(
            Long userId,
            BigDecimal amount,
            String currency,
            UUID providerTxnId,
            UUID gameId
    ) {
        log.info("External game debit: userId={}, amount={}, currency={}, txnId={}",
                userId, amount, currency, providerTxnId);

        return walletRepository.findByUserProfileId(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found for user: " + userId)))
                .flatMap(wallet -> processDebit(wallet, amount, currency))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(50))
                        .filter(throwable -> throwable instanceof OptimisticLockingFailureException))
                .as(transactionalOperator::transactional)
                .onErrorResume(InsufficientBalanceException.class, e ->
                        Mono.just(WalletResult.builder()
                                .success(false)
                                .errorCode("INSUFFICIENT_FUNDS")
                                .errorMessage(e.getMessage())
                                .build()))
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.just(WalletResult.builder()
                                .success(false)
                                .errorCode("ACCOUNT_INVALID")
                                .errorMessage(e.getMessage())
                                .build()))
                .onErrorResume(e -> {
                    log.error("Error during external game debit", e);
                    return Mono.just(WalletResult.builder()
                            .success(false)
                            .errorCode("UNKNOWN_ERROR")
                            .errorMessage("Internal error processing debit")
                            .build());
                });
    }

//    private Mono<WalletResult> processDebit(Wallet wallet, BigDecimal amount, String currency) {
//        BigDecimal roundedAmount = roundAmount(amount, currency);
//
//        if (wallet.getTotalAvailableBalance().compareTo(roundedAmount) < 0) {
//            return Mono.error(new InsufficientBalanceException("Insufficient balance"));
//        }
//
//        // Deduct from available balance
//        wallet.setTotalAvailableBalance(wallet.getTotalAvailableBalance().subtract(roundedAmount));
//
//        // Also deduct from availableToWithdraw if applicable
//        if (wallet.getAvailableToWithdraw().compareTo(roundedAmount) >= 0) {
//            wallet.setAvailableToWithdraw(wallet.getAvailableToWithdraw().subtract(roundedAmount));
//        } else {
//            wallet.setAvailableToWithdraw(BigDecimal.ZERO);
//        }
//
//        return walletRepository.save(wallet)
//                .map(savedWallet -> WalletResult.builder()
//                        .success(true)
//                        .balance(formatBalance(savedWallet.getTotalAvailableBalance(), currency))
//                        .balanceAmount(savedWallet.getTotalAvailableBalance())
//                        .build());
//    }

    private Mono<WalletResult> processDebit(Wallet wallet, BigDecimal amount, String currency) {
        BigDecimal roundedAmount = roundAmount(amount, currency);

        // 1️⃣ Check total balance first
        if (wallet.getTotalAvailableBalance().compareTo(roundedAmount) < 0) {
            return Mono.error(new InsufficientBalanceException("Insufficient balance"));
        }

        BigDecimal remaining = roundedAmount;

        BigDecimal usedFromWelcome = BigDecimal.ZERO;
        BigDecimal usedFromReferral = BigDecimal.ZERO;
        BigDecimal usedFromPromotional = BigDecimal.ZERO;
        BigDecimal usedFromLocked = BigDecimal.ZERO;
        BigDecimal usedFromDeposit = BigDecimal.ZERO;

        String lastPaymentFrom = "";

        // 2️⃣ Debit Welcome Bonus
        if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                wallet.getAvailableWelcomeBonus().compareTo(BigDecimal.ZERO) > 0) {

            usedFromWelcome = wallet.getAvailableWelcomeBonus().min(remaining);
            wallet.setAvailableWelcomeBonus(wallet.getAvailableWelcomeBonus().subtract(usedFromWelcome));
            wallet.setWelcomeBonus(wallet.getWelcomeBonus().subtract(usedFromWelcome));
            remaining = remaining.subtract(usedFromWelcome);
            lastPaymentFrom += "WELCOME_BONUS/" + usedFromWelcome.toPlainString();
        }

        // 3️⃣ Debit Referral Bonus
        if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                wallet.getAvailableReferralBonus().compareTo(BigDecimal.ZERO) > 0) {

            usedFromReferral = wallet.getAvailableReferralBonus().min(remaining);
            wallet.setAvailableReferralBonus(wallet.getAvailableReferralBonus().subtract(usedFromReferral));
            wallet.setReferralBonus(wallet.getReferralBonus().subtract(usedFromReferral));
            remaining = remaining.subtract(usedFromReferral);
            lastPaymentFrom += "*REFERRAL_BONUS/" + usedFromReferral.toPlainString();
        }

        // 4️⃣ Debit Promotional Bonus
        if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                wallet.getPromotionalBonus().compareTo(BigDecimal.ZERO) > 0) {

            usedFromPromotional = wallet.getPromotionalBonus().min(remaining);
            wallet.setPromotionalBonus(wallet.getPromotionalBonus().subtract(usedFromPromotional));
            remaining = remaining.subtract(usedFromPromotional);
            lastPaymentFrom += "*PROMOTIONAL_BONUS/" + usedFromPromotional.toPlainString();
        }

        // 5️⃣ Debit Locked Amount
        if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                wallet.getLockedAmount().compareTo(BigDecimal.ZERO) > 0) {

            usedFromLocked = wallet.getLockedAmount().min(remaining);
            wallet.setLockedAmount(wallet.getLockedAmount().subtract(usedFromLocked));
            remaining = remaining.subtract(usedFromLocked);
            lastPaymentFrom += "*LOCKED_AMOUNT/" + usedFromLocked.toPlainString();
        }

        // 6️⃣ Debit Deposit bonus
        if (remaining.compareTo(BigDecimal.ZERO) > 0 &&
                wallet.getDepositBonus().compareTo(BigDecimal.ZERO) > 0) {

            usedFromDeposit = wallet.getDepositBonus().min(remaining);
            wallet.setDepositBonus(wallet.getDepositBonus().subtract(usedFromDeposit));
            remaining = remaining.subtract(usedFromDeposit);
            lastPaymentFrom += "*DEPOSIT_BONUS/" + usedFromDeposit.toPlainString();
        }

        String paymentSources = lastPaymentFrom.startsWith("*")
                ? lastPaymentFrom.substring(1)
                : lastPaymentFrom;
        wallet.setLastPaymentFrom(paymentSources);

        // 7️⃣ Deduct full amount from totalAvailableBalance
        wallet.setTotalAvailableBalance(wallet.getTotalAvailableBalance().subtract(roundedAmount));

        // Update availableToWithdraw (recomputed from remaining components)
        BigDecimal availableToWithdraw = wallet.getTotalAvailableBalance()
                .subtract(wallet.getAvailableWelcomeBonus())
                .subtract(wallet.getAvailableReferralBonus())
                .subtract(wallet.getLockedAmount())
                .subtract(wallet.getDepositBonus())
                .subtract(wallet.getPromotionalBonus());

        // Guard against negative due to rounding/edge cases
        if (availableToWithdraw.compareTo(BigDecimal.ZERO) < 0) {
            availableToWithdraw = BigDecimal.ZERO;
        }
        wallet.setAvailableToWithdraw(availableToWithdraw);

        String finalPaymentSources = paymentSources;
        return walletRepository.save(wallet)
                .map(savedWallet -> WalletResult.builder()
                        .success(true)
                        .balance(formatBalance(savedWallet.getTotalAvailableBalance(), currency))
                        .balanceAmount(savedWallet.getTotalAvailableBalance())
                        .paymentSources(finalPaymentSources)
                        .build());
    }


    /**
     * Credit funds for external game win
     */
    public Mono<WalletResult> creditExternalGame(
            Long userId,
            BigDecimal resultAmount,
            String currency,
            UUID providerTxnId,
            UUID debitId,
            UUID gameId
    ) {
        log.info("External game credit: userId={}, result={}, currency={}, txnId={}",
                userId, resultAmount, currency, providerTxnId);

        return walletRepository.findByUserProfileId(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found for user: " + userId)))
                .flatMap(wallet -> processCredit(wallet, resultAmount, currency))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(50))
                        .filter(throwable -> throwable instanceof OptimisticLockingFailureException))
                .as(transactionalOperator::transactional)
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.just(WalletResult.builder()
                                .success(false)
                                .errorCode("ACCOUNT_INVALID")
                                .errorMessage(e.getMessage())
                                .build()))
                .onErrorResume(e -> {
                    log.error("Error during external game credit", e);
                    return Mono.just(WalletResult.builder()
                            .success(false)
                            .errorCode("UNKNOWN_ERROR")
                            .errorMessage("Internal error processing credit")
                            .build());
                });
    }

    private Mono<WalletResult> processCredit(Wallet wallet, BigDecimal resultAmount, String currency) {
        BigDecimal roundedAmount = roundAmount(resultAmount, currency);

        // Credit the result amount (already includes stake per doc)
        wallet.setTotalAvailableBalance(wallet.getTotalAvailableBalance().add(roundedAmount));
        wallet.setAvailableToWithdraw(wallet.getAvailableToWithdraw().add(roundedAmount));
        wallet.setTotalPrizeAmount(wallet.getTotalPrizeAmount().add(roundedAmount));

        return walletRepository.save(wallet)
                .map(savedWallet -> WalletResult.builder()
                        .success(true)
                        .balance(formatBalance(savedWallet.getTotalAvailableBalance(), currency))
                        .balanceAmount(savedWallet.getTotalAvailableBalance())
                        .build());
    }

    /**
     * Rollback/refund for external game
     */
    public Mono<WalletResult> rollbackExternalGame(
            Long userId,
            BigDecimal amount,
            String currency,
            UUID providerTxnId,
            UUID debitId,
            UUID gameId,
            String paymentSources
    ) {
        log.info("External game rollback: userId={}, amount={}, currency={}, txnId={}",
                userId, amount, currency, providerTxnId);

        return walletRepository.findByUserProfileId(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found for user: " + userId)))
                .flatMap(wallet -> processRollback(wallet, amount, currency, paymentSources))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(50))
                        .filter(throwable -> throwable instanceof OptimisticLockingFailureException))
                .as(transactionalOperator::transactional)
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.just(WalletResult.builder()
                                .success(false)
                                .errorCode("ACCOUNT_INVALID")
                                .errorMessage(e.getMessage())
                                .build()))
                .onErrorResume(e -> {
                    log.error("Error during external game rollback", e);
                    return Mono.just(WalletResult.builder()
                            .success(false)
                            .errorCode("UNKNOWN_ERROR")
                            .errorMessage("Internal error processing rollback")
                            .build());
                });
    }

//    private Mono<WalletResult> processRollback(Wallet wallet, BigDecimal amount, String currency) {
//        BigDecimal roundedAmount = roundAmount(amount, currency);
//
//        // Refund the amount
//        wallet.setTotalAvailableBalance(wallet.getTotalAvailableBalance().add(roundedAmount));
//        wallet.setAvailableToWithdraw(wallet.getAvailableToWithdraw().add(roundedAmount));
//
//        return walletRepository.save(wallet)
//                .map(savedWallet -> WalletResult.builder()
//                        .success(true)
//                        .balance(formatBalance(savedWallet.getTotalAvailableBalance(), currency))
//                        .balanceAmount(savedWallet.getTotalAvailableBalance())
//                        .build());
//    }


    private Mono<WalletResult> processRollback(Wallet wallet, BigDecimal amount, String currency, String paymentSources) {
        BigDecimal roundedAmount = roundAmount(amount, currency);

        // 1️⃣ Always restore total balance
        wallet.setTotalAvailableBalance(wallet.getTotalAvailableBalance().add(roundedAmount));

        // 2️⃣ Restore the same “buckets” used during the ORIGINAL debit.
        // The breakdown is stored per-transaction (payment_sources) because
        // wallet.lastPaymentFrom only reflects the most recent debit and would
        // restore the wrong buckets when debits/rollbacks interleave.
        BigDecimal explainedBySources = BigDecimal.ZERO;

        if (paymentSources != null && !paymentSources.isBlank()) {

            // Expected: WELCOME_BONUS/20.00*REFERRAL_BONUS/40.00*LOCKED_AMOUNT/10.50
            String[] sources = paymentSources.split("\\*");

            for (String entry : sources) {

                if (entry == null || entry.isBlank() || !entry.contains("/")) {
                    // skip malformed
                    continue;
                }

                String[] parts = entry.split("/");
                if (parts.length != 2) {
                    continue;
                }

                String sourceType = parts[0].trim();
                String amountStr = parts[1].trim();

                BigDecimal refundAmount;
                try {
                    refundAmount = new BigDecimal(amountStr);
                } catch (Exception ex) {
                    continue;
                }

                if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                explainedBySources = explainedBySources.add(refundAmount);

                switch (sourceType) {
                    case "WELCOME_BONUS":
                        wallet.setAvailableWelcomeBonus(wallet.getAvailableWelcomeBonus().add(refundAmount));
                        wallet.setWelcomeBonus(wallet.getWelcomeBonus().add(refundAmount));
                        break;

                    case "REFERRAL_BONUS":
                        wallet.setAvailableReferralBonus(wallet.getAvailableReferralBonus().add(refundAmount));
                        wallet.setReferralBonus(wallet.getReferralBonus().add(refundAmount));
                        break;

                    case "PROMOTIONAL_BONUS":
                        wallet.setPromotionalBonus(wallet.getPromotionalBonus().add(refundAmount));
                        break;

                    case "LOCKED_AMOUNT":
                        wallet.setLockedAmount(wallet.getLockedAmount().add(refundAmount));
                        break;

                    case "DEPOSIT_BONUS":
                        wallet.setDepositBonus(wallet.getDepositBonus().add(refundAmount));
                        break;

                    default:
                        // unknown bucket -> ignore (totalAvailableBalance already restored)
                        break;
                }
            }
        }

        // 3️⃣ Recompute availableToWithdraw using the same rule used after debit
        BigDecimal computedAvailableToWithdraw = wallet.getTotalAvailableBalance()
                .subtract(wallet.getAvailableWelcomeBonus())
                .subtract(wallet.getAvailableReferralBonus())
                .subtract(wallet.getLockedAmount())
                .subtract(wallet.getDepositBonus())
                .subtract(wallet.getPromotionalBonus());

        if (computedAvailableToWithdraw.compareTo(BigDecimal.ZERO) < 0) {
            computedAvailableToWithdraw = BigDecimal.ZERO;
        }
        wallet.setAvailableToWithdraw(computedAvailableToWithdraw);

        return walletRepository.save(wallet)
                .map(savedWallet -> WalletResult.builder()
                        .success(true)
                        .balance(formatBalance(savedWallet.getTotalAvailableBalance(), currency))
                        .balanceAmount(savedWallet.getTotalAvailableBalance())
                        .build());
    }


    /**
     * Round amount based on currency type
     * Fiat: 2 decimals, Crypto: 9 decimals
     */
    private BigDecimal roundAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        // Common crypto currencies
        boolean isCrypto = currency != null && (
                currency.equalsIgnoreCase("BTC") ||
                        currency.equalsIgnoreCase("ETH") ||
                        currency.equalsIgnoreCase("USDT") ||
                        currency.equalsIgnoreCase("USDC") ||
                        currency.equalsIgnoreCase("BNB") ||
                        currency.equalsIgnoreCase("SOL")
        );

        int scale = isCrypto ? 9 : 2;
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Format balance as string with proper decimals
     */
    public String formatBalance(BigDecimal balance, String currency) {
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        return roundAmount(balance, currency).toPlainString();
    }
}
