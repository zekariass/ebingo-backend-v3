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


//    @GetMapping("/{id}")
//    @Operation(summary = "Get transaction by ID", description = "Get transaction by ID")
//    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> getTransactionById(
//            @RequestParam Long telegramId,
//            @Parameter(required = true, description = "Transaction ID") @RequestParam Long id,
//            ServerWebExchange exchange) {
//        return transactionService.getTransactionById(id, telegramId)
//                .map(txn -> ApiResponse.<TransactionDto>builder()
//                        .statusCode(HttpStatus.OK.value())
//                        .success(true)
//                        .message("Transaction is retrieved successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(txn)
//                        .build()
//                )
//                .map(ResponseEntity::ok);
//    }
//
//
//    @PostMapping("/deposit/initiate-offline")
//    @Operation(summary = "Initiate deposit", description = "Initiate deposit")
//    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> initiateDeposit(
//            @Parameter(required = true, description = "Amount") @RequestBody InitiateDepositRequest depositRequest,
//            @RequestParam Long telegramId,
//            ServerWebExchange exchange
//    ) {
//
//        return transactionService.initiateOfflineDeposit(depositRequest, telegramId)
//                .map(txn -> ApiResponse.<TransactionDto>builder()
//                        .statusCode(HttpStatus.CREATED.value())
//                        .success(true)
//                        .message("Deposit initiated successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(txn)
//                        .build()
//                )
//                .map(ResponseEntity::ok);
//    }
//
//
//    @PutMapping("/deposit/confirm-offline")
//    @Operation(summary = "Confirm deposit offline", description = "Confirm deposit offline")
//    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> confirmDepositOfflineByAdmin(
//            @Parameter(required = true, description = "txnRef") @RequestParam String txnRef,
//            @Parameter(required = false, description = "metaData") @RequestBody String metaData,
//            @RequestParam Long approverTelegramId,
//            ServerWebExchange exchange
//    ) {
//
//        return transactionService.confirmDepositOfflineByAdmin(txnRef, metaData, approverTelegramId)
//                .map(txn -> ApiResponse.<TransactionDto>builder()
//                        .statusCode(HttpStatus.OK.value())
//                        .success(true)
//                        .message("Deposit confirmed successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(txn)
//                        .build()
//                )
//                .map(ResponseEntity::ok);
//    }
//
//
//    @GetMapping("/by-status")
//    @Operation(summary = "Get transactions by status", description = "Get transactions by status")
//    public Mono<ResponseEntity<ApiResponse<List<TransactionDto>>>> getPaginatedTransactionsByStatusForAdmin(
//            @RequestParam(required = false) PaymentOrderStatus status,
//            @RequestParam(required = false) TransactionType type,
//            @RequestParam Integer page,
//            @RequestParam(defaultValue = "10") Integer size,
//            @RequestParam String sortBy,
//            ServerWebExchange exchange
//    ) {
//        return transactionService.getPaginatedDepositsByStatusForAdmin(status, type, page, size, sortBy)
//                .collectList()
//                .map(txns -> ApiResponse.<List<TransactionDto>>builder()
//                        .statusCode(HttpStatus.OK.value())
//                        .success(true)
//                        .message("Transactions retrieved successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(txns)
//                        .build())
//                .map(ResponseEntity::ok);
//    }
//
//
//    @PutMapping("/deposit/{id}/cancel-offline")
//    @Operation(summary = "Cancel deposit offline", description = "Cancel deposit offline")
//    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> cancelDepositOffline(
//            @Parameter(required = true, description = "id") @PathVariable Long id,
//            ServerWebExchange exchange
//    ) {
//
//        return transactionService.cancelDepositOffline(id)
//                .map(txn -> ApiResponse.<TransactionDto>builder()
//                        .statusCode(HttpStatus.OK.value())
//                        .success(true)
//                        .message("Deposit cancelled successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(txn)
//                        .build()
//                )
//                .map(ResponseEntity::ok);
//    }
//
//


//
//
//    @PatchMapping("/{txnRef}/change-status")
//    @Operation(summary = "Change Transaction status", description = "Change Transaction status")
//    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> changeStatus(
//            @RequestParam Long approverTelegramId,
//            @PathVariable String txnRef,
//            @RequestParam PaymentOrderStatus status,
//            ServerWebExchange exchange) {
//
//        return transactionService.changeTransactionStatus(txnRef, status, approverTelegramId)
//                .map(txn -> ApiResponse.<TransactionDto>builder()
//                        .statusCode(HttpStatus.OK.value())
//                        .success(true)
//                        .message("Withdrawal approved successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(txn)
//                        .build())
//                .map(ResponseEntity::ok);
//    }
//
//
}
