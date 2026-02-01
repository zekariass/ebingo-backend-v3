package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.payment.dto.DepositTransferDto;
import com.ebingo.backend.payment.dto.DepositTransferRequestDto;
import com.ebingo.backend.payment.service.DepositTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/secured/deposit/transfers")
@RequiredArgsConstructor
@Tag(name = "Deposit Transfers Endpoint", description = "Deposit Transfers Endpoint")
public class DepositTransferController {

    private final DepositTransferService depositTransferService;

    // Get a single deposit transfer
    @GetMapping("/{id}")
    @Operation(summary = "Get a single deposit transfer", description = "Retrieve a single deposit transfer for the authenticated Telegram user")
    public Mono<ResponseEntity<ApiResponse<DepositTransferDto>>> getASingleDepositTransfer(
//            @AuthenticatedTelegramUser TelegramUser user,
            @PathVariable Long id,
            @RequestParam Long agentId,
            @RequestParam Long telegramId,
            ServerWebExchange exchange
    ) {
        return depositTransferService.getASingleDepositTransfer(id, telegramId, agentId)
                .map(transfer -> ApiResponse.<DepositTransferDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transfer retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(transfer)
                        .build())
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> handleError(ex, exchange, "Failed to retrieve deposit transfer"));
    }

    // Create a deposit transfer
    @PostMapping
    @Operation(summary = "Create a deposit transfer", description = "Create a deposit transfer for the authenticated Telegram user")
    public Mono<ResponseEntity<ApiResponse<DepositTransferDto>>> createDepositTransfer(
//            @AuthenticatedTelegramUser TelegramUser user,
            @RequestParam Long telegramId,
            @Valid @RequestBody DepositTransferRequestDto depositTransferDto,
            ServerWebExchange exchange
    ) {
        return depositTransferService.createDepositTransfer(depositTransferDto, telegramId)
                .map(transfer -> ApiResponse.<DepositTransferDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Transfer successful")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(transfer)
                        .build())
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> handleError(ex, exchange, "Failed to create deposit transfer"));
    }

    // Delete a deposit transfer
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a deposit transfer", description = "Delete a deposit transfer for the authenticated Telegram user")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteDepositTransfer(
//            @AuthenticatedTelegramUser TelegramUser user,
            @PathVariable Long id,
            @RequestParam Long agentId,
            @RequestParam Long telegramId,
            ServerWebExchange exchange
    ) {
        return depositTransferService.deleteDepositTransfer(id, telegramId, agentId)
                .then(Mono.fromSupplier(() -> ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transfer deleted successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .build()))
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> handleError(ex, exchange, "Failed to delete deposit transfer"));
    }

    // Centralized error handler for all endpoints
    private <T> Mono<ResponseEntity<ApiResponse<T>>> handleError(Throwable ex, ServerWebExchange exchange, String defaultMessage) {
        log.error("{}: {}", defaultMessage, ex.getMessage(), ex);

        HttpStatus status = (ex instanceof SecurityException)
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;

        return Mono.just(ResponseEntity.status(status)
                .body(ApiResponse.<T>builder()
                        .statusCode(status.value())
                        .success(false)
                        .message(ex.getMessage() != null ? ex.getMessage() : defaultMessage)
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .build()));
    }
}
