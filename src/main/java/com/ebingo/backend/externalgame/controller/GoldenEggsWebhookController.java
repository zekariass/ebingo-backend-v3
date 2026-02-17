package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.externalgame.dto.bonus.BonusDTOs.BonusWebhookRequest;
import com.ebingo.backend.externalgame.dto.webhook.BetRequest;
import com.ebingo.backend.externalgame.dto.webhook.ErrorResponse;
import com.ebingo.backend.externalgame.dto.webhook.InitRequest;
import com.ebingo.backend.externalgame.dto.webhook.RollbackRequest;
import com.ebingo.backend.externalgame.dto.webhook.WithdrawRequest;
import com.ebingo.backend.externalgame.service.GoldenEggsBonusService;
import com.ebingo.backend.externalgame.service.GoldenEggsIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/webhooks/golden-eggs")
@RequiredArgsConstructor
@Slf4j
public class GoldenEggsWebhookController {

    private final GoldenEggsIntegrationService integrationService;
    private final GoldenEggsBonusService bonusService;
    private final ObjectMapper objectMapper;

    /**
     * Main webhook endpoint that handles all actions from Golden Eggs provider
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> handleWebhook(ServerHttpRequest request) {
        log.info("Webhook request received from Golden Eggs");

        // Extract signature header
        String signature = request.getHeaders().getFirst("X-REQUEST-SIGN");

        if (signature == null || signature.isEmpty()) {
            log.warn("Missing signature header");
            return createErrorResponse("INVALID_TOKEN", "Missing signature");
        }

        // Read raw body for signature validation
        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String rawBody = new String(bytes, StandardCharsets.UTF_8);

                    // Validate signature
                    if (!integrationService.validateSignature(rawBody, signature)) {
                        log.warn("Invalid signature");
                        return createErrorResponse("INVALID_TOKEN", "Invalid signature");
                    }

                    // Parse JSON and route to appropriate handler
                    return routeWebhook(rawBody);
                })
                .onErrorResume(e -> {
                    log.error("Error processing webhook", e);
                    return createErrorResponse("UNKNOWN_ERROR", "Internal error");
                });
    }

    /**
     * Route webhook to appropriate handler based on action
     */
    private Mono<ResponseEntity<String>> routeWebhook(String rawBody) {
        try {
            // Parse base request to determine action
            var baseRequest = objectMapper.readTree(rawBody);
            String action = baseRequest.get("action").asText();

            log.info("Routing webhook action: {}", action);
            log.info("Raw body: {}", rawBody);

            switch (action) {
                case "init":
                    InitRequest initRequest = objectMapper.readValue(rawBody, InitRequest.class);
                    if (!isValidCurrency(initRequest.getData().getCurrency())) {
                        log.warn("Invalid currency for init request: {}", initRequest.getData().getCurrency());
                        return createErrorResponse("CHECKS_FAIL", "Only ETB currency is supported");
                    }
                    return integrationService.handleInit(initRequest)
                            .flatMap(response -> {
                                try {
                                    String json = objectMapper.writeValueAsString(response);
                                    return Mono.just(ResponseEntity.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(json));
                                } catch (Exception e) {
                                    log.error("Error serializing init response", e);
                                    return createErrorResponse("UNKNOWN_ERROR", "Serialization error");
                                }
                            });

                case "bet":
                    BetRequest betRequest = objectMapper.readValue(rawBody, BetRequest.class);
                    if (!isValidCurrency(betRequest.getData().getCurrency())) {
                        log.warn("Invalid currency for bet request: {}", betRequest.getData().getCurrency());
                        return createErrorResponse("CHECKS_FAIL", "Only ETB currency is supported");
                    }
                    return integrationService.handleBet(betRequest)
                            .flatMap(response -> {
                                try {
                                    String json = objectMapper.writeValueAsString(response);
                                    return Mono.just(ResponseEntity.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(json));
                                } catch (Exception e) {
                                    log.error("Error serializing bet response", e);
                                    return createErrorResponse("UNKNOWN_ERROR", "Serialization error");
                                }
                            });

                case "withdraw":
                    WithdrawRequest withdrawRequest = objectMapper.readValue(rawBody, WithdrawRequest.class);
                    if (!isValidCurrency(withdrawRequest.getData().getCurrency())) {
                        log.warn("Invalid currency for withdraw request: {}", withdrawRequest.getData().getCurrency());
                        return createErrorResponse("CHECKS_FAIL", "Only ETB currency is supported");
                    }
                    return integrationService.handleWithdraw(withdrawRequest)
                            .map(jsonResponse -> ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jsonResponse));

                case "rollback":
                    RollbackRequest rollbackRequest = objectMapper.readValue(rawBody, RollbackRequest.class);
                    if (!isValidCurrency(rollbackRequest.getData().getCurrency())) {
                        log.warn("Invalid currency for rollback request: {}", rollbackRequest.getData().getCurrency());
                        return createErrorResponse("CHECKS_FAIL", "Only ETB currency is supported");
                    }
                    return integrationService.handleRollback(rollbackRequest)
                            .map(jsonResponse -> ResponseEntity.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(jsonResponse));

                case "bonus-complete":
                    BonusWebhookRequest bonusCompleteRequest = objectMapper.readValue(rawBody, BonusWebhookRequest.class);
                    return bonusService.handleBonusComplete(bonusCompleteRequest)
                            .flatMap(response -> {
                                try {
                                    String json = objectMapper.writeValueAsString(response);
                                    return Mono.just(ResponseEntity.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(json));
                                } catch (Exception e) {
                                    log.error("Error serializing bonus-complete response", e);
                                    return createErrorResponse("UNKNOWN_ERROR", "Serialization error");
                                }
                            });

                case "bonus-expired-when-active":
                    BonusWebhookRequest bonusExpiredRequest = objectMapper.readValue(rawBody, BonusWebhookRequest.class);
                    return bonusService.handleBonusExpired(bonusExpiredRequest)
                            .flatMap(response -> {
                                try {
                                    String json = objectMapper.writeValueAsString(response);
                                    return Mono.just(ResponseEntity.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(json));
                                } catch (Exception e) {
                                    log.error("Error serializing bonus-expired response", e);
                                    return createErrorResponse("UNKNOWN_ERROR", "Serialization error");
                                }
                            });

                default:
                    log.warn("Unknown action: {}", action);
                    return createErrorResponse("UNKNOWN_ERROR", "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error parsing webhook request", e);
            return createErrorResponse("UNKNOWN_ERROR", "Invalid request format");
        }
    }

    /**
     * Validate that currency is ETB
     */
    private boolean isValidCurrency(String currency) {
        return "ETB".equalsIgnoreCase(currency);
    }

    /**
     * Create error response with HTTP 200 (as per provider requirements)
     */
    private Mono<ResponseEntity<String>> createErrorResponse(String code, String message) {
        try {
            ErrorResponse error = ErrorResponse.builder()
                    .code(code)
                    .message(message)
                    .build();
            String json = objectMapper.writeValueAsString(error);
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json));
        } catch (Exception e) {
            log.error("Error creating error response", e);
            return Mono.just(ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":\"" + code + "\"}"));
        }
    }
}
