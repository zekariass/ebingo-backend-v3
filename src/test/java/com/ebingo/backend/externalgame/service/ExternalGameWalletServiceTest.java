//package com.ebingo.backend.externalgame.service;
//
//import com.ebingo.backend.externalgame.dto.WalletResult;
//import com.ebingo.backend.payment.entity.Wallet;
//import com.ebingo.backend.payment.repository.WalletRepository;
//import com.ebingo.backend.system.exceptions.InsufficientBalanceException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.OptimisticLockingFailureException;
//import org.springframework.transaction.reactive.TransactionalOperator;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ExternalGameWalletServiceTest {
//
//    @Mock
//    private WalletRepository walletRepository;
//
//    @Mock
//    private TransactionalOperator transactionalOperator;
//
//    private ExternalGameWalletService walletService;
//
//    @BeforeEach
//    void setUp() {
//        walletService = new ExternalGameWalletService(walletRepository, transactionalOperator);
//
//        // Mock transactional operator to pass through
//        when(transactionalOperator.transactional(any())).thenAnswer(invocation -> invocation.getArgument(0));
//    }
//
//    @Test
//    void testDebitExternalGame_Success() {
//        // Arrange
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.00");
//        String currency = "USD";
//        UUID txnId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("500.00"));
//        Wallet updatedWallet = createWallet(userId, new BigDecimal("400.00"));
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//        when(walletRepository.save(any(Wallet.class))).thenReturn(Mono.just(updatedWallet));
//
//        // Act & Assert
//        StepVerifier.create(walletService.debitExternalGame(userId, amount, currency, txnId, gameId))
//                .expectNextMatches(result ->
//                        result.isSuccess() &&
//                        result.getBalance().equals("400.00") &&
//                        result.getBalanceAmount().compareTo(new BigDecimal("400.00")) == 0
//                )
//                .verifyComplete();
//
//        verify(walletRepository).findByUserProfileId(userId);
//        verify(walletRepository).save(any(Wallet.class));
//    }
//
//    @Test
//    void testDebitExternalGame_InsufficientFunds() {
//        // Arrange
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("600.00");
//        String currency = "USD";
//        UUID txnId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("500.00"));
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//
//        // Act & Assert
//        StepVerifier.create(walletService.debitExternalGame(userId, amount, currency, txnId, gameId))
//                .expectNextMatches(result ->
//                        !result.isSuccess() &&
//                        "INSUFFICIENT_FUNDS".equals(result.getErrorCode())
//                )
//                .verifyComplete();
//
//        verify(walletRepository).findByUserProfileId(userId);
//        verify(walletRepository, never()).save(any(Wallet.class));
//    }
//
//    @Test
//    void testDebitExternalGame_OptimisticLockingRetry() {
//        // Arrange
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.00");
//        String currency = "USD";
//        UUID txnId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("500.00"));
//        Wallet updatedWallet = createWallet(userId, new BigDecimal("400.00"));
//
//        AtomicInteger attemptCount = new AtomicInteger(0);
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//        when(walletRepository.save(any(Wallet.class)))
//                .thenAnswer(invocation -> {
//                    if (attemptCount.incrementAndGet() < 2) {
//                        return Mono.error(new OptimisticLockingFailureException("Version conflict"));
//                    }
//                    return Mono.just(updatedWallet);
//                });
//
//        // Act & Assert
//        StepVerifier.create(walletService.debitExternalGame(userId, amount, currency, txnId, gameId))
//                .expectNextMatches(result -> result.isSuccess())
//                .verifyComplete();
//
//        verify(walletRepository, atLeast(2)).findByUserProfileId(userId);
//        verify(walletRepository, atLeast(2)).save(any(Wallet.class));
//    }
//
//    @Test
//    void testCreditExternalGame_Success() {
//        // Arrange
//        Long userId = 1L;
//        BigDecimal resultAmount = new BigDecimal("200.00");
//        String currency = "USD";
//        UUID txnId = UUID.randomUUID();
//        UUID debitId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("400.00"));
//        Wallet updatedWallet = createWallet(userId, new BigDecimal("600.00"));
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//        when(walletRepository.save(any(Wallet.class))).thenReturn(Mono.just(updatedWallet));
//
//        // Act & Assert
//        StepVerifier.create(walletService.creditExternalGame(userId, resultAmount, currency, txnId, debitId, gameId))
//                .expectNextMatches(result ->
//                        result.isSuccess() &&
//                        result.getBalance().equals("600.00")
//                )
//                .verifyComplete();
//
//        verify(walletRepository).findByUserProfileId(userId);
//        verify(walletRepository).save(any(Wallet.class));
//    }
//
//    @Test
//    void testRollbackExternalGame_Success() {
//        // Arrange
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.00");
//        String currency = "USD";
//        UUID txnId = UUID.randomUUID();
//        UUID debitId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("400.00"));
//        Wallet updatedWallet = createWallet(userId, new BigDecimal("500.00"));
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//        when(walletRepository.save(any(Wallet.class))).thenReturn(Mono.just(updatedWallet));
//
//        // Act & Assert
//        StepVerifier.create(walletService.rollbackExternalGame(userId, amount, currency, txnId, debitId, gameId))
//                .expectNextMatches(result ->
//                        result.isSuccess() &&
//                        result.getBalance().equals("500.00")
//                )
//                .verifyComplete();
//
//        verify(walletRepository).findByUserProfileId(userId);
//        verify(walletRepository).save(any(Wallet.class));
//    }
//
//    @Test
//    void testRoundingFiat() {
//        // Test fiat currency rounding (2 decimals)
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("100.12345");
//        String currency = "USD";
//        UUID txnId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("500.00"));
//        Wallet updatedWallet = createWallet(userId, new BigDecimal("399.88"));
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//        when(walletRepository.save(any(Wallet.class))).thenReturn(Mono.just(updatedWallet));
//
//        StepVerifier.create(walletService.debitExternalGame(userId, amount, currency, txnId, gameId))
//                .expectNextMatches(result -> result.isSuccess())
//                .verifyComplete();
//    }
//
//    @Test
//    void testRoundingCrypto() {
//        // Test crypto currency rounding (9 decimals)
//        Long userId = 1L;
//        BigDecimal amount = new BigDecimal("0.123456789123");
//        String currency = "BTC";
//        UUID txnId = UUID.randomUUID();
//        UUID gameId = UUID.randomUUID();
//
//        Wallet wallet = createWallet(userId, new BigDecimal("1.0"));
//        Wallet updatedWallet = createWallet(userId, new BigDecimal("0.876543211"));
//
//        when(walletRepository.findByUserProfileId(userId)).thenReturn(Mono.just(wallet));
//        when(walletRepository.save(any(Wallet.class))).thenReturn(Mono.just(updatedWallet));
//
//        StepVerifier.create(walletService.debitExternalGame(userId, amount, currency, txnId, gameId))
//                .expectNextMatches(result -> result.isSuccess())
//                .verifyComplete();
//    }
//
//    private Wallet createWallet(Long userId, BigDecimal balance) {
//        Wallet wallet = new Wallet();
//        wallet.setId(1L);
//        wallet.setUserProfileId(userId);
//        wallet.setTotalAvailableBalance(balance);
//        wallet.setAvailableToWithdraw(balance);
//        wallet.setTotalPrizeAmount(BigDecimal.ZERO);
//        wallet.setWelcomeBonus(BigDecimal.ZERO);
//        wallet.setAvailableWelcomeBonus(BigDecimal.ZERO);
//        wallet.setReferralBonus(BigDecimal.ZERO);
//        wallet.setAvailableReferralBonus(BigDecimal.ZERO);
//        wallet.setPendingWithdrawal(BigDecimal.ZERO);
//        wallet.setLockedAmount(BigDecimal.ZERO);
//        wallet.setDepositBonus(BigDecimal.ZERO);
//        wallet.setPromotionalBonus(BigDecimal.ZERO);
//        wallet.setVersion(0L);
//        return wallet;
//    }
//}
