package com.ebingo.backend.agentgame.service;

import com.ebingo.backend.agentgame.dto.AgentGameResponse;
import com.ebingo.backend.agentgame.dto.CreateAgentGameRequest;
import com.ebingo.backend.agentgame.entity.AgentGame;
import com.ebingo.backend.agentgame.enums.GameCategory;
import com.ebingo.backend.agentgame.repository.AgentGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentGameService
 */
@ExtendWith(MockitoExtension.class)
class AgentGameServiceTest {

    @Mock
    private AgentGameRepository agentGameRepository;

    @InjectMocks
    private AgentGameService agentGameService;

    private AgentGame testAgentGame;
    private Long testAgentId;
    private Long testId;

    @BeforeEach
    void setUp() {
        testAgentId = 1L;
        testId = 100L;
        Instant now = Instant.now();

        testAgentGame = AgentGame.builder()
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
    void getAgentGames_ShouldReturnAllGamesForAgent() {
        // Arrange
        AgentGame externalGame = AgentGame.builder()
                .id(101L)
                .agentId(testAgentId)
                .gameCategory(GameCategory.EXTERNAL_GAMES)
                .gameTypes("GOLDEN_EGGS")
                .isEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(agentGameRepository.findByAgentId(testAgentId))
                .thenReturn(Flux.just(testAgentGame, externalGame));

        // Act & Assert
        StepVerifier.create(agentGameService.getAgentGames(testAgentId))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(testId);
                    assertThat(response.getAgentId()).isEqualTo(testAgentId);
                    assertThat(response.getGameCategory()).isEqualTo(GameCategory.BINGO);
                    assertThat(response.getGameTypes()).isEqualTo("CLASSIC,SPEED");
                    assertThat(response.getIsEnabled()).isTrue();
                })
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(101L);
                    assertThat(response.getGameCategory()).isEqualTo(GameCategory.EXTERNAL_GAMES);
                    assertThat(response.getGameTypes()).isEqualTo("GOLDEN_EGGS");
                })
                .verifyComplete();

        verify(agentGameRepository).findByAgentId(testAgentId);
    }

    @Test
    void getEnabledAgentGames_ShouldReturnOnlyEnabledGames() {
        // Arrange
        when(agentGameRepository.findByAgentIdAndIsEnabled(testAgentId, true))
                .thenReturn(Flux.just(testAgentGame));

        // Act & Assert
        StepVerifier.create(agentGameService.getEnabledAgentGames(testAgentId))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(testId);
                    assertThat(response.getIsEnabled()).isTrue();
                })
                .verifyComplete();

        verify(agentGameRepository).findByAgentIdAndIsEnabled(testAgentId, true);
    }

    @Test
    void getAgentGameById_WhenExists_ShouldReturnGame() {
        // Arrange
        when(agentGameRepository.findById(testId))
                .thenReturn(Mono.just(testAgentGame));

        // Act & Assert
        StepVerifier.create(agentGameService.getAgentGameById(testId))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(testId);
                    assertThat(response.getAgentId()).isEqualTo(testAgentId);
                    assertThat(response.getGameCategory()).isEqualTo(GameCategory.BINGO);
                    assertThat(response.getGameTypes()).isEqualTo("CLASSIC,SPEED");
                })
                .verifyComplete();

        verify(agentGameRepository).findById(testId);
    }

    @Test
    void getAgentGameById_WhenNotExists_ShouldReturnEmpty() {
        // Arrange
        when(agentGameRepository.findById(testId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(agentGameService.getAgentGameById(testId))
                .verifyComplete();

        verify(agentGameRepository).findById(testId);
    }

    @Test
    void createAgentGame_WhenNotExists_ShouldCreateSuccessfully() {
        // Arrange
        CreateAgentGameRequest request = CreateAgentGameRequest.builder()
                .agentId(testAgentId)
                .gameCategory(GameCategory.EXTERNAL_GAMES)
                .gameTypes("GOLDEN_EGGS")
                .isEnabled(true)
                .build();

        when(agentGameRepository.findByAgentIdAndGameCategory(testAgentId, GameCategory.EXTERNAL_GAMES))
                .thenReturn(Mono.empty());
        when(agentGameRepository.save(any(AgentGame.class)))
                .thenAnswer(invocation -> {
                    AgentGame saved = invocation.getArgument(0);
                    saved.setId(102L);
                    return Mono.just(saved);
                });

        // Act & Assert
        StepVerifier.create(agentGameService.createAgentGame(request))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(102L);
                    assertThat(response.getAgentId()).isEqualTo(testAgentId);
                    assertThat(response.getGameCategory()).isEqualTo(GameCategory.EXTERNAL_GAMES);
                    assertThat(response.getGameTypes()).isEqualTo("GOLDEN_EGGS");
                    assertThat(response.getIsEnabled()).isTrue();
                    assertThat(response.getCreatedAt()).isNotNull();
                    assertThat(response.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(agentGameRepository).findByAgentIdAndGameCategory(testAgentId, GameCategory.EXTERNAL_GAMES);
        verify(agentGameRepository).save(any(AgentGame.class));
    }

    @Test
    void createAgentGame_WhenAlreadyExists_ShouldReturnError() {
        // Arrange
        CreateAgentGameRequest request = CreateAgentGameRequest.builder()
                .agentId(testAgentId)
                .gameCategory(GameCategory.BINGO)
                .gameTypes("CLASSIC")
                .isEnabled(true)
                .build();

        when(agentGameRepository.findByAgentIdAndGameCategory(testAgentId, GameCategory.BINGO))
                .thenReturn(Mono.just(testAgentGame));

        // Act & Assert
        StepVerifier.create(agentGameService.createAgentGame(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("already exists"))
                .verify();

        verify(agentGameRepository).findByAgentIdAndGameCategory(testAgentId, GameCategory.BINGO);
        verify(agentGameRepository, never()).save(any(AgentGame.class));
    }

    @Test
    void updateAgentGameStatus_WhenExists_ShouldUpdateSuccessfully() {
        // Arrange
        when(agentGameRepository.findById(testId))
                .thenReturn(Mono.just(testAgentGame));
        when(agentGameRepository.save(any(AgentGame.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Act & Assert
        StepVerifier.create(agentGameService.updateAgentGameStatus(testId, false))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo(testId);
                    assertThat(response.getIsEnabled()).isFalse();
                    assertThat(response.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();

        verify(agentGameRepository).findById(testId);
        verify(agentGameRepository).save(any(AgentGame.class));
    }

    @Test
    void updateAgentGameStatus_WhenNotExists_ShouldReturnError() {
        // Arrange
        when(agentGameRepository.findById(testId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(agentGameService.updateAgentGameStatus(testId, false))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("not found"))
                .verify();

        verify(agentGameRepository).findById(testId);
        verify(agentGameRepository, never()).save(any(AgentGame.class));
    }

    @Test
    void deleteAgentGame_WhenExists_ShouldDeleteSuccessfully() {
        // Arrange
        when(agentGameRepository.findById(testId))
                .thenReturn(Mono.just(testAgentGame));
        when(agentGameRepository.deleteById(testId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(agentGameService.deleteAgentGame(testId))
                .verifyComplete();

        verify(agentGameRepository).findById(testId);
        verify(agentGameRepository).deleteById(testId);
    }

    @Test
    void deleteAgentGame_WhenNotExists_ShouldReturnError() {
        // Arrange
        when(agentGameRepository.findById(testId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(agentGameService.deleteAgentGame(testId))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("not found"))
                .verify();

        verify(agentGameRepository).findById(testId);
        verify(agentGameRepository, never()).deleteById(anyLong());
    }

    @Test
    void isGameCategoryEnabledForAgent_WhenEnabled_ShouldReturnTrue() {
        // Arrange
        when(agentGameRepository.existsByAgentIdAndGameCategoryAndIsEnabled(testAgentId, GameCategory.BINGO, true))
                .thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(agentGameService.isGameCategoryEnabledForAgent(testAgentId, GameCategory.BINGO))
                .expectNext(true)
                .verifyComplete();

        verify(agentGameRepository).existsByAgentIdAndGameCategoryAndIsEnabled(testAgentId, GameCategory.BINGO, true);
    }

    @Test
    void isGameCategoryEnabledForAgent_WhenNotEnabled_ShouldReturnFalse() {
        // Arrange
        when(agentGameRepository.existsByAgentIdAndGameCategoryAndIsEnabled(testAgentId, GameCategory.EXTERNAL_GAMES, true))
                .thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(agentGameService.isGameCategoryEnabledForAgent(testAgentId, GameCategory.EXTERNAL_GAMES))
                .expectNext(false)
                .verifyComplete();

        verify(agentGameRepository).existsByAgentIdAndGameCategoryAndIsEnabled(testAgentId, GameCategory.EXTERNAL_GAMES, true);
    }
}
