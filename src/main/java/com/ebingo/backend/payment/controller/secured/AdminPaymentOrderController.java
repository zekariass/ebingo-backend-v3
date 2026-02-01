package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.payment.dto.*;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.payment.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/secured/payment-orders/offline")
@RequiredArgsConstructor
@Tag(name = "Payment Orders Endpoint", description = "Payment Orders Endpoint")
public class AdminPaymentOrderController {

    private final PaymentOrderService paymentOrderService;


    @GetMapping
    public Mono<ResponseEntity<ApiResponse<PageResponse<PaymentOrderListDto>>>> getPaymentOrders(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam Long agentId,
            @RequestParam(required = false) PaymentOrderStatus status,
            @RequestParam(required = false) TransactionType txnType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
//            @AuthenticatedTelegramUser TelegramUser admin,
            ServerWebExchange exchange
    ) {
        PaymentOrderGetParamsDto params = PaymentOrderGetParamsDto.builder()
                .phoneNumber(phoneNumber)
                .status(status)
                .txnType(txnType)
                .page(page)
                .size(size)
                .agentId(agentId)
                .build();
        return paymentOrderService.getPaymentOrdersForAdmin(params)
                .map(pageResponse -> ApiResponse.<PageResponse<PaymentOrderListDto>>builder()
                        .success(true)
                        .statusCode(200)
                        .message("Withdrawals fetched successfully")
                        .path(exchange.getRequest().getPath().value())
                        .data(pageResponse)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @GetMapping("/detail/{id}")
    public Mono<ResponseEntity<ApiResponse<PaymentOrderDetailDto>>> getPaymentOrderDetail(
            @PathVariable Long id,
//            @AuthenticatedTelegramUser TelegramUser admin,
            ServerWebExchange exchange
    ) {

        return paymentOrderService.getPaymentOrderDetail(id)
                .map(detail -> ApiResponse.<PaymentOrderDetailDto>builder()
                        .success(true)
                        .statusCode(200)
                        .message("Payment order detail fetched successfully")
                        .path(exchange.getRequest().getPath().value())
                        .data(detail)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    // By Admin - Approve or Reject a withdrawal
    @PutMapping("/admin/change-withdrawal-status")
    @Operation(summary = "Approve Withdrawal", description = "Admin approves a pending withdrawal")
    public Mono<ResponseEntity<ApiResponse<PaymentOrderDto>>> approveWithdrawal(
            @RequestBody WithdrawalApprovalRequestDto dto,
            @RequestParam Long adminUserId,
//            @AuthenticatedTelegramUser TelegramUser admin,
            ServerWebExchange exchange) {

        log.info(">>>>>>>>>>>>>>>>>: Admin {} is approving withdrawal {}", adminUserId, dto);

        return paymentOrderService.confirmWithdrawalByAdmin(dto, adminUserId)
                .map(txn -> ApiResponse.<PaymentOrderDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Withdrawal approved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


    @PostMapping("/admin/add-promotional-discount")
    public Mono<ResponseEntity<ApiResponse<PaymentOrderDto>>> createPromotionalDiscountDeposit(
            @RequestBody PromotionalDiscountDepositRequestDto dto,
            @RequestParam Long adminTelegramId,
//            @AuthenticatedTelegramUser TelegramUser admin,
            ServerWebExchange exchange
    ) {
        return paymentOrderService.createPromotionalDiscountDeposit(dto, adminTelegramId)
                .map(txn -> ApiResponse.<PaymentOrderDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Deposit updated successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


    // Add a deposit
    @PostMapping("/offline-deposit")
    public Mono<ResponseEntity<ApiResponse<PaymentOrderDto>>> offlineDeposit(
            @RequestBody OfflineDepositRequestDto dto,
            ServerWebExchange exchange
    ) {
        return paymentOrderService.offlineDeposit(dto)
                .map(txn -> ApiResponse.<PaymentOrderDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Deposit created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


    // Check deposit status@Data
    @Builder
    @GetMapping("/check-deposit-status/{providerTxnRef}")
    public Mono<ResponseEntity<ApiResponse<PaymentOrderDto>>> checkDepositStatusByProviderTxnRef(
            @PathVariable String providerTxnRef,
//            @AuthenticatedTelegramUser TelegramUser user,
            ServerWebExchange exchange
    ) {
        return paymentOrderService.checkDepositStatusByProviderTxnRef(providerTxnRef)
                .map(txn -> ApiResponse.<PaymentOrderDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Deposit status fetched successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }

}
