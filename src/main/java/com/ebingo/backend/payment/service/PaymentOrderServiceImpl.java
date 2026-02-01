package com.ebingo.backend.payment.service;

import com.ebingo.backend.common.Util;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.service.DailyAgentAccountingService;
import com.ebingo.backend.common.service.DailyLeaderboardService;
import com.ebingo.backend.common.service.TotalAgentAccountingService;
import com.ebingo.backend.common.service.TotalLeaderboardService;
import com.ebingo.backend.payment.dto.*;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.entity.Wallet;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.payment.enums.WithdrawalMode;
import com.ebingo.backend.payment.mappers.PaymentMethodMapper;
import com.ebingo.backend.payment.mappers.PaymentOrderMapper;
import com.ebingo.backend.payment.mappers.WalletMapper;
import com.ebingo.backend.payment.repository.PaymentOrderRepository;
import com.ebingo.backend.payment.service.payment_strategy.PaymentStrategy;
import com.ebingo.backend.payment.service.payment_strategy.PaymentStrategyFactory;
import com.ebingo.backend.payment.service.withdrawal_strategy.WithdrawalStrategy;
import com.ebingo.backend.payment.service.withdrawal_strategy.WithdrawalStrategyFactory;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.mappers.UserProfileMapper;
import com.ebingo.backend.user.service.UserProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private final PaymentOrderRepository orderRepo;
    private final PaymentStrategyFactory strategyFactory;
    private final WithdrawalStrategyFactory withdrawalStrategyFactory;
    private final ObjectMapper mapper;
    private final WalletService walletService; // existing service
    private final PaymentMethodService paymentMethodService;
    private final UserProfileService userProfileService;
    private final TransactionService transactionService;
    private final TransactionalOperator transactionalOperator;
    private final AddisPayClientService addisPayClientService;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalOperator tx;
    private final DailyAgentAccountingService dailyAgentAccountingService;
    private final TotalAgentAccountingService totalAgentAccountingService;
    private final DailyLeaderboardService dailyLeaderboardService;
    private final TotalLeaderboardService totalLeaderboardService;


    @Override
    public Mono<PaymentOrderResponseDto> createOrder(PaymentOrderRequestDto dto) {

        String txnRef = generateTxnRef();

        PaymentOrder order = PaymentOrderMapper.toEntity(dto, txnRef);
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setNonce(UUID.randomUUID().toString());
        order.setAmount(dto.getAmount());
        order.setReason(Optional.ofNullable(dto.getReason()).orElse("Deposit"));
        order.setCurrency(Optional.ofNullable(dto.getCurrency()).orElse("ETB"));
        order.setPhoneNumber(Util.normalizePhoneNumber(dto.getPhoneNumber()));

        return userProfileService.getUserProfileByTelegramIdAndAgentId(
                        dto.getUserId(), dto.getAgentId()
                )
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(user -> paymentMethodService.getPaymentMethodById(dto.getPaymentMethodId())
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment method not found")))
                        .flatMap(paymentMethod ->
                                // Transactional save of the initial order
                                transactionalOperator.execute(status -> {
                                            order.setUserId(user.getId());
                                            order.setPaymentMethodId(paymentMethod.getId());

                                            if (order.getPhoneNumber() == null) {
                                                order.setPhoneNumber(Util.normalizePhoneNumber(user.getPhoneNumber()));
                                            }

                                            order.setInstructionsUrl(paymentMethod.getInstructionUrl());

                                            if (dto.getMetadata() != null) {
                                                try {
                                                    order.setMetaData(mapper.writeValueAsString(dto.getMetadata()));
                                                } catch (Exception ex) {
                                                    log.warn("Metadata serialization failed: {}", ex.getMessage());
                                                }
                                            }

                                            return orderRepo.save(order)
                                                    .onErrorResume(e -> {
                                                        status.setRollbackOnly();
                                                        return Mono.error(e);
                                                    });
                                        })
                                        .single()  // converts Flux<PaymentOrder> -> Mono<PaymentOrder>
                                        .flatMap(savedOrder -> {
                                            // Initiate payment provider outside transaction
                                            PaymentStrategy strategy = strategyFactory.getStrategy(
                                                    PaymentMethodMapper.toEntity(paymentMethod)
                                            );

                                            return strategy.initiateOrder(savedOrder, dto,
                                                            PaymentMethodMapper.toEntity(paymentMethod), user)
                                                    .flatMap(resp -> {
                                                        // Update order based on provider response
                                                        if (resp.getProviderUuid() != null) {
                                                            savedOrder.setProviderOrderRef(resp.getProviderUuid());
                                                            savedOrder.setStatus(PaymentOrderStatus.INITIATED);
                                                        } else if (resp.getInstructionsUrl() != null) {
                                                            savedOrder.setInstructionsUrl(resp.getInstructionsUrl());
                                                            savedOrder.setStatus(PaymentOrderStatus.AWAITING_APPROVAL);
                                                        }

                                                        return orderRepo.save(savedOrder)
                                                                .thenReturn(resp);
                                                    });
                                        })
                        )
                )
                .doOnError(ex -> log.error("Error creating payment order: {}", ex.getMessage(), ex));
    }

    private String generateTxnRef() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        int randomPart = ThreadLocalRandom.current().nextInt(100, 1000);
        return "PO-" + uuidPart + randomPart;
    }


    /**
     * Admin confirms offline order -> mark completed and credit wallet
     */
//    public Mono<Void> confirmOfflineOrder(Long orderId, Long adminTelegramId) {
//        return transactionalOperator.execute(status ->
//                userProfileService.getUserProfileByTelegramId(adminTelegramId)
//                        .switchIfEmpty(Mono.error(new UnauthorizedException("Admin not found")))
//                        .flatMap(adminUser -> {
//                            if (adminUser.getRole() != UserRole.ADMIN) {
//                                return Mono.error(new UnauthorizedException("Unauthorized: user is not an admin"));
//                            }
//
//                            return orderRepo.findById(orderId)
//                                    .switchIfEmpty(Mono.error(new RuntimeException("Order not found")))
//                                    .flatMap(order -> {
//                                        order.setStatus(PaymentOrderStatus.COMPLETED);
//                                        order.setApprovedBy(adminUser.getId());
//
//                                        return orderRepo.save(order)
//                                                .then(transactionService.createTransaction(order))
//                                                .then(walletService.credit(
//                                                        order.getUserId(),
//                                                        order.getAmount(),
//                                                        "Offline deposit",
//                                                        order.getTxnType(),
//                                                        null
//                                                ));
//                                    });
//                        })
//        ).then(); // Transaction completes when the Mono chain completes
//    }


    /**
     * Process Addispay callback — find order by provider ref and credit wallet if success.
     */

    @Override
    public Mono<Void> processAddisPayCallbackForSuccess(
            String sessionUuid,
            String paymentStatus,
            String totalAmount,
            String orderId,
            String nonce,
            String addisPayTransactionId,
            String thirdPartyTransactionRef) {

        log.info("Processing AddisPay callback for sessionUuid: {}, orderId: {}, paymentStatus: {}, totalAmount: {}, nonce: {}, addisPayTransactionId: {}, thirdPartyTransactionRef: {}", sessionUuid, orderId, paymentStatus, totalAmount, nonce, addisPayTransactionId, thirdPartyTransactionRef);

        if (StringUtils.isEmpty(sessionUuid)) {
            return Mono.error(new RuntimeException("Missing sessionUuid"));
        }

        return orderRepo.findByProviderOrderRef(sessionUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found for session: " + sessionUuid)))
                .flatMap(order -> {
                    log.info("Order found: {}", order.getId());

                    // Normalize status
                    String normalizedStatus = paymentStatus == null ? "" : paymentStatus.trim().toLowerCase();

                    Map<String, Object> metadata = Map.of(
                            "sessionUuid", sessionUuid,
                            "status", normalizedStatus,
                            "totalAmount", totalAmount != null ? totalAmount : "null",
                            "nonce", nonce != null ? nonce : "null",
                            "addisPayTransactionId", addisPayTransactionId != null ? addisPayTransactionId : "null",
                            "thirdPartyTransactionRef", thirdPartyTransactionRef != null ? thirdPartyTransactionRef : "null"
                    );

                    log.info("Processing Callback Metadata: {}", metadata);

                    ObjectMapper objectMapper = new ObjectMapper();
                    String metadataJson;
                    try {
                        metadataJson = objectMapper.writeValueAsString(metadata);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to serialize metadata", e));
                    }

                    order.setMetaData(metadataJson);

                    if (normalizedStatus.contains("success") || normalizedStatus.contains("completed")) {
                        order.setStatus(PaymentOrderStatus.COMPLETED);

                        return orderRepo.save(order)
                                .flatMap(savedOrder -> walletService.credit(
                                        savedOrder.getUserId(),
                                        savedOrder.getAmount(),
                                        "AddisPay deposit",
                                        savedOrder.getTxnType(),
                                        metadata
                                ));
                    } else {
                        order.setStatus(PaymentOrderStatus.FAILED);
                        return orderRepo.save(order).then();
                    }
                })
                .doOnSuccess(v -> log.info("Order processing completed for sessionUuid: {}", sessionUuid))
                .doOnError(e -> log.error("Error processing AddisPay success callback: {}", e.getMessage(), e))
                .then();
    }


    @Override
    public Mono<Void> processAddisPayCallbackForError(
            String sessionUuid,
            String paymentStatus,
            String totalAmount,
            String orderId,
            String nonce,
            String addisTransactionId,
            String thirdPartyTransactionRef) {

        log.info("Processing AddisPay ERROR callback for sessionUuid: {}, orderId: {}, paymentStatus: {}",
                sessionUuid, orderId, paymentStatus);

        if (StringUtils.isEmpty(sessionUuid)) {
            return Mono.error(new RuntimeException("Missing sessionUuid"));
        }

        return orderRepo.findByProviderOrderRef(sessionUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found for session: " + sessionUuid)))
                .flatMap(order -> {
                    log.info("Order found for ERROR callback: {}", order.getId());

                    String normalizedStatus = paymentStatus == null ? "" : paymentStatus.trim().toLowerCase();

                    Map<String, Object> metadata = Map.of(
                            "sessionUuid", sessionUuid,
                            "status", normalizedStatus,
                            "totalAmount", totalAmount != null ? totalAmount : "null",
                            "nonce", nonce != null ? nonce : "null",
                            "addisPayTransactionId", addisTransactionId != null ? addisTransactionId : "null",
                            "thirdPartyTransactionRef", thirdPartyTransactionRef != null ? thirdPartyTransactionRef : "null"
                    );

                    ObjectMapper objectMapper = new ObjectMapper();
                    String metadataJson;
                    try {
                        metadataJson = objectMapper.writeValueAsString(metadata);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to serialize metadata", e));
                    }

                    order.setMetaData(metadataJson);
                    order.setStatus(PaymentOrderStatus.FAILED);

                    return orderRepo.save(order);
                })
                .doOnSuccess(v -> log.info(" AddisPay ERROR callback processed for sessionUuid: {}", sessionUuid))
                .doOnError(e -> log.error("Error processing AddisPay error callback: {}", e.getMessage(), e))
                .then();
    }


    @Override
    public Mono<Void> processAddisPayCallbackCancel(
            String sessionUuid,
            String paymentStatus,
            String totalAmount,
            String orderId,
            String nonce,
            String addisTransactionId,
            String thirdPartyTransactionRef) {

        log.info("Processing AddisPay CANCEL callback for sessionUuid: {}, orderId: {}, paymentStatus: {}",
                sessionUuid, orderId, paymentStatus);

        if (StringUtils.isEmpty(sessionUuid)) {
            return Mono.error(new RuntimeException("Missing sessionUuid"));
        }

        return orderRepo.findByProviderOrderRef(sessionUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Order not found for session: " + sessionUuid)))
                .flatMap(order -> {
                    log.info("Order found for CANCEL callback: {}", order.getId());

                    String normalizedStatus = paymentStatus == null ? "" : paymentStatus.trim().toLowerCase();

                    Map<String, Object> metadata = Map.of(
                            "sessionUuid", sessionUuid,
                            "status", normalizedStatus,
                            "totalAmount", totalAmount != null ? totalAmount : "null",
                            "nonce", nonce != null ? nonce : "null",
                            "addisPayTransactionId", addisTransactionId != null ? addisTransactionId : "null",
                            "thirdPartyTransactionRef", thirdPartyTransactionRef != null ? thirdPartyTransactionRef : "null"
                    );

                    ObjectMapper objectMapper = new ObjectMapper();
                    String metadataJson;
                    try {
                        metadataJson = objectMapper.writeValueAsString(metadata);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to serialize metadata", e));
                    }

                    order.setMetaData(metadataJson);
                    order.setStatus(PaymentOrderStatus.CANCELLED);

                    return orderRepo.save(order);
                })
                .doOnSuccess(v -> log.info(" AddisPay CANCEL callback processed for sessionUuid: {}", sessionUuid))
                .doOnError(e -> log.error(" Error processing AddisPay cancel callback: {}", e.getMessage(), e))
                .then();
    }


    /**
     * Fetch all offline (manual) orders with pagination + sorting
     */
//    public Flux<PaymentOrderDto> getOfflineOrders(int page, int size, String sortBy) {
//        int pageNumber = Math.max(page, 1);
//        int pageSize = (size > 0 && size <= 100) ? size : 20;
//        long offset = (long) (pageNumber - 1) * pageSize;
//        String sortKey = normalizeSortKey(sortBy);
//
//        Flux<PaymentOrder> ordersFlux = switch (sortKey) {
//            case "amount" -> orderRepo.findOfflineOrdersOrderByAmountDesc(pageSize, offset);
//            case "status" -> orderRepo.findOfflineOrdersOrderByStatusDesc(pageSize, offset);
//            case "updatedat" -> orderRepo.findOfflineOrdersOrderByUpdatedAtDesc(pageSize, offset);
//            case "createdat" -> orderRepo.findOfflineOrdersOrderByCreatedAtDesc(pageSize, offset);
//            default -> orderRepo.findOfflineOrdersOrderByIdDesc(pageSize, offset);
//        };
//
//        return ordersFlux
//                .map(PaymentOrderMapper::toDto)
//                .doOnSubscribe(s -> log.info("Fetching offline orders, page={}, size={}, sortBy={}", page, size, sortBy))
//                .doOnError(e -> log.error("Failed to fetch offline orders: {}", e.getMessage(), e));
//    }

    /**
     * Fetch offline orders by status with pagination + sorting
     */
//    public Flux<PaymentOrderDto> getOfflineOrdersByStatus(String status, int page, int size, String sortBy) {
//        int pageNumber = Math.max(page, 1);
//        int pageSize = (size > 0 && size <= 100) ? size : 20;
//        long offset = (long) (pageNumber - 1) * pageSize;
//        String sortKey = normalizeSortKey(sortBy);
//
//        PaymentOrderStatus orderStatus;
//        try {
//            orderStatus = PaymentOrderStatus.valueOf(status.toUpperCase());
//        } catch (IllegalArgumentException e) {
//            return Flux.error(new IllegalArgumentException("Invalid payment order status: " + status));
//        }
//
//        Flux<PaymentOrder> ordersFlux = switch (sortKey) {
//            case "amount" -> orderRepo.findOfflineOrdersByStatusOrderByAmountDesc(orderStatus.name(), pageSize, offset);
//            case "updatedat" ->
//                    orderRepo.findOfflineOrdersByStatusOrderByUpdatedAtDesc(orderStatus.name(), pageSize, offset);
//            case "createdat" ->
//                    orderRepo.findOfflineOrdersByStatusOrderByCreatedAtDesc(orderStatus.name(), pageSize, offset);
//            default -> orderRepo.findOfflineOrdersByStatusOrderByIdDesc(orderStatus.name(), pageSize, offset);
//        };
//
//        return ordersFlux
//                .map(PaymentOrderMapper::toDto)
//                .doOnSubscribe(s -> log.info("Fetching offline orders by status={}, page={}, size={}, sortBy={}", status, page, size, sortBy))
//                .doOnError(e -> log.error("Failed to fetch offline orders by status {}: {}", status, e.getMessage(), e));
//    }

    /**
     * Fetch a user's pending orders with pagination + sorting
     */
//    public Flux<PaymentOrderDto> getUserPendingOrders(long userId, int page, int size, String sortBy) {
//        int pageNumber = Math.max(page, 1);
//        int pageSize = (size > 0 && size <= 100) ? size : 20;
//        long offset = (long) (pageNumber - 1) * pageSize;
//        String sortKey = normalizeSortKey(sortBy);
//
//        Flux<PaymentOrder> ordersFlux = switch (sortKey) {
//            case "amount" -> orderRepo.findPendingOrdersByUserIdOrderByAmountDesc(userId, pageSize, offset);
//            case "createdat" -> orderRepo.findPendingOrdersByUserIdOrderByCreatedAtDesc(userId, pageSize, offset);
//            default -> orderRepo.findPendingOrdersByUserIdOrderByIdDesc(userId, pageSize, offset);
//        };
//
//        return ordersFlux
//                .map(PaymentOrderMapper::toDto)
//                .doOnSubscribe(s -> log.info("Fetching pending orders for user={}, page={}, size={}, sortBy={}", userId, page, size, sortBy))
//                .doOnError(e -> log.error("Failed to fetch pending orders for user {}: {}", userId, e.getMessage(), e));
//    }
//
//    // --- Helpers ---
//    private String normalizeSortKey(String sortBy) {
//        return (sortBy != null) ? sortBy.toLowerCase() : "createdat";
//    }
    @Override
    public Mono<PaymentInitiateResponseDto> initiatePaymentOnline(PaymentInitiateRequestDto dto) {
        String phoneNumber = dto.getPhoneNumber();
        if (phoneNumber.contains("+")) {
            phoneNumber = phoneNumber.replace("+", "");
        }

        JsonNode payload = mapper.valueToTree(Map.of(
                "uuid", dto.getUuid(),
                "phone_number", phoneNumber,
                "encrypted_total_amount", dto.getEncryptedTotalAmount(),
                "merchant_name", dto.getMerchantName(),
                "selected_service", dto.getSelectedService(),
                "selected_bank", dto.getSelectedBank()
        ));

        log.info("AddisPay Payload: {}", payload.toPrettyString());

        return addisPayClientService.initiatePayment(payload)
                .flatMap(json -> {
                    log.info("AddisPay initiate-payment response: {}", json.toPrettyString());

                    PaymentInitiateResponseDto responseDto = PaymentInitiateResponseDto.builder()
                            .message(json.path("message").asText(null))
                            .data(json.path("data").isNull() ? "" : json.path("data").toString())
                            .details(json.path("details").asText(null))
                            .statusCode(json.path("status_code").asInt(0))
                            .build();

                    return Mono.just(responseDto);
                })
                .onErrorResume(e -> {
                    log.error("Error initiating AddisPay payment: {}", e.getMessage(), e);

                    // Try marking the order as failed before rethrowing
                    return orderRepo.findByProviderOrderRef(dto.getUuid())
                            .flatMap(order -> {
                                order.setStatus(PaymentOrderStatus.FAILED);
                                order.setMetaData("AddisPay error: " + e.getMessage());
                                return orderRepo.save(order)
                                        .doOnSuccess(o -> log.warn("Order {} marked FAILED due to AddisPay error", o.getId()));
                            })
                            .then(Mono.error(new RuntimeException("AddisPay initiate-payment failed: " + e.getMessage(), e)));
                });
    }


//    =========================================================================

//    @Override
//    public Mono<PaymentOrderDto> confirmWithdrawalByAdmin(WithdrawalApprovalRequestDto dto, Long adminUserId) {
//        log.info("Admin {} processing withdrawal order: {}", adminUserId, dto.getOrderId());
//
//        Mono<PaymentOrderDto> approvalFlow = orderRepo.findById(dto.getOrderId())
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment order not found")))
//                .flatMap(order -> {
//                    if (order.getTxnType() != TransactionType.WITHDRAWAL) {
//                        return Mono.error(new IllegalArgumentException("Order is not a withdrawal type"));
//                    }
//
//                    if (order.getStatus() != PaymentOrderStatus.AWAITING_APPROVAL && order.getStatus() != PaymentOrderStatus.PENDING) {
//                        return Mono.error(new IllegalStateException("Order is not awaiting approval"));
//                    }
//
//                    // If rejected
//                    if (!dto.isApprove()) {
//                        order.setStatus(PaymentOrderStatus.REJECTED);
//                        order.setReason(dto.getReason());
//                        order.setApprovedBy(adminUserId);
//
//                        return userProfileService.getUserProfileById(order.getUserId())
//                                .flatMap(profile ->
//                                        walletService.getWalletByUserProfileId(profile.getId())
//                                                .flatMap(walletDto -> {
//                                                    Wallet wallet = WalletMapper.toEntity(walletDto);
//                                                    // move back the funds
//                                                    wallet.setPendingWithdrawal(wallet.getPendingWithdrawal().subtract(order.getAmount()));
//                                                    wallet.setTotalAvailableBalance(wallet.getTotalAvailableBalance().add(order.getAmount()));
//                                                    wallet.setAvailableToWithdraw(wallet.getAvailableToWithdraw().add(order.getAmount()));
//                                                    return walletService.saveWallet(wallet)
//                                                            .then(orderRepo.save(order))
//                                                            .map(PaymentOrderMapper::toDto);
//                                                })
//                                );
//                    }
//
//                    // If approved
//                    order.setStatus(PaymentOrderStatus.COMPLETED);
//                    order.setApprovedBy(adminUserId);
//                    order.setUpdatedAt(Instant.now());
//                    order.setReason(dto.getReason());
//
//                    return userProfileService.getUserProfileById(order.getUserId())
//                            .flatMap(profile ->
//                                            walletService.getWalletByUserProfileId(profile.getId())
//                                                    .flatMap(walletDto -> {
//                                                        Wallet wallet = WalletMapper.toEntity(walletDto);
//                                                        // move from pendingWithdrawal → totalWithdrawal
//                                                        wallet.setPendingWithdrawal(wallet.getPendingWithdrawal().subtract(order.getAmount()));

    /// /                                                wallet.setTotalWithdrawal(wallet.getTotalWithdrawal().add(order.getAmount()));
//                                                        return walletService.saveWallet(wallet)
//                                                                .then(transactionService.createTransaction(order))
//                                                                .then(orderRepo.save(order))
//                                                                .map(PaymentOrderMapper::toDto);
//                                                    })
//                            );
//                })
//                .doOnSuccess(po -> log.info("Withdrawal {} successfully processed by admin {}", po.getTxnRef(), adminUserId))
//                .doOnError(e -> log.error("Withdrawal approval failed: {}", e.getMessage(), e));
//
//        return transactionalOperator.transactional(approvalFlow);
//    }
    @Override
    public Mono<PaymentOrderDto> confirmWithdrawalByAdmin(WithdrawalApprovalRequestDto dto, Long adminUserId) {
        log.info("Admin {} processing withdrawal order: {}", adminUserId, dto.getOrderId());

        Mono<PaymentOrderDto> approvalFlow = orderRepo.findById(dto.getOrderId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment order not found")))
                .flatMap(order -> {
                    BigDecimal amount = order.getAmount();

                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        return Mono.error(new IllegalStateException("Order amount must be greater than zero"));
                    }

                    if (order.getTxnType() != TransactionType.WITHDRAWAL) {
                        return Mono.error(new IllegalArgumentException("Order is not a withdrawal type"));
                    }

                    if (order.getStatus() != PaymentOrderStatus.AWAITING_APPROVAL
                            && order.getStatus() != PaymentOrderStatus.PENDING) {
                        return Mono.error(new IllegalStateException("Order is not awaiting approval"));
                    }

                    // Fetch user profile and wallet
                    return userProfileService.getUserProfileById(order.getUserId())
                            .flatMap(profile ->
                                    walletService.getWalletByUserProfileId(profile.getId())
                                            .flatMap(walletDto -> {
                                                Wallet wallet = WalletMapper.toEntity(walletDto);

                                                if (!dto.isApprove()) {
                                                    // ------------------ REJECTION ------------------
                                                    // Safely move back funds to available balances
                                                    wallet.setPendingWithdrawal(
                                                            wallet.getPendingWithdrawal().subtract(amount));
                                                    wallet.setTotalAvailableBalance(
                                                            wallet.getTotalAvailableBalance().add(amount));
                                                    wallet.setAvailableToWithdraw(
                                                            wallet.getAvailableToWithdraw().add(amount));

                                                    order.setStatus(PaymentOrderStatus.REJECTED);
                                                    order.setReason(dto.getReason());
                                                    order.setApprovedBy(adminUserId);
                                                    order.setUpdatedAt(Instant.now());

                                                    return walletService.saveWallet(wallet, dto.getAgentId())
                                                            .then(orderRepo.save(order))
                                                            .map(PaymentOrderMapper::toDto);
                                                }

                                                // ------------------ APPROVAL ------------------
                                                // Validate sufficient pending withdrawal
                                                if (wallet.getPendingWithdrawal().compareTo(amount) < 0) {
                                                    return Mono.error(new IllegalStateException(
                                                            "Insufficient pending withdrawal balance for approval"));
                                                }

                                                // Deduct pending withdrawal
                                                wallet.setPendingWithdrawal(
                                                        wallet.getPendingWithdrawal().subtract(amount));

                                                order.setStatus(PaymentOrderStatus.COMPLETED);
                                                order.setApprovedBy(adminUserId);
                                                order.setUpdatedAt(Instant.now());
                                                order.setReason(dto.getReason());


                                                return walletService.saveWallet(wallet, dto.getAgentId())
                                                        .then(orderRepo.save(order))
                                                        .map(PaymentOrderMapper::toDto)
                                                        .flatMap(savedDto ->
                                                                dailyAgentAccountingService.updateForWithdrawal(dto.getAgentId(), amount)
                                                                        .then(totalAgentAccountingService.updateForWithdrawal(dto.getAgentId(), amount))
                                                                        .then(dailyLeaderboardService.updateForWithdrawal(dto.getAgentId(), profile.getId(), amount, profile.getIsBot()))
                                                                        .then(totalLeaderboardService.updateForWithdrawal(dto.getAgentId(), profile.getId(), amount, profile.getIsBot()))
                                                                        .thenReturn(savedDto)
                                                        );

                                            })
                            );
                })
                .doOnSuccess(po -> log.info("Withdrawal {} successfully processed by admin {}", po.getTxnRef(), adminUserId))
                .doOnError(e -> log.error("Withdrawal approval failed: {}", e.getMessage(), e));

        return transactionalOperator.transactional(approvalFlow);
    }


    @Override
    public Mono<WithdrawalResponseDto> withdraw(WithdrawRequestDto withdrawRequestDto, Long telegramId) {
        log.info("Initiating withdrawal for user: {}, amount: {}", telegramId, withdrawRequestDto.getAmount());

        BigDecimal amount = withdrawRequestDto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Invalid withdrawal amount"));
        }

        // Step 0: fetch payment method
        return paymentMethodService.getPaymentMethodById(withdrawRequestDto.getPaymentMethodId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment method not found")))
                .flatMap(paymentMethodDto -> {
                    PaymentMethod paymentMethod = PaymentMethodMapper.toEntity(paymentMethodDto);

                    // Step 1: conditional password authentication
                    Mono<UserProfileDto> authMono;
                    if (Boolean.TRUE.equals(paymentMethod.getWithdrawalRequirePassword())) {
                        String password = withdrawRequestDto.getPassword();
                        if (password == null || password.isBlank()) {
                            return Mono.error(new IllegalArgumentException("Password is required for this withdrawal method"));
                        }

                        authMono = userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, withdrawRequestDto.getAgentId())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                                .flatMap(user -> {
                                    if (!passwordEncoder.matches(password, user.getPassword())) {
                                        return Mono.error(new IllegalArgumentException("Invalid password"));
                                    }
                                    return Mono.just(user);
                                });
                    } else {
                        authMono = userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, withdrawRequestDto.getAgentId())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")));
                    }

                    // Step 2: route to offline or online withdrawal
                    return authMono.flatMap(userProfileDto -> {
                        if (userProfileDto.getIsBot()) {
                            return Mono.error(new IllegalArgumentException("Bots are not allowed to perform withdrawals"));
                        }

                        log.info("Processing withdrawal for user: {}", withdrawRequestDto);

                        if (withdrawRequestDto.getWithdrawalMode() == WithdrawalMode.OFFLINE) {
                            log.info("Processing offline withdrawal for user: {}", userProfileDto.getTelegramId());
                            return processOfflineWithdrawal(userProfileDto, withdrawRequestDto, amount);
                        } else {
                            log.info("Processing online withdrawal for user: {}", userProfileDto.getTelegramId());
                            return processOnlineWithdrawal(userProfileDto, withdrawRequestDto, amount);
                        }
                    });
                });
    }

    // -------------------
// Offline withdrawal
// -------------------
    private Mono<WithdrawalResponseDto> processOfflineWithdrawal(UserProfileDto userProfileDto,
                                                                 WithdrawRequestDto withdrawRequestDto,
                                                                 BigDecimal amount) {

        return getOrCreateWallet(userProfileDto)
                .flatMap(walletDto -> {
                    Wallet wallet = WalletMapper.toEntity(walletDto);

                    if (wallet.getAvailableToWithdraw() == null || wallet.getAvailableToWithdraw().compareTo(amount) < 0) {
                        return Mono.error(new RuntimeException("Insufficient withdrawable balance"));
                    }

                    PaymentOrder order = buildPaymentOrder(userProfileDto, withdrawRequestDto, amount);
                    order.setStatus(PaymentOrderStatus.PENDING); // offline always pending

                    return transactionalOperator.transactional(
                            walletService.getWalletByUserProfileId(userProfileDto.getId())
                                    .switchIfEmpty(Mono.error(new RuntimeException("Wallet not found for user")))
                                    .flatMap(currWalletDto -> {
                                        Wallet currWallet = WalletMapper.toEntity(currWalletDto);

                                        // Re-check availability
                                        if (currWallet.getAvailableToWithdraw() == null || currWallet.getAvailableToWithdraw().compareTo(amount) < 0) {
                                            return Mono.error(new RuntimeException("Insufficient withdrawable balance (concurrent)"));
                                        }

                                        // Reserve funds in pendingWithdrawal
                                        currWallet.setAvailableToWithdraw(nonNullSubtract(currWallet.getAvailableToWithdraw(), amount));
                                        currWallet.setTotalAvailableBalance(nonNullSubtract(currWallet.getTotalAvailableBalance(), amount));
                                        currWallet.setPendingWithdrawal(nonNullAdd(currWallet.getPendingWithdrawal(), amount));


                                        return walletService.saveWallet(currWallet, withdrawRequestDto.getAgentId())
                                                .then(orderRepo.save(order))
                                                .map(savedOrder -> {
                                                    WithdrawalResponseDto response = new WithdrawalResponseDto();
                                                    response.setStatus(PaymentOrderStatus.PENDING);
                                                    response.setMessage("Withdrawal request is pending admin approval");
                                                    response.setData(savedOrder.getId().toString());
                                                    response.setWithdrawalMode(withdrawRequestDto.getWithdrawalMode());
                                                    return response;
                                                });
                                    })
                    );
                });
    }

    // -------------------
// Online withdrawal (current provider-based flow)
// -------------------
    private Mono<WithdrawalResponseDto> processOnlineWithdrawal(UserProfileDto userProfileDto,
                                                                WithdrawRequestDto withdrawRequestDto,
                                                                BigDecimal amount) {

        // Phase A: wallet reservation + order creation
        return getOrCreateWallet(userProfileDto)
                .flatMap(walletDto -> {
                    Wallet wallet = WalletMapper.toEntity(walletDto);

                    if (wallet.getAvailableToWithdraw() == null || wallet.getAvailableToWithdraw().compareTo(amount) < 0) {
                        return Mono.error(new RuntimeException("Insufficient withdrawable balance"));
                    }

                    PaymentOrder order = buildPaymentOrder(userProfileDto, withdrawRequestDto, amount);
                    order.setStatus(PaymentOrderStatus.INITIATED);

                    return transactionalOperator.transactional(
                            walletService.getWalletByUserProfileId(userProfileDto.getId())
                                    .switchIfEmpty(Mono.error(new RuntimeException("Wallet not found for user")))
                                    .flatMap(currWalletDto -> {
                                        Wallet currWallet = WalletMapper.toEntity(currWalletDto);

                                        if (currWallet.getAvailableToWithdraw() == null || currWallet.getAvailableToWithdraw().compareTo(amount) < 0) {
                                            return Mono.error(new RuntimeException("Insufficient withdrawable balance (concurrent)"));
                                        }

                                        currWallet.setAvailableToWithdraw(nonNullSubtract(currWallet.getAvailableToWithdraw(), amount));
                                        currWallet.setTotalAvailableBalance(nonNullSubtract(currWallet.getTotalAvailableBalance(), amount));

                                        return walletService.saveWallet(currWallet, withdrawRequestDto.getAgentId())
                                                .then(orderRepo.save(order))
                                                .map(savedOrder -> Tuples.of(savedOrder, userProfileDto));
                                    })
                    );
                })
                // Phase B: external provider processing
                .flatMap(tuple -> processProviderWithdrawal(tuple.getT1(), tuple.getT2(), withdrawRequestDto));
    }


    // --- helper method for Phase B ---
    private Mono<WithdrawalResponseDto> processProviderWithdrawal(PaymentOrder savedOrder, UserProfileDto user, WithdrawRequestDto withdrawRequestDto) {
        BigDecimal amount = withdrawRequestDto.getAmount();

        return paymentMethodService.getPaymentMethodById(withdrawRequestDto.getPaymentMethodId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment method not found")))
                .flatMap(paymentMethodDto -> {
                    PaymentMethod method = PaymentMethodMapper.toEntity(paymentMethodDto);
                    WithdrawalStrategy strategy = withdrawalStrategyFactory.getStrategy(withdrawRequestDto);

                    return strategy.initiateWithdrawal(savedOrder, method, withdrawRequestDto, user)
                            .flatMap(providerResp -> {
                                PaymentOrderStatus withdrawalStatus = providerResp.getStatus();

                                return transactionalOperator.transactional(
                                        walletService.getWalletByUserProfileId(user.getId())
                                                .switchIfEmpty(Mono.error(new RuntimeException("Wallet not found for user during provider processing")))
                                                .flatMap(walletDto -> {
                                                    Wallet wallet = WalletMapper.toEntity(walletDto);

                                                    switch (withdrawalStatus) {
                                                        case COMPLETED:
                                                            if (withdrawRequestDto.getWithdrawalMode() == WithdrawalMode.OFFLINE) {
                                                                wallet.setPendingWithdrawal(nonNullSubtract(wallet.getPendingWithdrawal(), amount));
                                                            }
                                                            savedOrder.setStatus(PaymentOrderStatus.COMPLETED);
                                                            break;
                                                        case PENDING:
                                                        case AWAITING_APPROVAL:
                                                            savedOrder.setStatus(withdrawalStatus == PaymentOrderStatus.AWAITING_APPROVAL
                                                                    ? PaymentOrderStatus.AWAITING_APPROVAL
                                                                    : PaymentOrderStatus.PENDING);
                                                            break;
                                                        case FAILED:
                                                            wallet.setAvailableToWithdraw(nonNullAdd(wallet.getAvailableToWithdraw(), amount));
                                                            wallet.setTotalAvailableBalance(nonNullAdd(wallet.getTotalAvailableBalance(), amount));
                                                            if (withdrawRequestDto.getWithdrawalMode() == WithdrawalMode.OFFLINE) {
                                                                wallet.setPendingWithdrawal(nonNullSubtract(wallet.getPendingWithdrawal(), amount));
                                                            }
                                                            savedOrder.setStatus(PaymentOrderStatus.FAILED);
                                                            break;
                                                        default:
                                                            savedOrder.setStatus(PaymentOrderStatus.PENDING);
                                                    }

                                                    // Store provider metadata safely
                                                    try {
                                                        Map<String, Object> metaFromProvider = mapper.convertValue(providerResp, Map.class);
                                                        savedOrder.setMetaData(mapper.writeValueAsString(metaFromProvider));
                                                    } catch (Exception e) {
                                                        log.warn("Failed to serialize provider metadata for order {}: {}", savedOrder.getId(), e.getMessage(), e);
                                                        savedOrder.setMetaData("{\"meta_error\":\"serialization_failed\"}");
                                                    }

                                                    return walletService.saveWallet(wallet, withdrawRequestDto.getAgentId())
                                                            .then(orderRepo.save(savedOrder))
                                                            .map(updatedOrder -> providerResp);
                                                })
                                );
                            })
                            .onErrorResume(ex -> {
                                log.error("Provider call failed for order {}: {}", savedOrder.getId(), ex.getMessage(), ex);
                                savedOrder.setStatus(PaymentOrderStatus.FAILED);
                                savedOrder.setReason(ex.getMessage());

                                return transactionalOperator.transactional(
                                        walletService.getWalletByUserProfileId(user.getId())
                                                .switchIfEmpty(Mono.error(new RuntimeException("Wallet not found for user during error compensation")))
                                                .flatMap(walletDto -> {
                                                    Wallet wallet = WalletMapper.toEntity(walletDto);
                                                    wallet.setAvailableToWithdraw(nonNullAdd(wallet.getAvailableToWithdraw(), amount));
                                                    wallet.setTotalAvailableBalance(nonNullAdd(wallet.getTotalAvailableBalance(), amount));
                                                    if (withdrawRequestDto.getWithdrawalMode() == WithdrawalMode.OFFLINE) {
                                                        wallet.setPendingWithdrawal(nonNullSubtract(wallet.getPendingWithdrawal(), amount));
                                                    }
                                                    return walletService.saveWallet(wallet, withdrawRequestDto.getAgentId())
                                                            .then(orderRepo.save(savedOrder));
                                                })
                                ).then(Mono.error(ex));
                            });
                });
    }





    /* ---------- Helper methods used above ---------- */

    /**
     * Create or return existing wallet DTO for the user.
     */
    private Mono<WalletDto> getOrCreateWallet(UserProfileDto userProfileDto) {
        return walletService.getWalletByUserProfileId(userProfileDto.getId())
                .switchIfEmpty(
                        walletService.createWallet(UserProfileMapper.toEntity(userProfileDto), false)
                                .doOnNext(w -> log.info("Created wallet for user {}", userProfileDto.getId()))
                );
    }

    /**
     * Build PaymentOrder and serialize initial metadata. Throws RuntimeException if serialization fails.
     */
    private PaymentOrder buildPaymentOrder(UserProfileDto userProfileDto, WithdrawRequestDto request, BigDecimal amount) {
        PaymentOrder order = new PaymentOrder();
        order.setUserId(userProfileDto.getId());
        order.generateAndSetTxnRef();
        order.setPhoneNumber(Util.normalizePhoneNumber(request.getPhoneNumber()));
        order.setAmount(amount);
        order.setCurrency(request.getCurrency());
        order.setStatus(PaymentOrderStatus.PENDING);
        order.setReason("Withdrawal request");
        order.setPaymentMethodId(request.getPaymentMethodId());
        order.setTxnType(TransactionType.WITHDRAWAL);
        order.setNonce(UUID.randomUUID().toString());
        order.setAgentId(request.getAgentId());

        Map<String, String> metaData = new HashMap<>();
        if (request.getBankName() != null) metaData.put("bankName", request.getBankName());
        if (request.getAccountName() != null) metaData.put("accountName", request.getAccountName());
        if (request.getAccountNumber() != null) metaData.put("accountNumber", request.getAccountNumber());
        if (request.getPhoneNumber() != null)
            metaData.put("phoneNumber", Util.normalizePhoneNumber(request.getPhoneNumber()));

        try {
            order.setMetaData(mapper.writeValueAsString(metaData));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize withdrawal metadata", e);
        }

        return order;
    }


    /**
     * Utility: add with null-safety
     */
    private BigDecimal nonNullAdd(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b);
    }

    /**
     * Utility: subtract with null-safety
     */
    private BigDecimal nonNullSubtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        BigDecimal result = a.subtract(b);

//        return result.max(BigDecimal.ZERO);
        return result;
    }


    @Override
    public Mono<PageResponse<PaymentOrderDto>> getAdminWithdrawals(String status, int page, int size) {
        int offset = (page - 1) * size;

        Flux<PaymentOrder> withdrawalsFlux;
        Mono<Long> countMono;

        if (status != null && !status.isEmpty()) {
            withdrawalsFlux = orderRepo.findAllWithdrawalsByStatus(status, size, offset);
            countMono = orderRepo.countAllWithdrawalsByStatus(status);
        } else {
            withdrawalsFlux = orderRepo.findAllWithdrawals(size, offset);
            countMono = orderRepo.countAllWithdrawals();
        }

        return withdrawalsFlux
                .map(PaymentOrderMapper::toDto)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageResponse<>(
                        tuple.getT1(),
                        page,
                        size,
                        tuple.getT2()
                ));
    }


    @Override
    public Mono<PageResponse<PaymentOrderDto>> getUserWithdrawals(Long userId, String status, int page, int size) {
        int offset = (page - 1) * size;

        Flux<PaymentOrder> withdrawalsFlux;
        Mono<Long> countMono;

        if (status != null && !status.isEmpty()) {
            withdrawalsFlux = orderRepo.findWithdrawalsByUserAndStatus(userId, status, size, offset);
            countMono = orderRepo.countUserWithdrawalsByStatus(userId, status);
        } else {
            withdrawalsFlux = orderRepo.findWithdrawalsByUser(userId, size, offset);
            countMono = orderRepo.countUserWithdrawals(userId);
        }

        return withdrawalsFlux
                .map(PaymentOrderMapper::toDto)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageResponse<>(
                        tuple.getT1(),
                        page,
                        size,
                        tuple.getT2()
                ));
    }


    @Override
    public Mono<PageResponse<PaymentOrderDto>> getAdminDeposits(String status, int page, int size) {
        int offset = (page - 1) * size;

        Flux<PaymentOrder> depositsFlux;
        Mono<Long> countMono;

        if (status != null && !status.isEmpty()) {
            depositsFlux = orderRepo.findAllDepositsByStatus(status, size, offset);
            countMono = orderRepo.countAllDepositsByStatus(status);
        } else {
            depositsFlux = orderRepo.findAllDeposits(size, offset);
            countMono = orderRepo.countAllDeposits();
        }

        return depositsFlux
                .map(PaymentOrderMapper::toDto)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageResponse<>(
                        tuple.getT1(),
                        page,
                        size,
                        tuple.getT2()
                ));
    }

    @Override
    public Mono<PageResponse<PaymentOrderDto>> getUserDeposits(Long userId, String status, int page, int size) {
        int offset = (page - 1) * size;

        Flux<PaymentOrder> depositsFlux;
        Mono<Long> countMono;

        if (status != null && !status.isEmpty()) {
            depositsFlux = orderRepo.findDepositsByUserAndStatus(userId, status, size, offset);
            countMono = orderRepo.countUserDepositsByStatus(userId, status);
        } else {
            depositsFlux = orderRepo.findDepositsByUser(userId, size, offset);
            countMono = orderRepo.countUserDeposits(userId);
        }

        return depositsFlux
                .map(PaymentOrderMapper::toDto)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageResponse<>(
                        tuple.getT1(),
                        page,
                        size,
                        tuple.getT2()
                ));
    }

    @Override
    public Mono<PageResponse<PaymentOrderListDto>> getPaymentOrdersForAdmin(PaymentOrderGetParamsDto params) {

        int page = params.getPage();
        int size = params.getSize();
        int offset = (page - 1) * size;

        Flux<PaymentOrder> ordersFlux;
        Mono<Long> countMono;

        // If searching by user's phone number
        if (params.getPhoneNumber() != null && !params.getPhoneNumber().isEmpty() && params.getAgentId() == null) {

            log.info("Searching by phone number: {}", params.getPhoneNumber());
            ordersFlux = orderRepo.findByUserPhoneAndAgentIdOrderedDesc(
                    params.getPhoneNumber(),
                    params.getAgentId(),
                    params.getStatus(),
                    params.getTxnType(),
                    size,
                    offset
            );

            countMono = orderRepo.countByUserPhoneAndAgentId(
                    params.getPhoneNumber(),
                    params.getAgentId(),
                    params.getStatus(),
                    params.getTxnType()
            );

        } else {  // All payment orders

            log.info("Fetching all payment orders");
            ordersFlux = orderRepo.findAllWithFiltersOrderedDesc(
                    params.getAgentId(),
                    params.getStatus(),
                    params.getTxnType(),
                    size,
                    offset
            );

            countMono = orderRepo.countAllWithFilters(
                    params.getAgentId(),
                    params.getStatus(),
                    params.getTxnType()
            );
        }

        return ordersFlux
                .map(PaymentOrderMapper::toListDto)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageResponse<>(
                        tuple.getT1(),
                        page,
                        size,
                        tuple.getT2()
                ));
    }


//    @Override
//    public Mono<PaymentOrderDetailDto> getPaymentOrderDetail(Long poId) {
//        log.info("Fetching payment order with id: {}", poId);
//        return orderRepo.findById(poId)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment Order not found")))
//                .flatMap(po ->
//                        userProfileService.getUserProfileById(po.getUserId())
//                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
//                                .flatMap(user -> {
//                                            log.info("Fetching wallet for user: {}, of payment order: {}", user.getId(), po.getId());
//                                            return walletService.getWalletByUserProfileId(user.getId())
//                                                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found")))
//                                                    .map(wallet -> {
//                                                        // Get PaymentMethod and set payment method as well
//                                                        return PaymentOrderMapper.toDetailDto(po, user, wallet)
//                                                    });
//                                        }
//
//                                )
//                );
//    }


    @Override
    public Mono<PaymentOrderDetailDto> getPaymentOrderDetail(Long poId) {
        log.info("Fetching payment order with id: {}", poId);

        return orderRepo.findById(poId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment Order not found")))
                .flatMap(po ->
                        userProfileService.getUserProfileById(po.getUserId())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                                .flatMap(user ->
                                        walletService.getWalletByUserProfileId(user.getId())
                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found")))
                                                .flatMap(wallet ->
                                                        paymentMethodService.getPaymentMethodById(po.getPaymentMethodId())
                                                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment Method not found")))
                                                                .map(paymentMethod ->
                                                                        PaymentOrderMapper.toDetailDto(po, user, wallet, PaymentMethodMapper.toMinimalDto(paymentMethod))
                                                                )
                                                )
                                )
                );
    }


//    @Override
//    public Mono<PaymentOrderDto> offlineDeposit(OfflineDepositRequestDto dto) {
//
//        // Get payment method by code
//        Mono<PaymentMethodDto> paymentMethodMono = paymentMethodService.getPaymentMethodByCode(dto.getPaymentMethodCode())
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                        "Payment method not found for code: " + dto.getPaymentMethodCode()
//                )));
//
//        // Step 0: Check duplicate by provider reference
//        Mono<PaymentOrder> existingOrderMono =
//                orderRepo.findByProviderOrderRefAndStatus(dto.getPaymentProviderRef(), PaymentOrderStatus.COMPLETED)
//                        .flatMap(existing -> Mono.error(
//                                new IllegalStateException("Payment already completed: " + dto.getPaymentProviderRef())
//                        ));
//
//        // Step 1: Fetch user profile
//        Mono<UserProfileDto> userProfileMono =
//                userProfileService.getUserProfileByTelegramId(dto.getTelegramId())
//                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                                "User profile not found for telegram ID: " + dto.getTelegramId()
//                        )));
//
//        return existingOrderMono  // ensures no duplicates first
//                .then(userProfileMono)
//                .flatMap(userProfile -> {
//
//                    // Step 2: Build payment order
//                    PaymentOrder paymentOrder = new PaymentOrder();
//                    paymentOrder.setUserId(userProfile.getId());
//                    paymentOrder.setAmount(dto.getAmount());
//                    paymentOrder.setCurrency("ETB");
//                    paymentOrder.generateAndSetTxnRef();
//                    paymentOrder.setNonce(UUID.randomUUID().toString());
//                    paymentOrder.setProviderOrderRef(dto.getPaymentProviderRef());
//                    paymentOrder.setPaymentMethodId(dto.getPaymentMethodId());
//                    paymentOrder.setInstructionsUrl(dto.getInstructionsUrl());
//                    paymentOrder.setTxnType(TransactionType.DEPOSIT);
//                    paymentOrder.setStatus(PaymentOrderStatus.COMPLETED);
//                    paymentOrder.setReason("Offline deposit added");
//
//                    // metadata JSON encode
//                    try {
//                        String metadataJson = objectMapper.writeValueAsString(dto.getMetadata());
//                        paymentOrder.setMetaData(metadataJson);
//                    } catch (JsonProcessingException e) {
//                        return Mono.error(new RuntimeException("Failed to serialize metadata", e));
//                    }
//
//                    log.info("Creating offline deposit for user {} amount {}", userProfile.getId(), dto.getAmount());
//
//                    return orderRepo.save(paymentOrder)
//                            .flatMap(savedOrder ->
//                                    // Step 3: Credit wallet
//                                    walletService.credit(
//                                            userProfile.getId(),
//                                            dto.getAmount(),
//                                            "Offline deposit: " + savedOrder.getTxnRef(),
//                                            TransactionType.DEPOSIT,
//                                            Map.of(
//                                                    "paymentOrderId", savedOrder.getId(),
//                                                    "telegramId", dto.getTelegramId(),
//                                                    "providerRef", dto.getPaymentProviderRef()
//                                            )
//                                    ).thenReturn(savedOrder)
//                            )
//                            // Step 4: Map to DTO
//                            .map(PaymentOrderMapper::toDto);
//                });
//    }

//
//    @Override
//    public Mono<PaymentOrderDto> offlineDeposit(OfflineDepositRequestDto dto) {
//
//        log.info("Txn ref: {}", dto.getPaymentProviderRef());
//        String paymentProviderRef =
//                (dto.getPaymentProviderRef() == null || dto.getPaymentProviderRef().isBlank())
//                        ? generateManualProviderRef()
//                        : dto.getPaymentProviderRef().strip();
//
//        Mono<PaymentMethodDto> paymentMethodMono =
//                paymentMethodService.getPaymentMethodByCode(dto.getPaymentMethodCode())
//                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                                "Payment method not found for code: " + dto.getPaymentMethodCode()
//                        )));
//
//        Mono<Void> existingOrderMono =
//                orderRepo.findByProviderOrderRefAndStatusAndAgentId(
//                                paymentProviderRef,
//                                PaymentOrderStatus.COMPLETED,
//                                dto.getAgentId()
//                        )
//                        .flatMap(existing -> Mono.error(new RuntimeException(
//                                "Payment already completed: " + paymentProviderRef
//                        )))
//                        .then();
//
//        Mono<UserProfileDto> userProfileMono =
//                userProfileService.getUserProfileByTelegramIdAndAgentId(dto.getTelegramId(), dto.getAgentId())
//                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                                "User profile not found for telegram ID: " + dto.getTelegramId()
//                        )));
//
//        Mono<Void> updateDailyAgentAccountingMono = dailyAgentAccountingService
//                .updateForDeposit(dto.getAgentId(), dto.getAmount());
//
//        Mono<Void> updateTotalAgentAccountingMono = totalAgentAccountingService
//                .updateForDeposit(dto.getAgentId(), dto.getAmount());
//
//        Mono<PaymentOrderDto> flow =
//                existingOrderMono
//                        .then(Mono.zip(userProfileMono, paymentMethodMono, updateDailyAgentAccountingMono, updateTotalAgentAccountingMono))
//                        .flatMap(tuple -> {
//
//                            UserProfileDto user = tuple.getT1();
//                            PaymentMethodDto method = tuple.getT2();
//
//                            PaymentOrder order = new PaymentOrder();
//                            order.setUserId(user.getId());
//                            order.setAgentId(dto.getAgentId());
//                            order.setAmount(dto.getAmount());
//                            order.setCurrency("ETB");
//                            order.generateAndSetTxnRef();
//                            order.setNonce(UUID.randomUUID().toString());
//                            order.setProviderOrderRef(paymentProviderRef);
//                            order.setPaymentMethodId(method.getId());
//                            order.setInstructionsUrl(dto.getInstructionsUrl());
//                            order.setTxnType(TransactionType.DEPOSIT);
//                            order.setStatus(PaymentOrderStatus.COMPLETED);
//                            order.setReason("Offline deposit added");
//
//                            try {
//                                order.setMetaData(objectMapper.writeValueAsString(dto.getMetadata()));
//                            } catch (JsonProcessingException e) {
//                                return Mono.error(new RuntimeException("Failed to serialize metadata", e));
//                            }
//
//                            return orderRepo.save(order)
//                                    .flatMap(savedOrder ->
//                                            walletService.credit(
//                                                    user.getId(),
//                                                    dto.getAmount(),
//                                                    "Offline deposit: " + savedOrder.getTxnRef(),
//                                                    TransactionType.DEPOSIT,
//                                                    Map.of(
//                                                            "paymentOrderId", savedOrder.getId(),
//                                                            "telegramId", dto.getTelegramId(),
//                                                            "providerRef", paymentProviderRef
//                                                    )
//                                            ).thenReturn(savedOrder)
//                                    )
//                                    .map(PaymentOrderMapper::toDto);
//                        });
//
//        // ✅ Make the whole flow transactional
//        return flow.as(tx::transactional);
//    }
//


    @Override
    public Mono<PaymentOrderDto> offlineDeposit(OfflineDepositRequestDto dto) {

        final String paymentProviderRef =
                (dto.getPaymentProviderRef() == null || dto.getPaymentProviderRef().isBlank())
                        ? generateManualProviderRef()
                        : dto.getPaymentProviderRef().strip();

        Mono<Void> existingOrderMono =
                orderRepo.findByProviderOrderRefAndStatusAndAgentId(
                                paymentProviderRef,
                                PaymentOrderStatus.COMPLETED,
                                dto.getAgentId()
                        )
                        .flatMap(existing -> Mono.error(new RuntimeException(
                                "Payment already completed: " + paymentProviderRef
                        )))
                        .then();

        Mono<UserProfileDto> userProfileMono =
                userProfileService.getUserProfileByTelegramIdAndAgentId(dto.getTelegramId(), dto.getAgentId())
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "User profile not found for telegram ID: " + dto.getTelegramId()
                        )));

        Mono<PaymentMethodDto> paymentMethodMono =
                paymentMethodService.getPaymentMethodByCode(dto.getPaymentMethodCode())
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "Payment method not found for code: " + dto.getPaymentMethodCode()
                        )));

        Mono<PaymentOrderDto> flow =
                existingOrderMono
                        .then(Mono.zip(userProfileMono, paymentMethodMono))
                        .flatMap(tuple -> {
                            UserProfileDto user = tuple.getT1();
                            PaymentMethodDto method = tuple.getT2();

                            PaymentOrder order = new PaymentOrder();
                            order.setUserId(user.getId());
                            order.setAgentId(dto.getAgentId());
                            order.setAmount(dto.getAmount());
                            order.setCurrency("ETB");
                            order.generateAndSetTxnRef();
                            order.setNonce(UUID.randomUUID().toString());
                            order.setProviderOrderRef(paymentProviderRef);
                            order.setPaymentMethodId(method.getId());
                            order.setInstructionsUrl(dto.getInstructionsUrl());
                            order.setTxnType(TransactionType.DEPOSIT);
                            order.setStatus(PaymentOrderStatus.COMPLETED);
                            order.setReason("Offline deposit added");

                            try {
                                order.setMetaData(objectMapper.writeValueAsString(dto.getMetadata()));
                            } catch (JsonProcessingException e) {
                                return Mono.error(new RuntimeException("Failed to serialize metadata", e));
                            }

                            return orderRepo.save(order)
                                    .flatMap(savedOrder ->
                                            walletService.credit(
                                                    user.getId(),
                                                    dto.getAmount(),
                                                    "Offline deposit: " + savedOrder.getTxnRef(),
                                                    TransactionType.DEPOSIT,
                                                    Map.of(
                                                            "paymentOrderId", savedOrder.getId(),
                                                            "telegramId", dto.getTelegramId(),
                                                            "providerRef", paymentProviderRef
                                                    )
                                            ).thenReturn(savedOrder)
                                    )
//                                    .flatMap(savedOrder ->
//                                            dailyAgentAccountingService.updateForDeposit(dto.getAgentId(), dto.getAmount())
//                                                    .then(
//                                                            totalAgentAccountingService.updateForDeposit(dto.getAgentId(), dto.getAmount()))
//                                                    .thenReturn(savedOrder)
//                                    )
                                    .map(PaymentOrderMapper::toDto);
                        });

        return flow.as(tx::transactional);
    }


    private String generateManualProviderRef() {
        return "depo-" + UUID.randomUUID().toString().substring(0, 10);
    }


    @Override
    public Mono<PaymentOrderDto> checkDepositStatusByProviderTxnRef(String providerTxnRef) {
        return orderRepo.findByProviderOrderRef(providerTxnRef)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment order not found for provider txn ref: " + providerTxnRef)))
                .map(PaymentOrderMapper::toDto);
    }

//    @Override
//    public Mono<PaymentOrderDto> createPromotionalDiscountDeposit(PromotionalDiscountDepositRequestDto dto, long adminUserId) {
//
//        // Step 1: Check if admin user exists and is admin
//
//        // Step 2: Get user profile by telegram ID
//
//        // Step 3: Create payment order with COMPLETED status
//
//        // Step 4: Credit wallet
//
//        return null;
//    }


//    @Override
//    public Mono<PaymentOrderDto> createPromotionalDiscountDeposit(PromotionalDiscountDepositRequestDto dto, long adminUserId) {
//
//        // Step 1: Validate Admin User by userProfileId
//        Mono<UserProfileDto> adminCheckMono = userProfileService.getUserProfileByTelegramId(adminUserId)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Admin user not found: " + adminUserId)))
//                .flatMap(admin -> {
//                    if (!UserRole.ADMIN.equals(admin.getRole())) {
//                        return Mono.error(new IllegalAccessException("User is not authorized to perform this action"));
//                    }
//                    return Mono.just(admin);
//                });
//
//        // Step 2: Fetch target user by Telegram ID
//        Mono<UserProfileDto> userProfileMono = userProfileService
//                .getUserProfileByTelegramId(dto.getUserTelegramId())
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                        "User profile not found for telegram ID: " + dto.getUserTelegramId()
//                )));
//
//        // Step 3: Fetch internal payment method (required)
//        Mono<PaymentMethodDto> paymentMethodMono = paymentMethodService
//                .getPaymentMethodByCode("internal")
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
//                        "Payment method not found for code 'internal'"
//                )));
//
//        return adminCheckMono
//                .then(Mono.zip(userProfileMono, paymentMethodMono))
//                .flatMap(tuple -> {
//
//                    UserProfileDto userProfile = tuple.getT1();
//                    PaymentMethodDto paymentMethod = tuple.getT2();
//
//                    // Step 4: Create PaymentOrder
//                    PaymentOrder order = new PaymentOrder();
//                    order.setUserId(userProfile.getId());
//                    order.setAmount(dto.getAmount());
//                    order.setCurrency("ETB");
//                    order.generateAndSetTxnRef();
//                    order.setProviderOrderRef(null);
//                    order.setPaymentMethodId(paymentMethod.getId());
//                    order.setInstructionsUrl(null);
//                    order.setTxnType(TransactionType.PROMOTIONAL_BONUS);
//                    order.setNonce(UUID.randomUUID().toString());
//                    order.setApprovedBy(adminUserId);
//                    order.setReason("Promotional discount deposit");
//                    order.setStatus(PaymentOrderStatus.COMPLETED);
//
//                    log.info("Creating promotional discount deposit for user {} | amount {} | admin {}",
//                            userProfile.getId(), dto.getAmount(), adminUserId);
//
//                    return orderRepo.save(order)
//                            .flatMap(savedOrder ->
//
//                                    // Step 5: Credit wallet with PROMOTIONAL_BONUS type
//                                    walletService.credit(
//                                            userProfile.getId(),
//                                            dto.getAmount(),
//                                            "Promotional bonus deposit: " + savedOrder.getTxnRef(),
//                                            TransactionType.PROMOTIONAL_BONUS,
//                                            Map.of(
//                                                    "paymentOrderId", savedOrder.getId(),
//                                                    "userTelegramId", dto.getUserTelegramId(),
//                                                    "adminUserId", adminUserId
//                                            )
//                                    ).thenReturn(savedOrder)
//                            )
//                            .map(PaymentOrderMapper::toDto);
//                });
//    }


    @Override
    public Mono<PaymentOrderDto> createPromotionalDiscountDeposit(
            PromotionalDiscountDepositRequestDto dto,
            long adminUserId
    ) {

        // Step 1: Validate Admin
        Mono<UserProfileDto> adminCheckMono = userProfileService
                .getUserProfileByTelegramIdAndAgentId(adminUserId, dto.getAgentId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Admin user not found: " + adminUserId)))
                .flatMap(admin -> {
                    if (!UserRole.ADMIN.equals(admin.getRole()) && !UserRole.AGENT.equals(admin.getRole())) {
                        return Mono.error(new IllegalAccessException("User is not authorized for discounts"));
                    }
                    return Mono.just(admin);
                });

        // Step 2: Validate target user
        Mono<UserProfileDto> userProfileMono = userProfileService
                .getUserProfileByTelegramIdAndAgentId(dto.getUserTelegramId(), dto.getAgentId())
                .switchIfEmpty(
                        Mono.error(new ResourceNotFoundException(
                                "User profile not found for telegram ID: " + dto.getUserTelegramId()
                        ))
                );

        // Step 3: Fetch payment method
        Mono<PaymentMethodDto> paymentMethodMono = paymentMethodService
                .getPaymentMethodByCode("internal")
                .switchIfEmpty(
                        Mono.error(new ResourceNotFoundException("Payment method 'internal' not found"))
                );

        // === COMBINED REACTIVE BUSINESS LOGIC ===
        Mono<PaymentOrderDto> businessFlow = adminCheckMono
                .then(Mono.zip(userProfileMono, paymentMethodMono))
                .flatMap(tuple -> {

                    UserProfileDto userProfile = tuple.getT1();
                    PaymentMethodDto paymentMethod = tuple.getT2();

                    PaymentOrder order = new PaymentOrder();
                    order.setUserId(userProfile.getId());
                    order.setAmount(dto.getAmount());
                    order.setCurrency("ETB");
                    order.generateAndSetTxnRef();
                    order.setTxnType(TransactionType.PROMOTIONAL_BONUS);
                    order.setPaymentMethodId(paymentMethod.getId());
                    order.setNonce(UUID.randomUUID().toString());
                    order.setApprovedBy(adminUserId);
                    order.setReason("Promotional discount deposit");
                    order.setProviderOrderRef("promo-" + UUID.randomUUID().toString().substring(0, 10));
                    order.setStatus(PaymentOrderStatus.COMPLETED);
                    order.setAgentId(dto.getAgentId());

                    log.info("Creating promo deposit for user {} | amount {} | admin {}",
                            userProfile.getId(), dto.getAmount(), adminUserId);

                    return orderRepo.save(order)
                            .flatMap(savedOrder ->
                                    walletService.credit(
                                            userProfile.getId(),
                                            dto.getAmount(),
                                            "Promotional bonus deposit: " + savedOrder.getTxnRef(),
                                            TransactionType.PROMOTIONAL_BONUS,
                                            Map.of(
                                                    "paymentOrderId", savedOrder.getId(),
                                                    "userTelegramId", dto.getUserTelegramId(),
                                                    "adminUserId", adminUserId
                                            )
                                    ).thenReturn(savedOrder)
                            )
//                            .flatMap(savedOrder ->
//                                    dailyAgentAccountingService.updateForPromoBonus(savedOrder.getAgentId(), savedOrder.getAmount())
//                                            .then(totalAgentAccountingService.updateForPromoBonus(savedOrder.getAgentId(), savedOrder.getAmount()))
//                                            .thenReturn(savedOrder)
//                            )
                            .map(PaymentOrderMapper::toDto);
                });

        // === TRANSACTIONAL WRAPPER ===
        return transactionalOperator.transactional(businessFlow);
    }


}
