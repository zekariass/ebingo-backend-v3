package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.externalgame.dto.webhook.*;
import com.ebingo.backend.externalgame.service.GoldenEggsBonusService;
import com.ebingo.backend.externalgame.service.GoldenEggsIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(GoldenEggsWebhookController.class)
@ContextConfiguration(classes = {GoldenEggsWebhookController.class, GoldenEggsWebhookControllerTest.TestConfig.class})
class GoldenEggsWebhookControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GoldenEggsIntegrationService integrationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Configuration
    static class TestConfig {
        @Bean
        public GoldenEggsIntegrationService integrationService() {
            return Mockito.mock(GoldenEggsIntegrationService.class);
        }

        @Bean
        public GoldenEggsBonusService bonusService() {
            return Mockito.mock(GoldenEggsBonusService.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String SECRET = "test-secret-key";

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        Mockito.reset(integrationService);
        // Default signature validation to true
        when(integrationService.validateSignature(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void testInitWebhook_Success() throws Exception {
        // Prepare request
        InitRequest request = InitRequest.builder()
                .action("init")
                .token("test-token")
                .data(InitRequestData.builder()
                        .currency("ETB")
                        .operator("test-operator")
                        .gameMode("crash")
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response
        InitResponse response = InitResponse.builder()
                .code("OK")
                .userId("1")
                .nickname("testuser")
                .balance("100.00")
                .currency("ETB")
                .operator("test-operator")
                .token("session-token")
                .build();

        when(integrationService.handleInit(any(InitRequest.class)))
                .thenReturn(Mono.just(response));

        // Execute and verify
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.userId").isEqualTo("1")
                .jsonPath("$.balance").isEqualTo("100.00");
    }

    @Test
    void testBetWebhook_InsufficientFunds() throws Exception {
        // Prepare request
        BetRequest request = BetRequest.builder()
                .action("bet")
                .token("session-token")
                .gameMode("crash")
                .data(BetRequestData.builder()
                        .amount("100.00")
                        .currency("ETB")
                        .operator("test-operator")
                        .userId("1")
                        .transactionId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response with error
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INSUFFICIENT_FUNDS")
                .message("Insufficient balance")
                .build();

        when(integrationService.handleBet(any(BetRequest.class)))
                .thenReturn(Mono.just(errorResponse));

        // Execute and verify
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void testWithdrawWebhook_Success() throws Exception {
        // Prepare request
        WithdrawRequest request = WithdrawRequest.builder()
                .action("withdraw")
                .token("session-token")
                .gameMode("crash")
                .data(WithdrawRequestData.builder()
                        .userId("1")
                        .currency("ETB")
                        .operator("test-operator")
                        .amount("100.00")
                        .result("200.00")
                        .coefficient("2.00")
                        .transactionId(UUID.randomUUID())
                        .debitId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response
        String responseJson = "{\"code\":\"OK\",\"balance\":\"200.00\"}";
        when(integrationService.handleWithdraw(any(WithdrawRequest.class)))
                .thenReturn(Mono.just(responseJson));

        // Execute and verify
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.balance").isEqualTo("200.00");
    }

    @Test
    void testWithdrawWebhook_Idempotency() throws Exception {
        // Same transaction ID should return cached response
        UUID transactionId = UUID.randomUUID();

        WithdrawRequest request = WithdrawRequest.builder()
                .action("withdraw")
                .token("session-token")
                .gameMode("crash")
                .data(WithdrawRequestData.builder()
                        .userId("1")
                        .currency("ETB")
                        .operator("test-operator")
                        .amount("100.00")
                        .result("200.00")
                        .coefficient("2.00")
                        .transactionId(transactionId)
                        .debitId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service to return cached response
        String cachedResponse = "{\"code\":\"OK\",\"balance\":\"200.00\"}";
        when(integrationService.handleWithdraw(any(WithdrawRequest.class)))
                .thenReturn(Mono.just(cachedResponse));

        // First call
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.balance").isEqualTo("200.00");

        // Second call should return same response
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.balance").isEqualTo("200.00");
    }

    @Test
    void testRollbackWebhook_Success() throws Exception {
        // Prepare request
        RollbackRequest request = RollbackRequest.builder()
                .action("rollback")
                .token("session-token")
                .gameMode("crash")
                .data(RollbackRequestData.builder()
                        .userId("1")
                        .currency("ETB")
                        .operator("test-operator")
                        .amount("100.00")
                        .transactionId(UUID.randomUUID())
                        .debitId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response
        String responseJson = "{\"code\":\"OK\",\"balance\":\"100.00\"}";
        when(integrationService.handleRollback(any(RollbackRequest.class)))
                .thenReturn(Mono.just(responseJson));

        // Execute and verify
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.balance").isEqualTo("100.00");
    }

    @Test
    void testWebhook_InvalidSignature() throws Exception {
        // Override default mock to return false for this test
        when(integrationService.validateSignature(anyString(), anyString())).thenReturn(false);

        InitRequest request = InitRequest.builder()
                .action("init")
                .token("test-token")
                .data(InitRequestData.builder()
                        .currency("ETB")
                        .operator("test-operator")
                        .gameMode("crash")
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // Execute with invalid signature
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", "invalid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk() // Still 200 as per provider requirements
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    void testWebhook_MissingSignature() throws Exception {
        InitRequest request = InitRequest.builder()
                .action("init")
                .token("test-token")
                .data(InitRequestData.builder()
                        .currency("ETB")
                        .operator("test-operator")
                        .gameMode("crash")
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // Execute without signature header
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk() // Still 200 as per provider requirements
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    void testInitWebhook_InvalidCurrency() throws Exception {
        // Prepare request with invalid currency (not ETB)
        InitRequest request = InitRequest.builder()
                .action("init")
                .token("test-token")
                .data(InitRequestData.builder()
                        .currency("USD") // Invalid - should be ETB
                        .operator("test-operator")
                        .gameMode("crash")
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Execute and verify - should return error WITHOUT calling service
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk() // Still 200 as per provider requirements
                .expectBody()
                .jsonPath("$.code").isEqualTo("CHECKS_FAIL")
                .jsonPath("$.message").isEqualTo("Only ETB currency is supported");

        // Verify that handleInit was NEVER called (balance not changed)
        Mockito.verify(integrationService, Mockito.never()).handleInit(any(InitRequest.class));
    }

    @Test
    void testBetWebhook_InvalidCurrency() throws Exception {
        // Prepare request with invalid currency (not ETB)
        BetRequest request = BetRequest.builder()
                .action("bet")
                .token("session-token")
                .gameMode("crash")
                .data(BetRequestData.builder()
                        .amount("100.00")
                        .currency("USD") // Invalid - should be ETB
                        .operator("test-operator")
                        .userId("1")
                        .transactionId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Execute and verify - should return error WITHOUT calling service
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk() // Still 200 as per provider requirements
                .expectBody()
                .jsonPath("$.code").isEqualTo("CHECKS_FAIL")
                .jsonPath("$.message").isEqualTo("Only ETB currency is supported");

        // Verify that handleBet was NEVER called (balance not changed)
        Mockito.verify(integrationService, Mockito.never()).handleBet(any(BetRequest.class));
    }

    @Test
    void testWithdrawWebhook_InvalidCurrency() throws Exception {
        // Prepare request with invalid currency (not ETB)
        WithdrawRequest request = WithdrawRequest.builder()
                .action("withdraw")
                .token("session-token")
                .gameMode("crash")
                .data(WithdrawRequestData.builder()
                        .userId("1")
                        .currency("EUR") // Invalid - should be ETB
                        .operator("test-operator")
                        .amount("100.00")
                        .result("200.00")
                        .coefficient("2.00")
                        .transactionId(UUID.randomUUID())
                        .debitId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Execute and verify - should return error WITHOUT calling service
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk() // Still 200 as per provider requirements
                .expectBody()
                .jsonPath("$.code").isEqualTo("CHECKS_FAIL")
                .jsonPath("$.message").isEqualTo("Only ETB currency is supported");

        // Verify that handleWithdraw was NEVER called (balance not changed)
        Mockito.verify(integrationService, Mockito.never()).handleWithdraw(any(WithdrawRequest.class));
    }

    @Test
    void testRollbackWebhook_InvalidCurrency() throws Exception {
        // Prepare request with invalid currency (not ETB)
        RollbackRequest request = RollbackRequest.builder()
                .action("rollback")
                .token("session-token")
                .gameMode("crash")
                .data(RollbackRequestData.builder()
                        .userId("1")
                        .currency("GBP") // Invalid - should be ETB
                        .operator("test-operator")
                        .amount("100.00")
                        .transactionId(UUID.randomUUID())
                        .debitId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Execute and verify - should return error WITHOUT calling service
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk() // Still 200 as per provider requirements
                .expectBody()
                .jsonPath("$.code").isEqualTo("CHECKS_FAIL")
                .jsonPath("$.message").isEqualTo("Only ETB currency is supported");

        // Verify that handleRollback was NEVER called (balance not changed)
        Mockito.verify(integrationService, Mockito.never()).handleRollback(any(RollbackRequest.class));
    }

    @Test
    void testBetWebhook_ValidCurrency_ETB() throws Exception {
        // Prepare request with valid currency (ETB)
        BetRequest request = BetRequest.builder()
                .action("bet")
                .token("session-token")
                .gameMode("crash")
                .data(BetRequestData.builder()
                        .amount("100.00")
                        .currency("ETB") // Valid
                        .operator("test-operator")
                        .userId("1")
                        .transactionId(UUID.randomUUID())
                        .gameId(UUID.randomUUID())
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response
        BetResponse response = BetResponse.builder()
                .code("OK")
                .balance("900.00")
                .hideFromStat(false)
                .build();

        when(integrationService.handleBet(any(BetRequest.class)))
                .thenReturn(Mono.just(response));

        // Execute and verify - should call service and return success
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.balance").isEqualTo("900.00")
                .jsonPath("$.hideFromStat").isEqualTo(false);

        // Verify that handleBet WAS called (balance changed)
        Mockito.verify(integrationService, Mockito.times(1)).handleBet(any(BetRequest.class));
    }

    @Test
    void testRollbackWebhook_OriginalBetNotFound() throws Exception {
        // Prepare rollback request
        RollbackRequest request = RollbackRequest.builder()
                .action("rollback")
                .token("session-token")
                .gameMode("crash")
                .data(RollbackRequestData.builder()
                        .currency("ETB")
                        .operator("test-operator")
                        .amount("100.00")
                        .userId("1")
                        .transactionId(UUID.randomUUID())
                        .debitId(UUID.randomUUID()) // This debitId doesn't exist in the database
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response - original BET transaction not found
        String errorResponseJson = objectMapper.writeValueAsString(
                ErrorResponse.builder()
                        .code("DEBIT_TRANSACTION_NOT_FOUND")
                        .message("Related debit transaction was not found")
                        .build()
        );

        when(integrationService.handleRollback(any(RollbackRequest.class)))
                .thenReturn(Mono.just(errorResponseJson));

        // Execute and verify
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("DEBIT_TRANSACTION_NOT_FOUND")
                .jsonPath("$.message").isEqualTo("Related debit transaction was not found");

        // Verify that handleRollback was called
        Mockito.verify(integrationService, Mockito.times(1)).handleRollback(any(RollbackRequest.class));
    }

    @Test
    void testRollbackWebhook_SuccessWithValidBet() throws Exception {
        // Prepare rollback request
        RollbackRequest request = RollbackRequest.builder()
                .action("rollback")
                .token("session-token")
                .gameMode("crash")
                .data(RollbackRequestData.builder()
                        .currency("ETB")
                        .operator("test-operator")
                        .amount("100.00")
                        .userId("1")
                        .transactionId(UUID.randomUUID())
                        .debitId(UUID.randomUUID()) // Assume this exists
                        .gameId(UUID.randomUUID())
                        .isFinished(true)
                        .build())
                .build();

        String requestBody = objectMapper.writeValueAsString(request);
        String signature = generateSignature(requestBody);

        // Mock service response - successful rollback
        String successResponseJson = objectMapper.writeValueAsString(
                RollbackResponse.builder()
                        .code("OK")
                        .balance("1100.00")
                        .build()
        );

        when(integrationService.handleRollback(any(RollbackRequest.class)))
                .thenReturn(Mono.just(successResponseJson));

        // Execute and verify
        webTestClient.post()
                .uri("/webhooks/golden-eggs")
                .header("X-REQUEST-SIGN", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("OK")
                .jsonPath("$.balance").isEqualTo("1100.00");

        // Verify that handleRollback was called
        Mockito.verify(integrationService, Mockito.times(1)).handleRollback(any(RollbackRequest.class));
    }

    private String generateSignature(String body) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(body.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
