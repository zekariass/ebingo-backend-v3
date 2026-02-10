package com.ebingo.backend.game.ws;

import com.ebingo.backend.common.telegram.TelegramAuthVerifier;
import com.ebingo.backend.game.dto.WSMessage;
import com.ebingo.backend.game.enums.ParticipantType;
import com.ebingo.backend.game.service.CardPoolService;
import com.ebingo.backend.game.service.CardSelectionService;
import com.ebingo.backend.game.service.GameService;
import com.ebingo.backend.game.service.RedisPublisher;
import com.ebingo.backend.game.service.state.PlayerStateService;
import com.ebingo.backend.game.service.sync.RoomRegistry;
import com.ebingo.backend.system.redis.RedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketHandler implements WebSocketHandler {

    private final ReactiveRedisMessageListenerContainer listenerContainer;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final CardPoolService cardPoolService;
    private final CardSelectionService cardSelectionService;
    private final RedisPublisher publisher;
    private final GameService gameService;
    private final TelegramAuthVerifier telegramAuthVerifier;
    private final ObjectMapper objectMapper;
    private final PlayerStateService playerStateService;
    private final RoomRegistry roomRegistry;

    private final Map<String, Sinks.Many<WSMessage>> sessionSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
//        log.info("======================>>> New WS connection: {}", session.getId());

        // 1️⃣ Extract query params
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams();

        // 2️⃣ Extract and decode initData
//        String encodedInitData = params.getFirst("initData");
        Long agentId = params.getFirst("agentId") != null ? Long.valueOf(Objects.requireNonNull(params.getFirst("agentId"))) : null;
        String userId = params.getFirst("userId");
//
//        log.info("Decoded initData: {}", encodedInitData);

//        if (encodedInitData == null || encodedInitData.isBlank()) {
//            return sendErrorAndClose(session, "Missing initData query param");
//        }
//
//        String initData;
//        try {
//            initData = URLDecoder.decode(encodedInitData, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            log.error("Failed to decode initData", e);
//            return sendErrorAndClose(session, "Invalid initData format");
//        }

//        log.info("=======================>>> Decoded initData: {}", initData);

        // 3️⃣ Verify Telegram initData
//        Optional<Map<String, String>> verified = telegramAuthVerifier.verifyInitData(initData);
//        if (verified.isEmpty()) {
//            log.warn("Invalid Telegram initData signature");
//            return sendErrorAndClose(session, "Invalid Telegram initData signature");
//        }

//        Map<String, String> data = verified.get();

        // 4️⃣ Extract user info
//        Map<String, Object> user;
//        try {
//            user = objectMapper.readValue(data.get("user"), Map.class);
//        } catch (JsonProcessingException e) {
//            log.error("Failed to parse user from initData", e);
//            return sendErrorAndClose(session, "Malformed user data in initData");
//        }

//        log.info("============================>>>> User: {}", user);

//        String userId = String.valueOf(user.get("id"));
//        if (userId == null) {
//            return sendErrorAndClose(session, "Missing user ID in initData");
//        }
//
//        String username = (String) user.getOrDefault("username", "User:" + userId);

        // 5️⃣ Extract roomId from query param
        Long roomId = Optional.ofNullable(params.getFirst("roomId"))
                .map(Long::valueOf)
                .orElse(null);

        if (roomId == null) {
            return sendErrorAndClose(session, "Missing roomId query param");
        }

        // 6️⃣ Start the WebSocket session
        return startSession(session, userId, roomId, agentId);
    }

    /**
     * Utility method to send a JSON-formatted error message before closing the connection.
     */
    private Mono<Void> sendErrorAndClose(WebSocketSession session, String message) {
        log.warn("Closing session {} due to error: {}", session.getId(), message);

        Map<String, Object> errorPayload = Map.of(
                "type", "error",
                "message", message,
                "timestamp", Instant.now().toString()
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(errorPayload);
        } catch (JsonProcessingException e) {
            json = "{\"type\":\"error\",\"message\":\"" + message + "\"}";
        }

        WebSocketMessage wsMessage = session.textMessage(json);
        return session.send(Mono.just(wsMessage))
                .then(session.close(CloseStatus.BAD_DATA));
    }


    private Mono<Void> startSession(WebSocketSession session, String userId, Long roomId, Long agentId) {
        String sessionId = session.getId();
        Sinks.Many<WSMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionSinks.put(sessionId, sink);

        if (userId == null || roomId == null) {
            return publisher.publishUserEvent(userId,
                    Map.of("type", "error", "payload", Map.of(
                            "message", "roomId and userId query params are required",
                            "errorType", "invalid_params"
                    ))).then(session.close(CloseStatus.BAD_DATA));
        }

        // -------------------------------
        // Subscribe to channels
        // -------------------------------

        // 1️⃣ Subscribe to the main room explicitly
        Flux<ReactiveSubscription.Message<String, String>> mainRoomFlux =
                listenerContainer.receive(ChannelTopic.of(RedisKeys.roomChannel(roomId)));

//        // 2️⃣ Subscribe to all other active rooms, skipping the main room
//        Flux<ReactiveSubscription.Message<String, String>> otherRoomsFlux =
//                roomRegistry.getActiveRooms()
//                        .filter(rid -> !rid.equals(roomId))
//                        .flatMap(rid -> listenerContainer.receive(ChannelTopic.of(RedisKeys.roomChannel(rid))));

        // 2️⃣ Subscribe to all other active rooms, skipping the main room
        Flux<ReactiveSubscription.Message<String, String>> otherRoomsFlux =
                roomRegistry.getActiveRooms()
                        .map(Map.Entry::getKey) // extract roomId
                        .filter(activeRoomId -> !activeRoomId.equals(roomId))
                        .flatMap(activeRoomId ->
                                listenerContainer.receive(
                                        ChannelTopic.of(RedisKeys.roomChannel(activeRoomId))
                                )
                        );

        // 3️⃣ Subscribe to the user-specific channel
        Flux<ReactiveSubscription.Message<String, String>> userFlux =
                listenerContainer.receive(ChannelTopic.of(RedisKeys.userChannel(userId)));

        // Merge all subscriptions
        Flux<WSMessage> incomingMessages = Flux.merge(mainRoomFlux, otherRoomsFlux, userFlux)
                .map(ReactiveSubscription.Message::getMessage)
                .map(msg -> {
                    try {
                        return mapper.readValue(msg, WSMessage.class);
                    } catch (Exception e) {
                        log.error("Failed to parse Redis message: {}", msg, e);
                        return new WSMessage("error", Map.of("message", "invalid_message"));
                    }
                });

        Disposable redisSubscription = incomingMessages.subscribe(sink::tryEmitNext);

        // -------------------------------
        // Prepare outgoing messages
        // -------------------------------
        Flux<WebSocketMessage> outgoing = sink.asFlux()
                .map(msg -> {
                    try {
                        return session.textMessage(mapper.writeValueAsString(msg));
                    } catch (Exception e) {
                        log.error("Error serializing WS message", e);
                        return session.textMessage("{}");
                    }
                });

        // -------------------------------
        // Handle incoming messages from client
        // -------------------------------
        Mono<Void> incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(text -> {
                    try {
                        WSMessage msg = mapper.readValue(text, WSMessage.class);
                        return handleClientMessage(msg, roomId, userId, agentId);
                    } catch (Exception e) {
                        log.error("Invalid WS message: {}", text, e);
                        return publisher.publishUserEvent(userId,
                                Map.of("type", "error", "payload", Map.of("message", "invalid_message"))).then();
                    }
                })
                .then();

        // -------------------------------
        // Add user to the main room
        // -------------------------------
        Mono<Void> addUser = redis.opsForSet()
                .add(RedisKeys.roomPlayersKey(roomId), userId)
                .then();

        // -------------------------------
        // Combine everything and cleanup
        // -------------------------------
        return addUser.then(Mono.when(
                        session.send(outgoing),
                        incoming
                ))
                .doFinally(sig -> {
                    // Remove user from the main room
                    redis.opsForSet().remove(RedisKeys.roomPlayersKey(roomId), userId).subscribe();
                    sessionSinks.remove(sessionId);
                    redisSubscription.dispose();
                });
    }


    private Mono<Void> handleClientMessage(
            WSMessage msg, Long roomId, String userId,
            Long agentId) {
        String type = msg.getType();
        Map<String, Object> payload = msg.getPayload() != null ? msg.getPayload() : Map.of();
        switch (type) {
            case "room.getGameStateRequest":
                Integer capacity = (Integer) payload.get("capacity");
                log.info("Getting game state for user {} in room {}, capacity: {}", userId, roomId, capacity);
                return gameService.getOrInitializeGame(roomId, userId, capacity, agentId)
                        .flatMap(gs -> {
                                    Mono<Set<String>> userSelectedCardsIds = playerStateService.getPlayerCardIds(gs.getGameId(), userId); // Placeholder userId
                                    return userSelectedCardsIds
                                            .flatMap(userCardsIds -> {
                                                gs.setUserSelectedCardsIds(userCardsIds);
                                                return publisher.publishUserEvent(userId,
                                                        Map.of("type", "room.serverGameState", "payload", Map.of(
                                                                "success", true,
                                                                "error", "",
                                                                "gameState", gs,
                                                                "roomId", roomId,
                                                                "agentId", agentId
                                                        )));
                                            }).then();
                                }
                        ).then();

//            case "card.cardSelectRequest":
//                String cardId = (payload.get("cardId") != null) ? payload.get("cardId").toString() : null;
//                Long gameIdEvent = (payload.get("gameId") != null) ? Long.valueOf(payload.get("gameId").toString()) : null;
//                return cardSelectionService.claimCard(roomId, gameIdEvent, userId, cardId, 2).then();
//
//            case "card.cardReleaseRequest":
//                String cardId2 = (payload.get("cardId") != null) ? payload.get("cardId").toString() : null;
//                Long gameEventId2 = (payload.get("gameId") != null) ? Long.valueOf(payload.get("gameId").toString()) : null;
//                return cardSelectionService.releaseCard(roomId, gameEventId2, userId, cardId2).then();

            case "game.playerJoinRequest":
                Long gameEventId3 = (payload.get("gameId") != null) ? Long.valueOf(payload.get("gameId").toString()) : null;
                BigDecimal fee3 = (payload.get("fee") != null) ? BigDecimal.valueOf(Double.parseDouble(payload.get("fee").toString())) : null;
                Integer capacity3 = (payload.get("capacity") != null) ? Integer.parseInt(payload.get("capacity").toString()) : 100;
                String userId3 = (payload.get("playerId") != null) ? payload.get("playerId").toString() : null;
                List<String> selectedCardIds = (payload.get("userSelectedCardsIds") != null)
                        ? (List<String>) payload.get("userSelectedCardsIds")
                        : List.of();
                return gameService.playerJoin(roomId, gameEventId3, userId3, capacity3, fee3, selectedCardIds, agentId, ParticipantType.REAL).then();

            case "game.playerLeaveRequest":
                Long gameEventId4 = (payload.get("gameId") != null) ? Long.valueOf(payload.get("gameId").toString()) : null;
                String userId4 = (payload.get("playerId") != null) ? payload.get("playerId").toString() : null;
                return gameService.leaveGame(roomId, gameEventId4, userId4, agentId, ParticipantType.REAL);

            case "card.markNumberRequest":
                Long gameEventId5 = (payload.get("gameId") != null) ? Long.valueOf(payload.get("gameId").toString()) : null;
                return gameService.markNumber(roomId, gameEventId5, userId, payload);

            case "card.unmarkNumberRequest":
                Long gameEventId6 = (payload.get("gameId") != null) ? Long.valueOf(payload.get("gameId").toString()) : null;
                return gameService.unmarkNumber(roomId, gameEventId6, userId, payload);

            case "game.bingoClaimRequest":
                return gameService.claimBingo(roomId, userId, payload, agentId, ParticipantType.REAL);

            case "game.getCard":
                String cardId3 = (payload.get("cardId") != null) ? payload.get("cardId").toString() : null;
                return gameService.getCard(roomId, cardId3, userId);

            case "ping":
                return publisher.publishEvent(RedisKeys.roomChannel(roomId),
                        Map.of("type", "pong",
                                "payload", Map.of())).then();

            default:
                return publisher.publishUserEvent(userId,
                        Map.of("type", "error", "payload",
                                Map.of("message", "unknown_action",
                                        "type", type))).then();
        }
    }

    public void sendToSession(String sessionId, WSMessage msg) {
        Optional.ofNullable(sessionSinks.get(sessionId)).ifPresent(s -> s.tryEmitNext(msg));
    }

    private Mono<Void> publishUserError(String userId, String message, String errorType) {
        return publisher.publishUserEvent(userId,
                Map.of("type", "error", "payload",
                        Map.of("message", message, "errorType", errorType))).then();
    }
}
