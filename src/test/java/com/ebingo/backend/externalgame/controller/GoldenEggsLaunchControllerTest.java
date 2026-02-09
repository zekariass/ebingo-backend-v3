package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.externalgame.dto.LaunchRequest;
import com.ebingo.backend.externalgame.dto.LaunchResponse;
import com.ebingo.backend.externalgame.service.GoldenEggsIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(GoldenEggsController.class)
@ContextConfiguration(classes = {GoldenEggsController.class, GoldenEggsLaunchControllerTest.TestConfig.class})
class GoldenEggsLaunchControllerTest {

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
    }

    @Test
    void testLaunchGame_Success() throws Exception {
        // Prepare request
        LaunchRequest request = LaunchRequest.builder()
                .agentId(1L)
                .gameMode("crash")
                .currency("USD")
                .initData("valid-init-data")
                .build();

        // Mock service response
        LaunchResponse response = LaunchResponse.builder()
                .url("https://api.golden-eggs.games/game/crash?token=test-token&operator=op1&currency=USD")
                .build();

        when(integrationService.generateLaunchUrl(any(LaunchRequest.class)))
                .thenReturn(Mono.just(response));

        // Execute and verify
        webTestClient.post()
                .uri("/external-games/golden-eggs/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.url").exists()
                .jsonPath("$.url").value(url -> url.toString().contains("token="));
    }

    @Test
    void testLaunchGame_InvalidInitData() throws Exception {
        // Prepare request
        LaunchRequest request = LaunchRequest.builder()
                .agentId(1L)
                .gameMode("crash")
                .currency("USD")
                .initData("invalid-init-data")
                .build();

        // Mock service to throw InvalidInitDataException
        when(integrationService.generateLaunchUrl(any(LaunchRequest.class)))
                .thenReturn(Mono.error(new GoldenEggsIntegrationService.InvalidInitDataException("Invalid initData")));

        // Execute and verify
        webTestClient.post()
                .uri("/external-games/golden-eggs/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_INIT_DATA");
    }

    @Test
    void testLaunchGame_AgentNotFound() throws Exception {
        // Prepare request
        LaunchRequest request = LaunchRequest.builder()
                .agentId(999L)
                .gameMode("crash")
                .currency("USD")
                .initData("valid-init-data")
                .build();

        // Mock service to throw AgentNotFoundException
        when(integrationService.generateLaunchUrl(any(LaunchRequest.class)))
                .thenReturn(Mono.error(new GoldenEggsIntegrationService.AgentNotFoundException("Agent not found")));

        // Execute and verify
        webTestClient.post()
                .uri("/external-games/golden-eggs/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AGENT_NOT_FOUND");
    }

    @Test
    void testLaunchGame_MissingRequiredFields() throws Exception {
        // Prepare request with missing fields
        LaunchRequest request = LaunchRequest.builder()
                .gameMode("crash")
                // Missing agentId, currency, initData
                .build();

        // Execute and verify - should fail validation
        webTestClient.post()
                .uri("/external-games/golden-eggs/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void testLaunchGame_TwoSequentialCalls_DifferentTokens() throws Exception {
        // Prepare request
        LaunchRequest request = LaunchRequest.builder()
                .agentId(1L)
                .gameMode("crash")
                .currency("USD")
                .initData("valid-init-data")
                .build();

        // Mock service to return different tokens
        LaunchResponse response1 = LaunchResponse.builder()
                .url("https://api.golden-eggs.games/game/crash?token=token1&operator=op1&currency=USD")
                .build();

        LaunchResponse response2 = LaunchResponse.builder()
                .url("https://api.golden-eggs.games/game/crash?token=token2&operator=op1&currency=USD")
                .build();

        when(integrationService.generateLaunchUrl(any(LaunchRequest.class)))
                .thenReturn(Mono.just(response1))
                .thenReturn(Mono.just(response2));

        // First call
        String url1 = webTestClient.post()
                .uri("/external-games/golden-eggs/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LaunchResponse.class)
                .returnResult()
                .getResponseBody()
                .getUrl();

        // Second call
        String url2 = webTestClient.post()
                .uri("/external-games/golden-eggs/launch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LaunchResponse.class)
                .returnResult()
                .getResponseBody()
                .getUrl();

        // Verify tokens are different
        assert !url1.equals(url2) : "Tokens should be different for each launch";
    }
}
