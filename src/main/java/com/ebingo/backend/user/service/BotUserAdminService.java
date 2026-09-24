package com.ebingo.backend.user.service;

import com.ebingo.backend.agent.repository.AgentRepository;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.system.exceptions.DataIntegrityException;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.constant.BotNamePool;
import com.ebingo.backend.user.dto.BotUserBulkCreateDto;
import com.ebingo.backend.user.dto.BotUserBulkCreateResultDto;
import com.ebingo.backend.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bulk creation of bot user profiles and their wallets for admin use.
 *
 * ID pattern (matches existing robot seed migrations):
 *   user_profile.id = user_profile.telegram_id = wallet.id = wallet.user_profile_id
 *   = startId + i, for i in [0, count)
 * Phone numbers: startPhone + i (Ethiopian format, e.g. 251900017013).
 *
 * All inserts run in a single transaction; any failure rolls everything back.
 * After the inserts the BIGSERIAL sequences of both tables are advanced past
 * the highest inserted id so future auto-generated ids cannot collide.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotUserAdminService {

    private static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("1000000000");

    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;
    private final AgentRepository agentRepository;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;

    public Mono<BotUserBulkCreateResultDto> createBotUsers(BotUserBulkCreateDto dto) {
        int count = dto.getCount();
        long startId = dto.getStartId();
        long endId = startId + count - 1;
        long startPhone = dto.getStartPhone();
        long endPhone = startPhone + count - 1;
        BigDecimal balance = dto.getInitialBalance() != null ? dto.getInitialBalance() : DEFAULT_INITIAL_BALANCE;
        // Shuffled copy: bots are assigned names[i], so each bot gets a
        // distinct random name as long as count <= pool size (516 built-in).
        List<BotUserBulkCreateDto.BotName> names = new ArrayList<>(
                (dto.getNames() != null && !dto.getNames().isEmpty()) ? dto.getNames() : BotNamePool.NAMES);
        Collections.shuffle(names);

        return agentRepository.existsById(dto.getAgentId())
                .flatMap(agentExists -> {
                    if (!agentExists) {
                        return Mono.error(new ResourceNotFoundException(
                                "Agent not found with ID: " + dto.getAgentId()));
                    }
                    return checkConflicts(startId, endId, startPhone, endPhone);
                })
                .then(Mono.defer(() -> {
                    LocalDateTime profileNow = LocalDateTime.now();
                    Instant walletNow = Instant.now();

                    return Flux.range(0, count)
                            .concatMap(i -> insertOne(dto, names, balance, profileNow, walletNow, i))
                            .then(syncSequences())
                            .thenReturn(BotUserBulkCreateResultDto.builder()
                                    .createdCount(count)
                                    .firstId(startId)
                                    .lastId(endId)
                                    .firstPhone(String.valueOf(startPhone))
                                    .lastPhone(String.valueOf(endPhone))
                                    .botRoomId(dto.getBotRoomId())
                                    .agentId(dto.getAgentId())
                                    .initialBalance(balance)
                                    .build());
                }))
                .as(transactionalOperator::transactional)
                .doOnSubscribe(s -> log.info(
                        "Bulk-creating {} bot users: ids [{}..{}], phones [{}..{}], botRoomId={}, agentId={}",
                        count, startId, endId, startPhone, endPhone, dto.getBotRoomId(), dto.getAgentId()))
                .doOnSuccess(r -> log.info("Created {} bot users with wallets (ids {}..{})",
                        r.getCreatedCount(), r.getFirstId(), r.getLastId()))
                .doOnError(e -> log.error("Bulk bot user creation failed, transaction rolled back", e));
    }

    private Mono<Void> checkConflicts(long startId, long endId, long startPhone, long endPhone) {
        return Mono.zip(
                        userProfileRepository.countConflictingBotUsers(
                                startId, endId, String.valueOf(startPhone), String.valueOf(endPhone)),
                        walletRepository.countConflictingWallets(startId, endId))
                .flatMap(tuple -> {
                    long userConflicts = tuple.getT1();
                    long walletConflicts = tuple.getT2();
                    if (userConflicts > 0 || walletConflicts > 0) {
                        return Mono.error(new DataIntegrityException(String.format(
                                "ID/phone range already in use: %d user_profile and %d wallet rows conflict " +
                                        "with ids [%d..%d] / phones [%d..%d]. Nothing was inserted.",
                                userConflicts, walletConflicts, startId, endId, startPhone, endPhone)));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> insertOne(BotUserBulkCreateDto dto,
                                 List<BotUserBulkCreateDto.BotName> names,
                                 BigDecimal balance,
                                 LocalDateTime profileNow,
                                 Instant walletNow,
                                 int i) {
        long id = dto.getStartId() + i;
        String phone = String.valueOf(dto.getStartPhone() + i);
        BotUserBulkCreateDto.BotName name = names.get(i % names.size());

        return userProfileRepository.insertBotUser(
                        id, id,
                        name.getFirstName(), name.getLastName(), name.getNickname(),
                        phone, dto.getBotRoomId(), dto.getAgentId(),
                        profileNow, profileNow)
                .then(walletRepository.insertWallet(
                        id, id, dto.getAgentId(), balance, walletNow, walletNow))
                .then();
    }

    /**
     * Advances both BIGSERIAL sequences past the current max(id) so that rows
     * inserted later without explicit ids cannot collide with these bot rows.
     * Never moves a sequence backwards.
     */
    private Mono<Void> syncSequences() {
        return databaseClient.sql(
                        "SELECT setval('user_profile_id_seq', " +
                                "GREATEST((SELECT COALESCE(MAX(id), 1) FROM user_profile), " +
                                "(SELECT last_value FROM user_profile_id_seq)))")
                .then()
                .then(databaseClient.sql(
                        "SELECT setval('wallet_id_seq', " +
                                "GREATEST((SELECT COALESCE(MAX(id), 1) FROM wallet), " +
                                "(SELECT last_value FROM wallet_id_seq)))")
                        .then());
    }
}
