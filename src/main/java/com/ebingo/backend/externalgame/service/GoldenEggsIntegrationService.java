package com.ebingo.backend.externalgame.service;

import com.ebingo.backend.agent.repository.AgentRepository;
import com.ebingo.backend.common.telegram.TelegramAuthVerifier;
import com.ebingo.backend.externalgame.config.GoldenEggsConfig;
import com.ebingo.backend.externalgame.dto.GameModeDto;
import com.ebingo.backend.externalgame.dto.LaunchRequest;
import com.ebingo.backend.externalgame.dto.LaunchResponse;
import com.ebingo.backend.externalgame.dto.webhook.*;
import com.ebingo.backend.externalgame.entity.ExternalGameAuthToken;
import com.ebingo.backend.externalgame.entity.ExternalGameSession;
import com.ebingo.backend.externalgame.entity.ExternalGameTxn;
import com.ebingo.backend.externalgame.repository.ExternalGameAuthTokenRepository;
import com.ebingo.backend.externalgame.repository.ExternalGameSessionRepository;
import com.ebingo.backend.externalgame.repository.ExternalGameTxnRepository;
import com.ebingo.backend.payment.entity.Wallet;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.payment.service.WalletService;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.repository.UserProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoldenEggsIntegrationService {

    private final GoldenEggsConfig config;
    private final WebClient goldenEggsWebClient;
    private final ExternalGameAuthTokenRepository authTokenRepository;
    private final ExternalGameSessionRepository sessionRepository;
    private final ExternalGameTxnRepository txnRepository;
    private final ExternalGameWalletService walletService;
    private final WalletService bingoWalletService;
    private final WalletRepository walletRepository;
    private final UserProfileRepository userProfileRepository;
    private final AgentRepository agentRepository;
    private final TelegramAuthVerifier telegramAuthVerifier;
    private final TransactionalOperator transactionalOperator;
    private final ObjectMapper objectMapper;
    private final GoldenEggsAccountingService accountingService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Get list of game modes from provider
     */
//    public Mono<String> getGameModesList() {
//        log.info("Fetching game modes list from Golden Eggs");
//
//        return goldenEggsWebClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/api/gameModesList")
//                        .queryParam("operatorId", config.getOperatorId())
//                        .build())
//                .retrieve()
//                .bodyToMono(String.class)
//                .doOnSuccess(response -> log.debug("Game modes list retrieved successfully"))
//                .doOnError(error -> log.error("Error fetching game modes list", error));
//    }
    public Mono<List<GameModeDto>> getGameModesList() {
        log.info("Fetching game modes list from Golden Eggs");

        return goldenEggsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/gameModesList")
                        .queryParam("operatorId", config.getOperatorId())
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<GameModeDto>>() {
                })
                .doOnSuccess(response -> log.debug("Game modes list retrieved successfully"))
                .doOnError(error -> log.error("Error fetching game modes list", error));
    }


    /**
     * Generate launch URL with auth token using Telegram initData authentication
     * No Spring Security dependency - uses Telegram WebApp initData for auth
     */
    public Mono<LaunchResponse> generateLaunchUrl(LaunchRequest request) {
        log.info("Generating launch URL for agentId={}, gameMode={}", request.getAgentId(), request.getGameMode());

        // Step 1: Load agent and get bot token
        return agentRepository.findById(request.getAgentId())
                .switchIfEmpty(Mono.error(new AgentNotFoundException("Agent not found: " + request.getAgentId())))
                .flatMap(agent -> {
                    if (agent.getBotToken() == null || agent.getBotToken().isEmpty()) {
                        return Mono.error(new InvalidConfigurationException("Agent has no bot token configured"));
                    }

                    // Step 2: Verify Telegram initData
                    var verificationResult = telegramAuthVerifier.verifyInitData(
                            request.getInitData(),
                            agent.getBotToken(),
                            config.getInitDataMaxAgeSeconds()
                    );

                    if (verificationResult.isEmpty()) {
                        return Mono.error(new InvalidInitDataException("Invalid or expired Telegram initData"));
                    }

                    var params = verificationResult.get();
                    String userDataJson = params.get("user");
                    if (userDataJson == null) {
                        return Mono.error(new InvalidInitDataException("No user data in initData"));
                    }

                    // Step 3: Parse Telegram user ID
                    Long telegramUserId;
                    try {
                        var userNode = objectMapper.readTree(userDataJson);
                        telegramUserId = userNode.get("id").asLong();
                        log.info("Telegram user authenticated: telegramId={}, agentId={}", telegramUserId, request.getAgentId());
                    } catch (Exception e) {
                        log.error("Failed to parse user data from initData", e);
                        return Mono.error(new InvalidInitDataException("Invalid user data format"));
                    }

                    // Step 4: Find or create platform user
                    return findOrCreateUser(telegramUserId, request.getAgentId(), params)
                            .flatMap(platformUser -> generateAndSaveAuthToken(platformUser.getId(), request)
                                    .retryWhen(Retry.max(3)
                                            .filter(throwable -> throwable instanceof DataIntegrityViolationException)
                                            .doBeforeRetry(signal -> log.warn("Token collision detected, retrying...")))
                            );
                })
                .onErrorResume(AgentNotFoundException.class, e -> {
                    log.warn("Agent not found: {}", request.getAgentId());
                    return Mono.error(e);
                })
                .onErrorResume(InvalidInitDataException.class, e -> {
                    log.warn("Invalid initData for agentId={}", request.getAgentId());
                    return Mono.error(e);
                });
    }

    /**
     * Find existing user or create new one based on Telegram ID
     */
    private Mono<UserProfile> findOrCreateUser(Long telegramUserId, Long agentId, java.util.Map<String, String> telegramParams) {
        return userProfileRepository.findByTelegramIdAndAgentId(telegramUserId, agentId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating new user for telegramId={}, agentId={}", telegramUserId, agentId);

                    // Parse user info from Telegram data
                    String firstName = null;
                    String lastName = null;
                    String username = null;

                    try {
                        String userDataJson = telegramParams.get("user");
                        if (userDataJson != null) {
                            var userNode = objectMapper.readTree(userDataJson);
                            firstName = userNode.has("first_name") ? userNode.get("first_name").asText() : null;
                            lastName = userNode.has("last_name") ? userNode.get("last_name").asText() : null;
//                            username = userNode.has("username") ? userNode.get("username").asText() : null;
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse user details from initData", e);
                    }

                    UserProfile newUser = new UserProfile();
                    newUser.setTelegramId(telegramUserId);
                    newUser.setAgentId(agentId);
                    newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);
                    newUser.setNickname(null); // Can be set later
                    newUser.setIsBot(false);
                    newUser.setIsDeleted(false);
                    newUser.setCreatedAt(java.time.LocalDateTime.now());
                    newUser.setUpdatedAt(java.time.LocalDateTime.now());

                    return userProfileRepository.save(newUser)
                            .flatMap(savedUser ->
                                    bingoWalletService.createWallet(savedUser, false)
                                            .thenReturn(savedUser)
                            )
                            .doOnSuccess(user ->
                                    log.info("Created new user: id={}, telegramId={}", user.getId(), telegramUserId)
                            );

                }));
    }

    /**
     * Generate unique auth token and save to database
     */
    private Mono<LaunchResponse> generateAndSaveAuthToken(Long userId, LaunchRequest request) {
        // Generate cryptographically secure authToken (different from token parameter)
        String authToken = generateSecureToken();
        Instant expiresAt = Instant.now().plusSeconds(config.getLaunchTokenTtlMinutes() * 60L);

        ExternalGameAuthToken authTokenEntity = ExternalGameAuthToken.builder()
                .token(authToken)
                .userId(userId)
                .agentId(request.getAgentId())
                .operatorId(config.getOperatorId())
                .currency(request.getCurrency())
                .gameMode(request.getGameMode())
                .expiresAt(expiresAt)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        return authTokenRepository.save(authTokenEntity)
                .map(saved -> {
                    // Generate token parameter for subId validation using HMAC-SHA256
                    // token = HMAC-SHA256(aggregatorId:subId, operatorSecretKey)
                    String subId = request.getBrandName() != null ? request.getBrandName() : "";
                    String validationToken = generateSubIdValidationToken(config.getAggregatorId(), subId);

                    // URL-encode lobbyUrl since it may contain query parameters
                    String encodedLobbyUrl = "";
                    if (request.getLobbyUrl() != null && !request.getLobbyUrl().isEmpty()) {
                        try {
                            encodedLobbyUrl = java.net.URLEncoder.encode(request.getLobbyUrl(), java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            log.warn("Failed to encode lobbyUrl, using original value", e);
                            encodedLobbyUrl = request.getLobbyUrl();
                        }
                    }

                    // Construct game URL following provider specification
                    // URL format: aggregatorId, subId, gameMode, currency, authToken, lang, adaptive, isDemoPlay, token, lobbyUrl, brandName
                    StringBuilder urlBuilder = new StringBuilder();
                    urlBuilder.append(config.getApiBaseUrl())
                            .append("/api/aggregator/launch?")
                            .append("aggregatorId=").append(config.getAggregatorId() != null ? config.getAggregatorId() : "")
                            .append("&subId=").append(subId)
                            .append("&gameMode=").append(request.getGameMode())
                            .append("&currency=").append(request.getCurrency() != null ? request.getCurrency() : "ETB")
                            .append("&authToken=").append(authToken)
                            .append("&lang=").append(request.getLang() != null ? request.getLang() : "en")
                            .append("&adaptive=").append(request.getAdaptive() != null ? request.getAdaptive() : "true")
                            .append("&isDemoPlay=").append(request.getIsDemoPlay() != null ? request.getIsDemoPlay() : "false")
                            .append("&token=").append(validationToken)
                            .append("&lobbyUrl=").append(encodedLobbyUrl)
                            .append("&brandName=").append(request.getBrandName() != null ? request.getBrandName() : "");

                    // Add optional userCountryCode if provided
                    if (request.getUserCountryCode() != null && !request.getUserCountryCode().isEmpty()) {
                        urlBuilder.append("&userCountryCode=").append(request.getUserCountryCode());
                    }

                    String gameUrl = urlBuilder.toString();

                    log.info("Generated game URL for userId={}, authToken={}, validationToken={}",
                            userId, truncateToken(authToken), truncateToken(validationToken));
                    System.out.println(">>>>>>>>>>>>>>>>>>>>  Generated game URL: " + gameUrl);

                    return LaunchResponse.builder()
                            .url(gameUrl)
                            .build();
                });
    }

    // Custom exceptions
    public static class AgentNotFoundException extends RuntimeException {
        public AgentNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidInitDataException extends RuntimeException {
        public InvalidInitDataException(String message) {
            super(message);
        }
    }

    public static class InvalidConfigurationException extends RuntimeException {
        public InvalidConfigurationException(String message) {
            super(message);
        }
    }

    /**
     * Generate subId validation token using HMAC-SHA256
     * Token = HMAC-SHA256(aggregatorId:subId, operatorSecretKey)
     * This follows the provider's specification for validating subId
     */
    private String generateSubIdValidationToken(String aggregatorId, String subId) {
        try {
            // Create key as "aggregatorId:subId"
            String key = aggregatorId + ":" + subId;

            System.out.println(">>>>>>>>>>>>>>>>>>>>  Generating subId validation token with key: " + key);

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    config.getSignatureSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(key.getBytes(StandardCharsets.UTF_8));
            String token = bytesToHex(hash);

            log.debug("Generated subId validation token for key={}", key);
            return token;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating subId validation token", e);
            throw new RuntimeException("Failed to generate subId validation token", e);
        }
    }

    /**
     * Validate webhook signature
     */
    public boolean validateSignature(String rawBody, String signature) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    config.getSignatureSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);

            // Constant-time comparison
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating signature", e);
            return false;
        }
    }

    /**
     * Handle init webhook
     */
    public Mono<Object> handleInit(InitRequest request) {
        log.info("Handling init request for token: {}", truncateToken(request.getToken()));

        return authTokenRepository.findActiveToken(request.getToken(), Instant.now())
                .flatMap(authToken -> {
                    // Get user and wallet
                    return userProfileRepository.findById(authToken.getUserId())
                            .zipWhen(user -> walletRepository.findByUserProfileId(user.getId()))
                            .flatMap(tuple -> {
                                UserProfile user = tuple.getT1();
                                Wallet wallet = tuple.getT2();

                                // Generate session token
                                String sessionToken = generateSecureToken();
                                Instant sessionExpiresAt = Instant.now()
                                        .plusSeconds(config.getSessionTokenTtlMinutes() * 60L);

                                ExternalGameSession session = ExternalGameSession.builder()
                                        .sessionToken(sessionToken)
                                        .authToken(authToken.getToken())
                                        .userId(user.getId())
                                        .agentId(authToken.getAgentId())
                                        .operatorId(config.getOperatorId())
                                        .currency(request.getData().getCurrency())
                                        .gameMode(request.getData().getGameMode())
                                        .expiresAt(sessionExpiresAt)
                                        .status("ACTIVE")
                                        .createdAt(Instant.now())
                                        .lastSeenAt(Instant.now())
                                        .build();

                                return sessionRepository.save(session)
                                        .map(savedSession -> {
                                            String balance = walletService.formatBalance(
                                                    wallet.getTotalAvailableBalance(),
                                                    request.getData().getCurrency()
                                            );

                                            return (Object) InitResponse.builder()
                                                    .code("OK")
                                                    .userId(user.getId().toString())
                                                    .nickname(user.getNickname() != null ? user.getNickname() : user.getFirstName())
                                                    .balance(balance)
                                                    .currency(request.getData().getCurrency())
                                                    .operator(config.getOperatorId())
                                                    .userAvatar(null) // UserProfile doesn't have profilePictureUrl
                                                    .token(sessionToken)
                                                    .build();
                                        });
                            });
                })
                .switchIfEmpty(Mono.just(ErrorResponse.builder()
                        .code("INVALID_TOKEN")
                        .message("Invalid or expired token")
                        .build()))
                .onErrorResume(e -> {
                    log.error("Error handling init", e);
                    return Mono.just(ErrorResponse.builder()
                            .code("UNKNOWN_ERROR")
                            .message("Internal error")
                            .build());
                });
    }

    /**
     * Handle bet webhook
     */
    public Mono<Object> handleBet(BetRequest request) {
        log.info("Handling bet request: txnId={}, amount={}",
                request.getData().getTransactionId(), request.getData().getAmount());

        return sessionRepository.findActiveSession(request.getToken(), Instant.now())
                .flatMap(session -> {
                    BigDecimal amount = new BigDecimal(request.getData().getAmount());

                    // Create transaction record
                    ExternalGameTxn txn = ExternalGameTxn.builder()
                            .id(UUID.randomUUID())
                            .action("BET")
                            .providerTransactionId(request.getData().getTransactionId())
                            .gameId(request.getData().getGameId())
                            .userId(session.getUserId())
                            .agentId(session.getAgentId())
                            .operatorId(session.getOperatorId())
                            .currency(request.getData().getCurrency())
                            .gameMode(request.getGameMode())
                            .amount(amount)
                            .isFinished(false)
                            .status("PENDING")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return txnRepository.save(txn)
                            .flatMap(savedTxn -> {
                                    // Mark as not new to allow UPDATE on subsequent saves
                                    savedTxn.setNew(false);
                                    return walletService.debitExternalGame(
                                                    session.getUserId(),
                                                    amount,
                                                    request.getData().getCurrency(),
                                                    request.getData().getTransactionId(),
                                                    request.getData().getGameId()
                                            )
                                            .flatMap(walletResult -> {
                                                if (walletResult.isSuccess()) {
                                                    savedTxn.setStatus("SUCCESS");
                                                    // Record bet in accounting (async)
                                                    recordBetAsync(amount, request.getData().getCurrency(), session.getAgentId());
                                                    return txnRepository.save(savedTxn)
                                                            .map(updated -> BetResponse.builder()
                                                                    .code("OK")
                                                                    .balance(walletResult.getBalance())
                                                                    .build());
                                                } else {
                                                    savedTxn.setStatus("FAILED");
                                                    savedTxn.setErrorCode(walletResult.getErrorCode());
                                                    savedTxn.setErrorMessage(walletResult.getErrorMessage());
                                                    return txnRepository.save(savedTxn)
                                                            .map(updated -> ErrorResponse.builder()
                                                                    .code(walletResult.getErrorCode())
                                                                    .message(walletResult.getErrorMessage())
                                                                    .build());
                                                }
                                            });
                            });
                })
                .switchIfEmpty(Mono.just(ErrorResponse.builder()
                        .code("INVALID_TOKEN")
                        .message("Invalid or expired session")
                        .build()))
                .onErrorResume(e -> {
                    log.error("Error handling bet", e);
                    return Mono.just(ErrorResponse.builder()
                            .code("UNKNOWN_ERROR")
                            .message("Internal error processing bet")
                            .build());
                });
    }

    /**
     * Handle withdraw webhook (idempotent)
     */
    public Mono<String> handleWithdraw(WithdrawRequest request) {
        log.info("Handling withdraw request: txnId={}, result={}",
                request.getData().getTransactionId(), request.getData().getResult());

        // Check for existing successful transaction (idempotency)
        return txnRepository.findSuccessfulTransaction("WITHDRAW", request.getData().getTransactionId())
                .flatMap(existingTxn -> {
                    log.info("Found existing successful withdraw transaction, returning cached response");
                    return Mono.just(existingTxn.getResponseSnapshot());
                })
                .switchIfEmpty(
                        sessionRepository.findActiveSession(request.getToken(), Instant.now())
                                .flatMap(session -> {
                                    ExternalGameSession gameSession = session;
                                    BigDecimal result = new BigDecimal(request.getData().getResult());
                                    BigDecimal amount = new BigDecimal(request.getData().getAmount());
                                    BigDecimal coefficient = request.getData().getCoefficient() != null
                                            ? new BigDecimal(request.getData().getCoefficient())
                                            : null;

                                    // Create transaction record
                                    ExternalGameTxn txn = ExternalGameTxn.builder()
                                            .id(UUID.randomUUID())
                                            .action("WITHDRAW")
                                            .providerTransactionId(request.getData().getTransactionId())
                                            .debitId(request.getData().getDebitId())
                                            .gameId(request.getData().getGameId())
                                            .userId(gameSession.getUserId())
                                            .agentId(gameSession.getAgentId())
                                            .operatorId(gameSession.getOperatorId())
                                            .currency(request.getData().getCurrency())
                                            .gameMode(request.getGameMode())
                                            .amount(amount)
                                            .result(result)
                                            .coefficient(coefficient)
                                            .isFinished(request.getData().getIsFinished())
                                            .status("PENDING")
                                            .createdAt(Instant.now())
                                            .updatedAt(Instant.now())
                                            .build();

                                    return txnRepository.save(txn)
                                            .flatMap(savedTxn -> {
                                                    // Mark as not new to allow UPDATE on subsequent saves
                                                    savedTxn.setNew(false);
                                                    return walletService.creditExternalGame(
                                                                    gameSession.getUserId(),
                                                                    result,
                                                                    request.getData().getCurrency(),
                                                                    request.getData().getTransactionId(),
                                                                    request.getData().getDebitId(),
                                                                    request.getData().getGameId()
                                                            )
                                                            .flatMap(walletResult -> {
                                                                try {
                                                                    String responseJson;
                                                                    if (walletResult.isSuccess()) {
                                                                        savedTxn.setStatus("SUCCESS");
                                                                        // Record win in accounting (async)
                                                                        recordWinAsync(result, request.getData().getCurrency(), gameSession.getAgentId());
                                                                        WithdrawResponse response = WithdrawResponse.builder()
                                                                                .code("OK")
                                                                                .balance(walletResult.getBalance())
                                                                                .build();
                                                                        responseJson = objectMapper.writeValueAsString(response);
                                                                    } else {
                                                                        savedTxn.setStatus("FAILED");
                                                                        savedTxn.setErrorCode(walletResult.getErrorCode());
                                                                        savedTxn.setErrorMessage(walletResult.getErrorMessage());
                                                                        ErrorResponse errorResponse = ErrorResponse.builder()
                                                                                .code(walletResult.getErrorCode())
                                                                                .message(walletResult.getErrorMessage())
                                                                                .build();
                                                                        responseJson = objectMapper.writeValueAsString(errorResponse);
                                                                    }

                                                                    savedTxn.setResponseSnapshot(responseJson);
                                                                    return txnRepository.save(savedTxn)
                                                                            .map(updated -> responseJson);
                                                                } catch (JsonProcessingException e) {
                                                                    log.error("Error serializing response", e);
                                                                    return Mono.just("{\"code\":\"UNKNOWN_ERROR\"}");
                                                                }
                                                            });
                                            });
                                })
                                .switchIfEmpty(Mono.fromCallable(() -> {
                                    try {
                                        return objectMapper.writeValueAsString(
                                                ErrorResponse.builder()
                                                        .code("INVALID_TOKEN")
                                                        .message("Invalid or expired session")
                                                        .build()
                                        );
                                    } catch (JsonProcessingException e) {
                                        return "{\"code\":\"INVALID_TOKEN\"}";
                                    }
                                }))
                )
                .onErrorResume(e -> {
                    log.error("Error handling withdraw", e);
                    try {
                        return Mono.just(objectMapper.writeValueAsString(
                                ErrorResponse.builder()
                                        .code("UNKNOWN_ERROR")
                                        .message("Internal error processing withdraw")
                                        .build()
                        ));
                    } catch (JsonProcessingException ex) {
                        return Mono.just("{\"code\":\"UNKNOWN_ERROR\"}");
                    }
                });
    }

    /**
     * Handle rollback webhook (idempotent)
     */
    public Mono<String> handleRollback(RollbackRequest request) {
        log.info("Handling rollback request: txnId={}, amount={}",
                request.getData().getTransactionId(), request.getData().getAmount());

        // Check for existing successful transaction (idempotency)
        return txnRepository.findSuccessfulTransaction("ROLLBACK", request.getData().getTransactionId())
                .flatMap(existingTxn -> {
                    log.info("Found existing successful rollback transaction, returning cached response");
                    return Mono.just(existingTxn.getResponseSnapshot());
                })
                .switchIfEmpty(
                        sessionRepository.findActiveSession(request.getToken(), Instant.now())
                                .flatMap(session -> {
                                    ExternalGameSession gameSession = session;
                                    BigDecimal amount = new BigDecimal(request.getData().getAmount());

                                    // Create transaction record
                                    ExternalGameTxn txn = ExternalGameTxn.builder()
                                            .id(UUID.randomUUID())
                                            .action("ROLLBACK")
                                            .providerTransactionId(request.getData().getTransactionId())
                                            .debitId(request.getData().getDebitId())
                                            .gameId(request.getData().getGameId())
                                            .userId(gameSession.getUserId())
                                            .agentId(gameSession.getAgentId())
                                            .operatorId(gameSession.getOperatorId())
                                            .currency(request.getData().getCurrency())
                                            .gameMode(request.getGameMode())
                                            .amount(amount)
                                            .isFinished(request.getData().getIsFinished())
                                            .status("PENDING")
                                            .createdAt(Instant.now())
                                            .updatedAt(Instant.now())
                                            .build();

                                    return txnRepository.save(txn)
                                            .flatMap(savedTxn -> {
                                                    // Mark as not new to allow UPDATE on subsequent saves
                                                    savedTxn.setNew(false);
                                                    return walletService.rollbackExternalGame(
                                                                    gameSession.getUserId(),
                                                                    amount,
                                                                    request.getData().getCurrency(),
                                                                    request.getData().getTransactionId(),
                                                                    request.getData().getDebitId(),
                                                                    request.getData().getGameId()
                                                            )
                                                            .flatMap(walletResult -> {
                                                                try {
                                                                    String responseJson;
                                                                    if (walletResult.isSuccess()) {
                                                                        savedTxn.setStatus("SUCCESS");
                                                                        // Record rollback in accounting (async)
                                                                        recordRollbackAsync(amount, request.getData().getCurrency(), gameSession.getAgentId());
                                                                        RollbackResponse response = RollbackResponse.builder()
                                                                                .code("OK")
                                                                                .balance(walletResult.getBalance())
                                                                                .build();
                                                                        responseJson = objectMapper.writeValueAsString(response);
                                                                    } else {
                                                                        savedTxn.setStatus("FAILED");
                                                                        savedTxn.setErrorCode(walletResult.getErrorCode());
                                                                        savedTxn.setErrorMessage(walletResult.getErrorMessage());
                                                                        ErrorResponse errorResponse = ErrorResponse.builder()
                                                                                .code(walletResult.getErrorCode())
                                                                                .message(walletResult.getErrorMessage())
                                                                                .build();
                                                                        responseJson = objectMapper.writeValueAsString(errorResponse);
                                                                    }

                                                                    savedTxn.setResponseSnapshot(responseJson);
                                                                    return txnRepository.save(savedTxn)
                                                                            .map(updated -> responseJson);
                                                                } catch (JsonProcessingException e) {
                                                                    log.error("Error serializing response", e);
                                                                    return Mono.just("{\"code\":\"UNKNOWN_ERROR\"}");
                                                                }
                                                            });
                                            });
                                })
                                .switchIfEmpty(Mono.fromCallable(() -> {
                                    try {
                                        return objectMapper.writeValueAsString(
                                                ErrorResponse.builder()
                                                        .code("INVALID_TOKEN")
                                                        .message("Invalid or expired session")
                                                        .build()
                                        );
                                    } catch (JsonProcessingException e) {
                                        return "{\"code\":\"INVALID_TOKEN\"}";
                                    }
                                }))
                )
                .onErrorResume(e -> {
                    log.error("Error handling rollback", e);
                    try {
                        return Mono.just(objectMapper.writeValueAsString(
                                ErrorResponse.builder()
                                        .code("UNKNOWN_ERROR")
                                        .message("Internal error processing rollback")
                                        .build()
                        ));
                    } catch (JsonProcessingException ex) {
                        return Mono.just("{\"code\":\"UNKNOWN_ERROR\"}");
                    }
                });
    }

    // Helper methods

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String truncateToken(String token) {
        if (token == null || token.length() <= 8) {
            return token;
        }
        return token.substring(0, 8) + "...";
    }

    /**
     * Record bet in accounting (fire-and-forget)
     */
    private void recordBetAsync(BigDecimal amount, String currency, Long agentId) {
        accountingService.recordBet(amount, currency, agentId)
                .subscribe(
                        null,
                        error -> log.error("Error recording bet in accounting", error)
                );
    }

    /**
     * Record win in accounting (fire-and-forget)
     */
    private void recordWinAsync(BigDecimal amount, String currency, Long agentId) {
        accountingService.recordWin(amount, currency, agentId)
                .subscribe(
                        null,
                        error -> log.error("Error recording win in accounting", error)
                );
    }

    /**
     * Record rollback in accounting (fire-and-forget)
     */
    private void recordRollbackAsync(BigDecimal amount, String currency, Long agentId) {
        accountingService.recordRollback(amount, currency, agentId)
                .subscribe(
                        null,
                        error -> log.error("Error recording rollback in accounting", error)
                );
    }
}
