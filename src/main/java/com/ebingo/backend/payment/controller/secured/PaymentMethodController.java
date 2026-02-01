package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.payment.dto.PaymentMethodDto;
import com.ebingo.backend.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/secured/payment-methods")
@RequiredArgsConstructor
@Tag(name = "Payment Method Secured Controller", description = "Payment Method Secured Controller")
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    @GetMapping
    @Operation(summary = "Get all payment methods", description = "Get all payment methods")
    public Mono<ResponseEntity<ApiResponse<List<PaymentMethodDto>>>> getAllPaymentMethods(
            ServerWebExchange exchange) {
        return paymentMethodService.getAllPaymentMethods()
                .collectList()
                .map(pms -> ApiResponse.<List<PaymentMethodDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Rooms retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(pms)
                        .build())
                .map(ResponseEntity::ok);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get payment method by ID", description = "Get payment method by ID")
    public Mono<ResponseEntity<ApiResponse<PaymentMethodDto>>> getPaymentMethodById(
            @Parameter(required = true, description = "Room ID") @RequestParam Long id,
            ServerWebExchange exchange) {
        return paymentMethodService.getPaymentMethodById(id)
                .map(pm -> ApiResponse.<PaymentMethodDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Payment method retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(pm)
                        .build()
                )
                .map(ResponseEntity::ok);
    }
}
