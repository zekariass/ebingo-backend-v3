package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.telegram.TelegramAuthService;
import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.payment.dto.WalletWithUserProfileDto;
import com.ebingo.backend.payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/secured/wallet")
@Tag(name = "Wallet Secured Controller", description = "Wallet Secured Controller")
public class WalletController {

    private final WalletService walletService;
    private final TelegramAuthService telegramAuthService;

    @GetMapping
    @Operation(summary = "Get wallet", description = "Get wallet for the authenticated Telegram user")
    public Mono<ResponseEntity<ApiResponse<WalletDto>>> getWallet(
//            @RequestHeader("x-init-data") String telegramInitData,
            @RequestParam Long telegramId,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {
        return walletService.getWalletByTelegramId(telegramId, agentId)
                .map(wallet -> ResponseEntity.ok(
                        ApiResponse.<WalletDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Wallet retrieved successfully")
                                .data(wallet)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ));
    }


    @GetMapping("/by-telegram-id")
    @Operation(summary = "Get wallet", description = "Get wallet by TelegramId")
    public Mono<ResponseEntity<ApiResponse<WalletDto>>> getWalletByTelegramId(
            @RequestParam Long telegramId,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {
        return walletService.getWalletByTelegramId(telegramId, agentId)
                .map(wallet -> ResponseEntity.ok(
                        ApiResponse.<WalletDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Wallet retrieved successfully")
                                .data(wallet)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ))
                .onErrorResume(ResponseStatusException.class, ex ->
                        Mono.just(ResponseEntity.status(ex.getStatusCode()).body(
                                ApiResponse.<WalletDto>builder()
                                        .statusCode(ex.getStatusCode().value())
                                        .success(false)
                                        .message(ex.getReason())
                                        .path(exchange.getRequest().getPath().value())
                                        .timestamp(Instant.now())
                                        .build()
                        ))
                );
    }

    @GetMapping("/details")
    @Operation(summary = "Get wallet details", description = "Get wallet details by user phone number")
    public Mono<ResponseEntity<ApiResponse<WalletWithUserProfileDto>>> getWalletDetails(
            @RequestParam String userPhone,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {
        return walletService.getWalletWithUserProfileByUserProfileId(userPhone, agentId)
                .map(walletDetails -> ResponseEntity.ok(
                        ApiResponse.<WalletWithUserProfileDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Wallet details retrieved successfully")
                                .data(walletDetails)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ));
    }


}
