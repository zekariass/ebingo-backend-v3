package com.ebingo.backend.payment.service;

import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.payment.dto.*;
import reactor.core.publisher.Mono;

public interface PaymentOrderService {
    Mono<PaymentOrderResponseDto> createOrder(PaymentOrderRequestDto dto);

    Mono<Void> processAddisPayCallbackForSuccess(
            String sessionUuid,
            String paymentStatus,
            String totalAmount,
            String orderId,
            String nonce,
            String addisPayTransactionId,
            String thirdPartyTransactionRef);

    Mono<Void> processAddisPayCallbackForError(
            String sessionUuid,
            String paymentStatus,
            String totalAmount,
            String orderId,
            String nonce,
            String addisTransactionId,
            String thirdPartyTransactionRef);

    Mono<Void> processAddisPayCallbackCancel(
            String sessionUuid,
            String paymentStatus,
            String totalAmount,
            String orderId,
            String nonce,
            String addisTransactionId,
            String thirdPartyTransactionRef);

    Mono<PaymentInitiateResponseDto> initiatePaymentOnline(PaymentInitiateRequestDto dto);

    Mono<PaymentOrderDto> confirmWithdrawalByAdmin(WithdrawalApprovalRequestDto dto, Long adminUserId);

    Mono<WithdrawalResponseDto> withdraw(WithdrawRequestDto withdrawRequestDto, Long telegramId);

    Mono<PageResponse<PaymentOrderDto>> getAdminWithdrawals(String status, int page, int size);

    Mono<PageResponse<PaymentOrderDto>> getUserWithdrawals(Long userId, String status, int page, int size);

    Mono<PageResponse<PaymentOrderDto>> getAdminDeposits(String status, int page, int size);

    Mono<PageResponse<PaymentOrderDto>> getUserDeposits(Long userId, String status, int page, int size);

    Mono<PageResponse<PaymentOrderListDto>> getPaymentOrdersForAdmin(PaymentOrderGetParamsDto params);

    Mono<PaymentOrderDetailDto> getPaymentOrderDetail(Long poId);

    Mono<PaymentOrderDto> offlineDeposit(OfflineDepositRequestDto dto);

    Mono<PaymentOrderDto> checkDepositStatusByProviderTxnRef(String providerTxnRef);

    Mono<PaymentOrderDto> createPromotionalDiscountDeposit(PromotionalDiscountDepositRequestDto dto, long adminTelegramId);
}
