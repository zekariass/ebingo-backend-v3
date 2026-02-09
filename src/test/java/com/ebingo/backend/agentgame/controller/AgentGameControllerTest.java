package com.ebingo.backend.agentgame.controller;

import com.ebingo.backend.agentgame.dto.AgentGameResponse;
import com.ebingo.backend.agentgame.dto.CreateAgentGameRequest;
import com.ebingo.backend.agentgame.enums.GameCategory;
import com.ebingo.backend.agentgame.service.AgentGameService;
import com.ebingo.backend.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AgentGameController
 */
@ExtendWith(MockitoExtension.class)
class AgentGameControllerTest {

    private WebTestClient webTestClient;

    @Mock
    private AgentGameService agentGameService;

    @InjectMocks
    private AgentGameController agentGameController;

    private AgentGameResponse testResponse;
    private Long testAgentId;
    private Long testId;

    @BeforeEach
    void setUp() {
        // Create WebTestClient with standalone setup
        webTestClient = WebTestClient.bindToController(agentGameController).build();
        
        testAgentId = 1L;
        testId = 100L;
        Instant now = Instant.now();

        testResponse = AgentGameResponse.builder()
                .id(testId)
                .agentId(testAgentId)
                .gameCategory(GameCategory.BINGO)
                .gameTypes("CLASSIC,SPEED")
                .isEnabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void getAgentGames_ShouldReturnAllGames() {
        // Arrange
        AgentGameResponse externalGamesResponse = AgentGameResponse.builder()
                .id(101L)
                .agentId(testAgentId)
                .gameCategory(GameCategory.EXTERNAL_GAMES)
                .gameTypes("GOLDEN_EGGS")
                .isEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(agentGameService.getAgentGames(testAgentId))
                .thenReturn(Flux.just(testResponse, externalGamesResponse));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/agent-games")
                        .queryParam("agentId", testAgentId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getStatusCode()).isEqualTo(200);
                    assertThat(response.getData()).isInstanceOf(List.class);
                });
    }

    @Test
    void getAgentGames_WithEnabledOnlyFilter_ShouldReturnEnabledGames() {
        // Arrange
        when(agentGameService.getEnabledAgentGames(testAgentId))
                .thenReturn(Flux.just(testResponse));

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/agent-games")
                        .queryParam("agentId", testAgentId)
                        .queryParam("enabledOnly", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData()).isInstanceOf(List.class);
                });
    }

    @Test
    void getAgentGameById_WhenExists_ShouldReturnGame() {
        // Arrange
        when(agentGameService.getAgentGameById(testId))
                .thenReturn(Mono.just(testResponse));

        // Act & Assert
        webTestClient.get()
                .uri("/api/agent-games/{id}", testId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApiResponse.class)
                .value(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getStatusCode()).isEqualTo(200);
                    assertThat(response.getMessage()).isEqualTo("Agent game retrieved successfully");
                });
    }

    @Test
    void getAgentGameById_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(agentGameService.getAgentGameById(testId))
                .thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.get()
                .uri("/api/agent-games/{id}", testId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiResponse.class)
                .value(response -> {
                    assertThat(response.isSuccess()).isFalse();
                    assertThat(response.getStatusCode()).isEqualTo(404);
                    assertThat(response.getMessage()).isEqualTo("Agent game not found");
                });
    }

    @Test
    void createAgentGame_WithValidRequest_ShouldReturnCreated() {
        // Arrange
        CreateAgentGameRequest request = CreateAgentGameRequest.builder()
                .agentId(testAgentId)
                .gameCategory(GameCategory.EXTERNAL_GAMES)
                .gameTypes("GOLDEN_EGGS")
                .isEnabled(true)
                .build();

        AgentGameResponse createdResponse = AgentGameResponse.builder()
                .id(102L)
                .agentId(testAgentId)
                .gameCategory(GameCategory.EXTERNAL_GAMES)
                .gameTypes("GOLDEN_EGGS")
                .isEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(agentGameService.createAgentGame(any(CreateAgentGameRequest.class)))
                .thenReturn(Mono.just(createdResponse));

        // Act & Assert
        webTestClient.post()
                .uri("/api/agent-games")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AgentGameResponse.class)
                .isEqualTo(createdResponse);
    }

    @Test
    void createAgentGame_WhenAlreadyExists_ShouldReturnBadRequest() {
        // Arrange
        CreateAgentGameRequest request = CreateAgentGameRequest.builder()
                .agentId(testAgentId)
                .gameCategory(GameCategory.BINGO)
                .gameTypes("CLASSIC")
                .isEnabled(true)
                .build();

        when(agentGameService.createAgentGame(any(CreateAgentGameRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Agent game association already exists")));

        // Act & Assert
        webTestClient.post()
                .uri("/api/agent-games")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createAgentGame_WithInvalidRequest_ShouldReturnBadRequest() {
        // Arrange - missing required fields
        CreateAgentGameRequest invalidRequest = CreateAgentGameRequest.builder()
                .isEnabled(true)
                .build();

        // Act & Assert
        webTestClient.post()
                .uri("/api/agent-games")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateAgentGameStatus_WhenExists_ShouldReturnUpdated() {
        // Arrange
        AgentGameResponse updatedResponse = AgentGameResponse.builder()
                .id(testId)
                .agentId(testAgentId)
                .gameCategory(GameCategory.BINGO)
                .gameTypes("CLASSIC,SPEED")
                .isEnabled(false)
                .createdAt(testResponse.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        when(agentGameService.updateAgentGameStatus(testId, false))
                .thenReturn(Mono.just(updatedResponse));

        // Act & Assert
        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/agent-games/{id}/status")
                        .queryParam("isEnabled", false)
                        .build(testId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AgentGameResponse.class)
                .isEqualTo(updatedResponse);
    }

    @Test
    void updateAgentGameStatus_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(agentGameService.updateAgentGameStatus(testId, false))
                .thenReturn(Mono.error(new IllegalArgumentException("Agent game not found")));

        // Act & Assert
        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/agent-games/{id}/status")
                        .queryParam("isEnabled", false)
                        .build(testId))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteAgentGame_WhenExists_ShouldReturnNoContent() {
        // Arrange
        when(agentGameService.deleteAgentGame(testId))
                .thenReturn(Mono.empty());

        // Act & Assert
        webTestClient.delete()
                .uri("/api/agent-games/{id}", testId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteAgentGame_WhenNotExists_ShouldReturn404() {
        // Arrange
        when(agentGameService.deleteAgentGame(testId))
                .thenReturn(Mono.error(new IllegalArgumentException("Agent game not found")));

        // Act & Assert
        webTestClient.delete()
                .uri("/api/agent-games/{id}", testId)
                .exchange()
                .expectStatus().isNotFound();
    }
}
