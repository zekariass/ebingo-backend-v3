package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.telegram.TelegramAuthVerifier;
import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.service.PaymentOrderServiceImpl;
import com.ebingo.backend.payment.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Tag(name = "Transaction Secured Controller", description = "Transaction Secured Controller")
@RequestMapping("/api/v1/secured/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final PaymentOrderServiceImpl paymentOrderServiceImpl;
    private final ObjectMapper objectMapper;
    private final TelegramAuthVerifier telegramAuthVerifier;

    @GetMapping
    @Operation(summary = "Get all transactions with pagination", description = "Get all transactions with pagination")
    public Mono<ResponseEntity<ApiResponse<List<TransactionDto>>>> getPaginatedTransactions(
            @RequestParam Long telegramId,
            @RequestParam Long agentId,
            @Parameter(required = true, description = "Page number") @RequestParam Integer page,
            @Parameter(required = false, description = "Page size") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(required = false, description = "Sort by") @RequestParam(defaultValue = "createdAt") String sortBy,
//            @AuthenticatedTelegramUser TelegramUser user,
            ServerWebExchange exchange
    ) {


        // Fetch transactions
        return transactionService.getPaginatedTransaction(telegramId, agentId, page, size, sortBy)
                .collectList()
                .map(txns -> ApiResponse.<List<TransactionDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transactions retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txns)
                        .build())
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError().body(
                        ApiResponse.<List<TransactionDto>>builder()
                                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .success(false)
                                .message("Internal server error: " + ex.getMessage())
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                )));
    }
}
