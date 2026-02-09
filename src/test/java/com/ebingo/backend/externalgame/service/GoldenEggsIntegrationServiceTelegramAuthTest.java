//package com.ebingo.backend.externalgame.service;
//
//import com.ebingo.backend.agent.entity.Agent;
//import com.ebingo.backend.agent.repository.AgentRepository;
//import com.ebingo.backend.common.telegram.TelegramAuthVerifier;
//import com.ebingo.backend.externalgame.config.GoldenEggsConfig;
//import com.ebingo.backend.externalgame.dto.LaunchRequest;
//import com.ebingo.backend.externalgame.dto.LaunchResponse;
//import com.ebingo.backend.externalgame.entity.ExternalGameAuthToken;
//import com.ebingo.backend.externalgame.repository.ExternalGameAuthTokenRepository;
//import com.ebingo.backend.user.entity.UserProfile;
//import com.ebingo.backend.user.repository.UserProfileRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.transaction.reactive.TransactionalOperator;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class GoldenEggsIntegrationServiceTelegramAuthTest {
//
//    @Mock
//    private GoldenEggsConfig config;
//
//    @Mock
//    private AgentRepository agentRepository;
//
//    @Mock
//    private TelegramAuthVerifier telegramAuthVerifier;
//
//    @Mock
//    private UserProfileRepository userProfileRepository;
//
//    @Mock
//    private ExternalGameAuthTokenRepository authTokenRepository;
//
//    @Mock
//    private TransactionalOperator transactionalOperator;
//
//    private GoldenEggsIntegrationService service;
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() {
//        objectMapper = new ObjectMapper();
//
//        // Mock config
//        when(config.getOperatorId()).thenReturn("test-operator");
//        when(config.getApiBaseUrl()).thenReturn("https://api.golden-eggs.games");
//        when(config.getLaunchTokenTtlMinutes()).thenReturn(20);
//        when(config.getInitDataMaxAgeSeconds()).thenReturn(600);
//
//        // Create service with mocked dependencies
//        service = new GoldenEggsIntegrationService(
//                config,
//                null, // WebClient not needed for these tests
//                authTokenRepository,
//                null, // sessionRepository
//                null, // txnRepository
//                null, // walletService
//                null, // walletRepository
//                userProfileRepository,
//                agentRepository,
//                telegramAuthVerifier,
//                transactionalOperator,
//                objectMapper,
//                new java.security.SecureRandom()
//        );
//    }
//
//    @Test
//    void testGenerateLaunchUrl_Success() {
//        // Arrange
//        Long agentId = 1L;
//        Long telegramUserId = 123456789L;
//        String botToken = "test-bot-token";
//        String initData = "valid-init-data";
//
//        LaunchRequest request = LaunchRequest.builder()
//                .agentId(agentId)
//                .gameMode("crash")
//                .currency("USD")
//                .initData(initData)
//                .build();
//
//        Agent agent = new Agent();
//        agent.setId(agentId);
//        agent.setBotToken(botToken);
//
//        Map<String, String> telegramParams = new HashMap<>();
//        telegramParams.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"Test\",\"username\":\"testuser\"}");
//        telegramParams.put("auth_date", String.valueOf(System.currentTimeMillis() / 1000));
//
//        UserProfile existingUser = new UserProfile();
//        existingUser.setId(100L);
//        existingUser.setTelegramId(telegramUserId);
//        existingUser.setAgentId(agentId);
//
//        ExternalGameAuthToken savedToken = ExternalGameAuthToken.builder()
//                .token("generated-token")
//                .userId(100L)
//                .operatorId("test-operator")
//                .currency("USD")
//                .gameMode("crash")
//                .expiresAt(Instant.now().plusSeconds(1200))
//                .status("ACTIVE")
//                .createdAt(Instant.now())
//                .build();
//
//        when(agentRepository.findById(agentId)).thenReturn(Mono.just(agent));
//        when(telegramAuthVerifier.verifyInitData(eq(initData), eq(botToken), anyInt()))
//                .thenReturn(Optional.of(telegramParams));
//        when(userProfileRepository.findByTelegramIdAndAgentId(telegramUserId, agentId))
//                .thenReturn(Mono.just(existingUser));
//        when(authTokenRepository.save(any(ExternalGameAuthToken.class)))
//                .thenReturn(Mono.just(savedToken));
//
//        // Act & Assert
//        StepVerifier.create(service.generateLaunchUrl(request))
//                .expectNextMatches(response ->
//                        response.getUrl().contains("token=generated-token") &&
//                        response.getUrl().contains("crash") &&
//                        response.getUrl().contains("USD")
//                )
//                .verifyComplete();
//
//        verify(agentRepository).findById(agentId);
//        verify(telegramAuthVerifier).verifyInitData(eq(initData), eq(botToken), anyInt());
//        verify(userProfileRepository).findByTelegramIdAndAgentId(telegramUserId, agentId);
//        verify(authTokenRepository).save(any(ExternalGameAuthToken.class));
//    }
//
//    @Test
//    void testGenerateLaunchUrl_AgentNotFound() {
//        // Arrange
//        LaunchRequest request = LaunchRequest.builder()
//                .agentId(999L)
//                .gameMode("crash")
//                .currency("USD")
//                .initData("valid-init-data")
//                .build();
//
//        when(agentRepository.findById(999L)).thenReturn(Mono.empty());
//
//        // Act & Assert
//        StepVerifier.create(service.generateLaunchUrl(request))
//                .expectError(GoldenEggsIntegrationService.AgentNotFoundException.class)
//                .verify();
//
//        verify(agentRepository).findById(999L);
//        verify(telegramAuthVerifier, never()).verifyInitData(anyString(), anyString(), anyInt());
//    }
//
//    @Test
//    void testGenerateLaunchUrl_InvalidInitData() {
//        // Arrange
//        Long agentId = 1L;
//        String botToken = "test-bot-token";
//        String initData = "invalid-init-data";
//
//        LaunchRequest request = LaunchRequest.builder()
//                .agentId(agentId)
//                .gameMode("crash")
//                .currency("USD")
//                .initData(initData)
//                .build();
//
//        Agent agent = new Agent();
//        agent.setId(agentId);
//        agent.setBotToken(botToken);
//
//        when(agentRepository.findById(agentId)).thenReturn(Mono.just(agent));
//        when(telegramAuthVerifier.verifyInitData(eq(initData), eq(botToken), anyInt()))
//                .thenReturn(Optional.empty());
//
//        // Act & Assert
//        StepVerifier.create(service.generateLaunchUrl(request))
//                .expectError(GoldenEggsIntegrationService.InvalidInitDataException.class)
//                .verify();
//
//        verify(agentRepository).findById(agentId);
//        verify(telegramAuthVerifier).verifyInitData(eq(initData), eq(botToken), anyInt());
//        verify(userProfileRepository, never()).findByTelegramIdAndAgentId(anyLong(), anyLong());
//    }
//
//    @Test
//    void testGenerateLaunchUrl_CreateNewUser() {
//        // Arrange
//        Long agentId = 1L;
//        Long telegramUserId = 987654321L;
//        String botToken = "test-bot-token";
//        String initData = "valid-init-data";
//
//        LaunchRequest request = LaunchRequest.builder()
//                .agentId(agentId)
//                .gameMode("crash")
//                .currency("USD")
//                .initData(initData)
//                .build();
//
//        Agent agent = new Agent();
//        agent.setId(agentId);
//        agent.setBotToken(botToken);
//
//        Map<String, String> telegramParams = new HashMap<>();
//        telegramParams.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"NewUser\",\"username\":\"newuser\"}");
//        telegramParams.put("auth_date", String.valueOf(System.currentTimeMillis() / 1000));
//
//        UserProfile newUser = new UserProfile();
//        newUser.setId(200L);
//        newUser.setTelegramId(telegramUserId);
//        newUser.setAgentId(agentId);
//        newUser.setFirstName("NewUser");
//        newUser.setNickname("newuser");
//
//        ExternalGameAuthToken savedToken = ExternalGameAuthToken.builder()
//                .token("new-user-token")
//                .userId(200L)
//                .operatorId("test-operator")
//                .currency("USD")
//                .gameMode("crash")
//                .expiresAt(Instant.now().plusSeconds(1200))
//                .status("ACTIVE")
//                .createdAt(Instant.now())
//                .build();
//
//        when(agentRepository.findById(agentId)).thenReturn(Mono.just(agent));
//        when(telegramAuthVerifier.verifyInitData(eq(initData), eq(botToken), anyInt()))
//                .thenReturn(Optional.of(telegramParams));
//        when(userProfileRepository.findByTelegramIdAndAgentId(telegramUserId, agentId))
//                .thenReturn(Mono.empty());
//        when(userProfileRepository.save(any(UserProfile.class)))
//                .thenReturn(Mono.just(newUser));
//        when(authTokenRepository.save(any(ExternalGameAuthToken.class)))
//                .thenReturn(Mono.just(savedToken));
//
//        // Act & Assert
//        StepVerifier.create(service.generateLaunchUrl(request))
//                .expectNextMatches(response ->
//                        response.getUrl().contains("token=new-user-token")
//                )
//                .verifyComplete();
//
//        verify(userProfileRepository).save(any(UserProfile.class));
//        verify(authTokenRepository).save(any(ExternalGameAuthToken.class));
//    }
//
//    @Test
//    void testGenerateLaunchUrl_NoBotToken() {
//        // Arrange
//        Long agentId = 1L;
//
//        LaunchRequest request = LaunchRequest.builder()
//                .agentId(agentId)
//                .gameMode("crash")
//                .currency("USD")
//                .initData("valid-init-data")
//                .build();
//
//        Agent agent = new Agent();
//        agent.setId(agentId);
//        agent.setBotToken(null); // No bot token
//
//        when(agentRepository.findById(agentId)).thenReturn(Mono.just(agent));
//
//        // Act & Assert
//        StepVerifier.create(service.generateLaunchUrl(request))
//                .expectError(GoldenEggsIntegrationService.InvalidConfigurationException.class)
//                .verify();
//
//        verify(agentRepository).findById(agentId);
//        verify(telegramAuthVerifier, never()).verifyInitData(anyString(), anyString(), anyInt());
//    }
//}
