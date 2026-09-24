package com.ebingo.backend.user.service;

import com.ebingo.backend.agent.repository.AgentRepository;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.system.exceptions.DataIntegrityException;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.dto.BotUserBulkCreateDto;
import com.ebingo.backend.user.dto.BotUserBulkCreateResultDto;
import com.ebingo.backend.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotUserAdminServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private DatabaseClient databaseClient;
    @Mock
    private DatabaseClient.GenericExecuteSpec executeSpec;
    @Mock
    private TransactionalOperator transactionalOperator;

    private BotUserAdminService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Pass-through transactional operator: run the wrapped publisher as-is
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // DatabaseClient fluent chain used for sequence sync (lenient: not reached on error paths)
        lenient().when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        lenient().when(executeSpec.then()).thenReturn(Mono.empty());

        service = new BotUserAdminService(
                userProfileRepository, walletRepository, agentRepository,
                databaseClient, transactionalOperator);
    }

    private BotUserBulkCreateDto baseDto() {
        return BotUserBulkCreateDto.builder()
                .startId(1000017013L)
                .startPhone(251900017013L)
                .botRoomId(16L)
                .agentId(1L)
                .count(3)
                .build();
    }

    private void stubHappyPath() {
        when(agentRepository.existsById(anyLong())).thenReturn(Mono.just(true));
        when(userProfileRepository.countConflictingBotUsers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(Mono.just(0L));
        when(walletRepository.countConflictingWallets(anyLong(), anyLong()))
                .thenReturn(Mono.just(0L));
        when(userProfileRepository.insertBotUser(anyLong(), anyLong(), anyString(), any(), any(),
                anyString(), anyLong(), anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Mono.just(1));
        when(walletRepository.insertWallet(anyLong(), anyLong(), anyLong(), any(BigDecimal.class),
                any(Instant.class), any(Instant.class)))
                .thenReturn(Mono.just(1));
    }

    @Test
    void createBotUsers_insertsProfilesAndWalletsWithIncrementingIds() {
        stubHappyPath();

        StepVerifier.create(service.createBotUsers(baseDto()))
                .assertNext(result -> {
                    assertEquals(3, result.getCreatedCount());
                    assertEquals(1000017013L, result.getFirstId());
                    assertEquals(1000017015L, result.getLastId());
                    assertEquals("251900017013", result.getFirstPhone());
                    assertEquals("251900017015", result.getLastPhone());
                    assertEquals(16L, result.getBotRoomId());
                    assertEquals(1L, result.getAgentId());
                    assertEquals(new BigDecimal("1000000000"), result.getInitialBalance());
                })
                .verifyComplete();

        // user_profile.id == telegram_id, incremented per record
        verify(userProfileRepository).insertBotUser(eq(1000017013L), eq(1000017013L),
                anyString(), any(), any(), eq("251900017013"), eq(16L), eq(1L),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userProfileRepository).insertBotUser(eq(1000017014L), eq(1000017014L),
                anyString(), any(), any(), eq("251900017014"), eq(16L), eq(1L),
                any(LocalDateTime.class), any(LocalDateTime.class));
        verify(userProfileRepository).insertBotUser(eq(1000017015L), eq(1000017015L),
                anyString(), any(), any(), eq("251900017015"), eq(16L), eq(1L),
                any(LocalDateTime.class), any(LocalDateTime.class));

        // wallet.id == user_profile_id == same id
        verify(walletRepository).insertWallet(eq(1000017013L), eq(1000017013L), eq(1L),
                eq(new BigDecimal("1000000000")), any(Instant.class), any(Instant.class));
        verify(walletRepository, times(3)).insertWallet(anyLong(), anyLong(), eq(1L),
                any(BigDecimal.class), any(Instant.class), any(Instant.class));

        // sequences synced for both tables
        verify(databaseClient, times(2)).sql(anyString());
    }

    @Test
    void createBotUsers_usesProvidedBalanceAndPicksNamesFromPool() {
        stubHappyPath();
        BotUserBulkCreateDto dto = baseDto();
        dto.setInitialBalance(new BigDecimal("500"));
        dto.setNames(List.of(new BotUserBulkCreateDto.BotName("Lensa", "Merga", "Lensi")));

        StepVerifier.create(service.createBotUsers(dto))
                .assertNext(result -> assertEquals(new BigDecimal("500"), result.getInitialBalance()))
                .verifyComplete();

        ArgumentCaptor<String> firstName = ArgumentCaptor.forClass(String.class);
        verify(userProfileRepository, times(3)).insertBotUser(anyLong(), anyLong(),
                firstName.capture(), eq("Merga"), eq("Lensi"),
                anyString(), anyLong(), anyLong(),
                any(LocalDateTime.class), any(LocalDateTime.class));
        assertTrue(firstName.getAllValues().stream().allMatch("Lensa"::equals));

        verify(walletRepository, times(3)).insertWallet(anyLong(), anyLong(), anyLong(),
                eq(new BigDecimal("500")), any(Instant.class), any(Instant.class));
    }

    @Test
    void createBotUsers_failsWhenAgentMissing() {
        when(agentRepository.existsById(99L)).thenReturn(Mono.just(false));
        BotUserBulkCreateDto dto = baseDto();
        dto.setAgentId(99L);

        StepVerifier.create(service.createBotUsers(dto))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verifyNoInteractions(userProfileRepository, walletRepository);
    }

    @Test
    void createBotUsers_failsOnIdConflict_andInsertsNothing() {
        when(agentRepository.existsById(anyLong())).thenReturn(Mono.just(true));
        when(userProfileRepository.countConflictingBotUsers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(Mono.just(2L));
        when(walletRepository.countConflictingWallets(anyLong(), anyLong()))
                .thenReturn(Mono.just(0L));

        StepVerifier.create(service.createBotUsers(baseDto()))
                .expectError(DataIntegrityException.class)
                .verify();

        verify(userProfileRepository, never()).insertBotUser(anyLong(), anyLong(), anyString(), any(), any(),
                anyString(), anyLong(), anyLong(), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(walletRepository, never()).insertWallet(anyLong(), anyLong(), anyLong(), any(BigDecimal.class),
                any(Instant.class), any(Instant.class));
    }

    @Test
    void createBotUsers_wrapsWorkInSingleTransaction() {
        stubHappyPath();

        StepVerifier.create(service.createBotUsers(baseDto()))
                .expectNextCount(1)
                .verifyComplete();

        verify(transactionalOperator).transactional(any(Mono.class));
    }
}
