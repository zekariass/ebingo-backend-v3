package com.ebingo.backend.externalgame.service;

import com.ebingo.backend.externalgame.config.GoldenEggsConfig;
import com.ebingo.backend.externalgame.dto.bonus.BonusDTOs.*;
import com.ebingo.backend.externalgame.entity.GoldenEggsBonus;
import com.ebingo.backend.externalgame.entity.GoldenEggsBonusTransaction;
import com.ebingo.backend.externalgame.repository.GoldenEggsBonusRepository;
import com.ebingo.backend.externalgame.repository.GoldenEggsBonusTransactionRepository;
import com.ebingo.backend.externalgame.util.GoldenEggsBonusUtil;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.user.repository.UserProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoldenEggsBonusService {

    private final GoldenEggsBonusRepository bonusRepository;
    private final GoldenEggsBonusTransactionRepository transactionRepository;
    private final ExternalGameWalletService walletService;
    private final WalletRepository walletRepository;
    private final UserProfileRepository userProfileRepository;
    private final GoldenEggsConfig config;
    private final WebClient goldenEggsWebClient;
    private final ObjectMapper objectMapper;

    /**
     * Create a bonus for a user
     * Flow:
     * 1. Save bonus locally with PENDING status
     * 2. Send create request to provider with X-REQUEST-SIGN header
     * 3. Update local status to CREATED (success) or FAILED (error)
     */
    public Mono<CreateBonusResponse> createBonus(String subId, CreateBonusRequest request) {
        UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(config.getAggregatorId(), subId);
        String signature = GoldenEggsBonusUtil.generateRequestSignature(
                subOperatorId.toString(),
                config.getSignatureSecret()
        );

        log.info("Creating bonus: bonusId={}, userId={}, subOperatorId={}", 
                request.getBonusId(), request.getUserId(), subOperatorId);

        Long userId = Long.parseLong(request.getUserId());

        // Step 1: Load user (for agentId + FK validity) then save bonus locally with PENDING status
        return userProfileRepository.findById(userId)
                .flatMap(user -> {
                    GoldenEggsBonus bonus = GoldenEggsBonus.builder()
                            .bonusId(request.getBonusId())
                            .userId(userId)
                            .agentId(user.getAgentId())
                            .subOperatorId(subOperatorId)
                            .gameModes(serializeGameModes(request.getGameModes()))
                            .currency(request.getCurrency())
                            .type(request.getType())
                            .status("PENDING") // Start with PENDING status
                            .bonusQuantity(Integer.parseInt(request.getFreebetConfig().getCount()))
                            .bonusAvailable(Integer.parseInt(request.getFreebetConfig().getCount()))
                            .winSum(BigDecimal.ZERO)
                            .freebetConfig(serializeFreebetConfig(request.getFreebetConfig()))
                            .expiresAt(request.getExpiresAt())
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return bonusRepository.save(bonus);
                })
                .flatMap(savedBonus -> {
                    log.info("Bonus saved locally with PENDING status: bonusId={}", request.getBonusId());
                    // Mark as persisted so subsequent saves issue UPDATE not INSERT
                    savedBonus.setNew(false);

                    // Step 2: Call provider API to create bonus
                    return goldenEggsWebClient.post()
                            .uri("/api/operator/v1/bonuses/" + subOperatorId)
                            .header("X-REQUEST-SIGN", signature)
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(CreateBonusResponse.class)
                            .flatMap(providerResponse -> {
                                // Step 3: Update local status based on provider response
                                if (providerResponse.getStatus() != null && providerResponse.getStatus()) {
                                    savedBonus.setStatus("CREATED");
                                    savedBonus.setUpdatedAt(Instant.now());
                                    log.info("Bonus created successfully at provider: bonusId={}", request.getBonusId());
                                } else {
                                    savedBonus.setStatus("FAILED");
                                    savedBonus.setUpdatedAt(Instant.now());
                                    log.warn("Bonus creation failed at provider: bonusId={}", request.getBonusId());
                                }
                                
                                return bonusRepository.save(savedBonus)
                                        .thenReturn(providerResponse);
                            })
                            .onErrorResume(error -> {
                                // Provider API call failed - update status to FAILED
                                log.error("Failed to create bonus at provider: bonusId={}", request.getBonusId(), error);
                                savedBonus.setStatus("FAILED");
                                savedBonus.setUpdatedAt(Instant.now());
                                
                                return bonusRepository.save(savedBonus)
                                        .thenReturn(CreateBonusResponse.builder().status(false).build());
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error saving bonus locally", e);
                    return Mono.just(CreateBonusResponse.builder().status(false).build());
                });
    }

    /**
     * Fetch bonuses with filters
     */
    public Mono<FetchBonusesResponse> fetchBonuses(String subId, String userId, String status, Integer page, Integer limit) {
        UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(config.getAggregatorId(), subId);
        String signature = GoldenEggsBonusUtil.generateRequestSignature(
                subOperatorId.toString(),
                config.getSignatureSecret()
        );

        log.info("Fetching bonuses: subOperatorId={}, userId={}, status={}", subOperatorId, userId, status);

        StringBuilder uriBuilder = new StringBuilder("/api/operator/v1/bonuses/" + subOperatorId + "?");
        if (userId != null) uriBuilder.append("filter[userId]=").append(userId).append("&");
        if (status != null) uriBuilder.append("filter[status]=").append(status).append("&");
        if (page != null) uriBuilder.append("pageable[page]=").append(page).append("&");
        if (limit != null) uriBuilder.append("pageable[limit]=").append(limit);

        return goldenEggsWebClient.get()
                .uri(uriBuilder.toString())
                .header("X-REQUEST-SIGN", signature)
                .retrieve()
                .bodyToMono(FetchBonusesResponse.class)
                .doOnSuccess(response -> log.info("Fetched {} bonuses", response.getBonuses().size()))
                .onErrorResume(e -> {
                    log.error("Error fetching bonuses", e);
                    return Mono.just(FetchBonusesResponse.builder().build());
                });
    }

    /**
     * View a single bonus
     */
    public Mono<BonusDTO> viewBonus(String subId, UUID bonusId) {
        UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(config.getAggregatorId(), subId);
        String signature = GoldenEggsBonusUtil.generateRequestSignature(
                subOperatorId.toString(),
                config.getSignatureSecret()
        );

        log.info("Viewing bonus: bonusId={}, subOperatorId={}", bonusId, subOperatorId);

        return goldenEggsWebClient.get()
                .uri("/api/operator/v1/bonuses/" + subOperatorId + "/" + bonusId)
                .header("X-REQUEST-SIGN", signature)
                .retrieve()
                .bodyToMono(BonusDTO.class)
                .doOnSuccess(bonus -> log.info("Bonus retrieved: bonusId={}, status={}", bonusId, bonus.getStatus()))
                .onErrorResume(e -> {
                    log.error("Error viewing bonus", e);
                    return Mono.empty();
                });
    }

    /**
     * Cancel a bonus
     */
    public Mono<CancelBonusResponse> cancelBonus(String subId, UUID bonusId) {
        UUID subOperatorId = GoldenEggsBonusUtil.generateSubOperatorId(config.getAggregatorId(), subId);
        String signature = GoldenEggsBonusUtil.generateRequestSignature(
                subOperatorId.toString(),
                config.getSignatureSecret()
        );

        log.info("Cancelling bonus: bonusId={}, subOperatorId={}", bonusId, subOperatorId);

        // Call provider first - only mark CANCELLED locally after provider confirms,
        // otherwise a failed provider call would leave local state inconsistent
        return goldenEggsWebClient.delete()
                .uri("/api/operator/v1/bonuses/" + subOperatorId + "/" + bonusId)
                .header("X-REQUEST-SIGN", signature)
                .retrieve()
                .bodyToMono(CancelBonusResponse.class)
                .flatMap(response -> {
                    if (response.getStatus() != null && response.getStatus()) {
                        return bonusRepository.findByBonusIdAndSubOperatorId(bonusId, subOperatorId)
                                .flatMap(bonus -> {
                                    bonus.setStatus("CANCELLED");
                                    bonus.setUpdatedAt(Instant.now());
                                    bonus.setNew(false); // loaded entity - UPDATE not INSERT
                                    return bonusRepository.save(bonus);
                                })
                                .thenReturn(response);
                    }
                    return Mono.just(response);
                })
                .doOnSuccess(response -> log.info("Bonus cancelled: bonusId={}", bonusId))
                .onErrorResume(e -> {
                    log.error("Error cancelling bonus", e);
                    return Mono.just(CancelBonusResponse.builder().status(false).build());
                });
    }

    /**
     * Handle bonus-complete webhook
     */
    public Mono<BonusWebhookResponse> handleBonusComplete(BonusWebhookRequest request) {
        UUID transactionId = request.getData().getTransactionId();
        UUID bonusId = request.getData().getBonusId();
        Long userId = Long.parseLong(request.getData().getUserId());
        BigDecimal winSum = new BigDecimal(request.getData().getWinSum());

        log.info("Handling bonus-complete: bonusId={}, userId={}, winSum={}", bonusId, userId, winSum);

        // Check idempotency
        return transactionRepository.findSuccessfulTransaction(transactionId)
                .flatMap(existing -> {
                    log.info("Found existing successful transaction, returning cached response");
                    return walletRepository.findByUserProfileId(userId)
                            .map(wallet -> (BonusWebhookResponse) BonusWebhookResponse.builder()
                                    .code("OK")
                                    .balance(wallet.getTotalAvailableBalance().toString())
                                    .hideFromStat(true)
                                    .build())
                            // Wallet missing must NOT fall through to the new-transaction branch
                            .switchIfEmpty(Mono.just(BonusWebhookResponse.builder()
                                    .code("ACCOUNT_INVALID")
                                    .balance("0")
                                    .build()));
                })
                .switchIfEmpty(Mono.defer(() ->
                        // Load user for agentId (multi-tenancy) and FK validity
                        userProfileRepository.findById(userId)
                                .flatMap(user -> {
                                    // Create transaction record
                                    GoldenEggsBonusTransaction txn = GoldenEggsBonusTransaction.builder()
                                            .id(UUID.randomUUID())
                                            .bonusId(bonusId)
                                            .transactionId(transactionId)
                                            .userId(userId)
                                            .agentId(user.getAgentId())
                                            .action("bonus-complete")
                                            .currency(request.getData().getCurrency())
                                            .winSum(winSum)
                                            .gameMode(request.getGameMode())
                                            .status("PENDING")
                                            .createdAt(Instant.now())
                                            .build();

                                    return transactionRepository.save(txn)
                                            .flatMap(savedTxn -> {
                                                savedTxn.setNew(false); // persisted - subsequent saves are UPDATEs
                                                // Credit wallet with winSum (includes bet amount per spec)
                                                return walletService.creditExternalGame(
                                                        userId,
                                                        winSum,
                                                        request.getData().getCurrency(),
                                                        transactionId,
                                                        bonusId,
                                                        null // gameId not applicable for bonus
                                                ).flatMap(walletResult -> {
                                                    if (walletResult.isSuccess()) {
                                                        savedTxn.setStatus("SUCCESS");
                                                        return transactionRepository.save(savedTxn)
                                                                .flatMap(updated -> {
                                                                    // Update bonus status
                                                                    return bonusRepository.findById(bonusId)
                                                                            .flatMap(bonus -> {
                                                                                bonus.setStatus("COMPLETED");
                                                                                bonus.setWinSum(winSum);
                                                                                bonus.setUpdatedAt(Instant.now());
                                                                                bonus.setNew(false); // loaded entity - UPDATE not INSERT
                                                                                return bonusRepository.save(bonus);
                                                                            })
                                                                            .thenReturn(BonusWebhookResponse.builder()
                                                                                    .code("OK")
                                                                                    .balance(walletResult.getBalance().toString())
                                                                                    .hideFromStat(true)
                                                                                    .build());
                                                                });
                                                    } else {
                                                        savedTxn.setStatus("FAILED");
                                                        return transactionRepository.save(savedTxn)
                                                                .thenReturn(BonusWebhookResponse.builder()
                                                                        .code("ERROR")
                                                                        .balance("0")
                                                                        .build());
                                                    }
                                                });
                                            });
                                })
                                .switchIfEmpty(Mono.just(BonusWebhookResponse.builder()
                                        .code("ACCOUNT_INVALID")
                                        .balance("0")
                                        .build()))
                ));
    }

    /**
     * Handle bonus-expired-when-active webhook
     */
    public Mono<BonusWebhookResponse> handleBonusExpired(BonusWebhookRequest request) {
        UUID transactionId = request.getData().getTransactionId();
        UUID bonusId = request.getData().getBonusId();
        Long userId = Long.parseLong(request.getData().getUserId());

        log.info("Handling bonus-expired-when-active: bonusId={}, userId={}", bonusId, userId);

        // Check idempotency
        return transactionRepository.findSuccessfulTransaction(transactionId)
                .flatMap(existing -> {
                    log.info("Found existing successful transaction, returning cached response");
                    return walletRepository.findByUserProfileId(userId)
                            .map(wallet -> (BonusWebhookResponse) BonusWebhookResponse.builder()
                                    .code("OK")
                                    .balance(wallet.getTotalAvailableBalance().toString())
                                    .hideFromStat(true)
                                    .build())
                            // Wallet missing must NOT fall through to the new-transaction branch
                            .switchIfEmpty(Mono.just(BonusWebhookResponse.builder()
                                    .code("ACCOUNT_INVALID")
                                    .balance("0")
                                    .build()));
                })
                .switchIfEmpty(Mono.defer(() ->
                        // Load user for agentId (multi-tenancy) and FK validity
                        userProfileRepository.findById(userId)
                                .flatMap(user -> {
                                    // Create transaction record
                                    GoldenEggsBonusTransaction txn = GoldenEggsBonusTransaction.builder()
                                            .id(UUID.randomUUID())
                                            .bonusId(bonusId)
                                            .transactionId(transactionId)
                                            .userId(userId)
                                            .agentId(user.getAgentId())
                                            .action("bonus-expired-when-active")
                                            .currency(request.getData().getCurrency())
                                            .winSum(BigDecimal.ZERO)
                                            .gameMode(request.getGameMode())
                                            .status("SUCCESS")
                                            .createdAt(Instant.now())
                                            .build();

                                    return transactionRepository.save(txn)
                                            .flatMap(savedTxn -> {
                                                // Update bonus status
                                                return bonusRepository.findById(bonusId)
                                                        .flatMap(bonus -> {
                                                            bonus.setStatus("EXPIRED_WHEN_ACTIVE");
                                                            bonus.setUpdatedAt(Instant.now());
                                                            bonus.setNew(false); // loaded entity - UPDATE not INSERT
                                                            return bonusRepository.save(bonus);
                                                        })
                                                        .then(walletRepository.findByUserProfileId(userId))
                                                        .map(wallet -> (BonusWebhookResponse) BonusWebhookResponse.builder()
                                                                .code("OK")
                                                                .balance(wallet.getTotalAvailableBalance().toString())
                                                                .hideFromStat(true)
                                                                .build());
                                            });
                                })
                                .switchIfEmpty(Mono.just(BonusWebhookResponse.builder()
                                        .code("ACCOUNT_INVALID")
                                        .balance("0")
                                        .build()))
                ));
    }

    private String serializeGameModes(java.util.List<String> gameModes) {
        try {
            return objectMapper.writeValueAsString(gameModes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize game modes", e);
        }
    }

    private String serializeFreebetConfig(Object config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize freebet config", e);
        }
    }
}
