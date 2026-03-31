package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.telegram.TelegramAuthVerifier;
import com.ebingo.backend.payment.dto.GameTransactionDto;
import com.ebingo.backend.payment.service.GameTransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/secured/game/transaction")
@RequiredArgsConstructor
@Tag(name = "Game Transaction Management", description = "Game Transaction Management")
@RequireAccessToken
public class GameTransactionController {

    private final GameTransactionService gameTransactionService;
    private final ObjectMapper objectMapper;
    private final TelegramAuthVerifier telegramAuthVerifier;

    @GetMapping
    @Operation(summary = "Get paginated game transactions", description = "Get paginated game transactions")
    public Mono<ResponseEntity<ApiResponse<List<GameTransactionDto>>>> getPaginatedGameTransactions(
            @RequestParam Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestHeader(value = "x-init-data") String telegramInitData,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {
        // Verify Telegram init data
        Optional<Map<String, String>> initData = telegramAuthVerifier.verifyInitData(telegramInitData);
        if (initData.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<List<GameTransactionDto>>builder()
                            .statusCode(HttpStatus.UNAUTHORIZED.value())
                            .success(false)
                            .message("Invalid Telegram init data")
                            .path(exchange.getRequest().getPath().value())
                            .timestamp(Instant.now())
                            .build()));
        }

        //  Parse user JSON
        Map<String, Object> user;
        try {
            String userJson = initData.get().get("user");
            if (userJson == null) {
                return Mono.just(ResponseEntity.badRequest()
                        .body(ApiResponse.<List<GameTransactionDto>>builder()
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .success(false)
                                .message("Missing 'user' field in initData")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()));
            }
            user = objectMapper.readValue(userJson, Map.class);
        } catch (JsonProcessingException e) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponse.<List<GameTransactionDto>>builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .success(false)
                            .message("Invalid user data: " + e.getOriginalMessage())
                            .path(exchange.getRequest().getPath().value())
                            .timestamp(Instant.now())
                            .build()));
        }

        //  Extract Long user ID safely
        Object idObj = user.get("id");
        if (idObj == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponse.<List<GameTransactionDto>>builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .success(false)
                            .message("Missing user ID in initData")
                            .path(exchange.getRequest().getPath().value())
                            .timestamp(Instant.now())
                            .build()));
        }

        long userId;
        try {
            if (idObj instanceof Number n) {
                userId = n.longValue();
            } else {
                // fallback if ID is accidentally a string
                userId = Long.parseLong(idObj.toString());
            }
        } catch (Exception e) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponse.<List<GameTransactionDto>>builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .success(false)
                            .message("Invalid user ID format: " + e.getMessage())
                            .path(exchange.getRequest().getPath().value())
                            .timestamp(Instant.now())
                            .build()));
        }

        //  Fetch transactions
        return gameTransactionService.getPaginatedGameTransactions(userId, agentId, page, size, sortBy)
                .collectList()
                .map(txns -> ApiResponse.<List<GameTransactionDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transactions retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txns)
                        .build())
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(
                        ResponseEntity.internalServerError().body(
                                ApiResponse.<List<GameTransactionDto>>builder()
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .success(false)
                                        .message("Internal server error: " + ex.getMessage())
                                        .path(exchange.getRequest().getPath().value())
                                        .timestamp(Instant.now())
                                        .build()
                        )
                ));
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get game transaction by ID", description = "Get game transaction by ID")
    public Mono<ResponseEntity<ApiResponse<GameTransactionDto>>> getGameTransactionById(
            @Parameter(required = true, description = "Game Transaction ID") @RequestParam Long id,
            @RequestHeader(value = "x-init-data", required = true) String telegramInitData,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {

        Optional<Map<String, String>> initData = telegramAuthVerifier.verifyInitData(telegramInitData);

        if (initData.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.<GameTransactionDto>builder()
                                    .statusCode(HttpStatus.UNAUTHORIZED.value())
                                    .success(false)
                                    .message("Invalid telegram init data")
                                    .path(exchange.getRequest().getPath().value())
                                    .timestamp(Instant.now())
                                    .build()
                    ));
        }

        Map<String, Object> user;

        try {
            user = objectMapper.readValue(initData.get().get("user"), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return gameTransactionService.getTransactionById(id, agentId, (Long) user.get("id"))
                .map(txn -> ApiResponse.<GameTransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Game transaction is retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


}
