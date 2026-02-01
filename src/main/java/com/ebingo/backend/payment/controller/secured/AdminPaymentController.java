//package com.ebingo.backend.payment.controller.secured;
//
//import com.ebingo.backend.common.dto.ApiResponse;
//import com.ebingo.backend.common.telegram.AuthenticatedTelegramUser;
//import com.ebingo.backend.common.telegram.TelegramUser;
//import com.ebingo.backend.payment.dto.PaymentOrderDto;
//import com.ebingo.backend.payment.service.PaymentOrderServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.time.Instant;
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/secured/admin/payments")
//@RequiredArgsConstructor
//public class AdminPaymentController {
//
//    private final PaymentOrderServiceImpl paymentOrderService;
//
//    /**
//     * Get offline (manual) payment orders, optionally filtered by status.
//     */
//    @GetMapping("/offline/orders")
//    public Mono<ResponseEntity<ApiResponse<List<PaymentOrderDto>>>> getOfflineOrders(
//            @RequestParam(required = false) String status,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @AuthenticatedTelegramUser TelegramUser admin,
//            ServerWebExchange exchange
//    ) {
//        Mono<List<PaymentOrderDto>> ordersMono = (status == null || status.isBlank())
//                ? paymentOrderService.getOfflineOrders(page, size, sortBy).collectList()
//                : paymentOrderService.getOfflineOrdersByStatus(status, page, size, sortBy).collectList();
//
//        return ordersMono
//                .map(orders -> ResponseEntity.ok(
//                        ApiResponse.<List<PaymentOrderDto>>builder()
//                                .statusCode(HttpStatus.OK.value())
//                                .success(true)
//                                .message("Offline orders retrieved successfully")
//                                .data(orders)
//                                .timestamp(Instant.now())
//                                .path(exchange.getRequest().getPath().value())
//                                .build()
//                ))
//                .onErrorResume(ex -> {
//                    log.error("Failed to fetch offline orders: {}", ex.getMessage(), ex);
//                    return Mono.just(ResponseEntity.internalServerError().body(
//                            ApiResponse.<List<PaymentOrderDto>>builder()
//                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                                    .success(false)
//                                    .message("Failed to retrieve offline orders: " + ex.getMessage())
//                                    .timestamp(Instant.now())
//                                    .path(exchange.getRequest().getPath().value())
//                                    .build()
//                    ));
//                });
//    }
//
//    /**
//     * Confirm offline payment manually by an admin.
//     */
//    @PostMapping("/offline/{orderId}/confirm")
//    public Mono<ResponseEntity<ApiResponse<Void>>> confirmOffline(
//            @PathVariable Long orderId,
//            @AuthenticatedTelegramUser TelegramUser admin,
//            ServerWebExchange exchange
//    ) {
//        return paymentOrderService.confirmOfflineOrder(orderId, admin.id())
//                .thenReturn(ResponseEntity.ok(
//                        ApiResponse.<Void>builder()
//                                .statusCode(HttpStatus.OK.value())
//                                .success(true)
//                                .message("Offline order confirmed successfully")
//                                .timestamp(Instant.now())
//                                .path(exchange.getRequest().getPath().value())
//                                .build()
//                ))
//                .onErrorResume(ex -> {
//                    log.error("Failed to confirm offline order {}: {}", orderId, ex.getMessage(), ex);
//                    return Mono.just(ResponseEntity.internalServerError().body(
//                            ApiResponse.<Void>builder()
//                                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                                    .success(false)
//                                    .message("Failed to confirm offline order: " + ex.getMessage())
//                                    .timestamp(Instant.now())
//                                    .path(exchange.getRequest().getPath().value())
//                                    .build()
//                    ));
//                });
//    }
//}
