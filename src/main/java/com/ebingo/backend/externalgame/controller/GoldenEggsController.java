package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.externalgame.dto.GameModeDto;
import com.ebingo.backend.externalgame.dto.LaunchRequest;
import com.ebingo.backend.externalgame.dto.LaunchResponse;
import com.ebingo.backend.externalgame.service.GoldenEggsIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;


@RestController
@RequestMapping("/external-games/golden-eggs")
@RequiredArgsConstructor
@RequireAccessToken
@Slf4j
public class GoldenEggsController {

    private final GoldenEggsIntegrationService integrationService;

    /**
     * Get list of available game modes
     */
//    @GetMapping(value = "/game-modes", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/game-modes")
    public Mono<ResponseEntity<ApiResponse<List<GameModeDto>>>> getGameModes(
            ServerWebExchange exchange
    ) {
        log.info("Request received for game modes list");
        return integrationService.getGameModesList()
                .map(response -> ApiResponse.<List<GameModeDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Game modes retrieved successfully")
                        .data(response)
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .build())
                .map(ResponseEntity::ok);
    }

    /**
     * Generate launch URL for a game using Telegram initData authentication
     * No Spring Security dependency - uses Telegram WebApp initData
     */
//    @PostMapping(value = "/launch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
//    public Mono<ResponseEntity<Object>> launchGame(@Valid @RequestBody LaunchRequest request) {
//        log.info("Launch game request: agentId={}, gameMode={}, currency={}",
//                request.getAgentId(), request.getGameMode(), request.getCurrency());
//
//        return integrationService.generateLaunchUrl(request)
//                .<ResponseEntity<Object>>map(response -> ResponseEntity.ok().body(response))
//                .onErrorResume(GoldenEggsIntegrationService.AgentNotFoundException.class, e -> {
//                    log.warn("Agent not found: {}", request.getAgentId());
//                    ErrorCodeResponse error = ErrorCodeResponse.builder()
//                            .code("AGENT_NOT_FOUND")
//                            .message(e.getMessage())
//                            .build();
//                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
//                })
//                .onErrorResume(GoldenEggsIntegrationService.InvalidInitDataException.class, e -> {
//                    log.warn("Invalid initData for agentId={}", request.getAgentId());
//                    ErrorCodeResponse error = ErrorCodeResponse.builder()
//                            .code("INVALID_INIT_DATA")
//                            .message(e.getMessage())
//                            .build();
//                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
//                })
//                .onErrorResume(GoldenEggsIntegrationService.InvalidConfigurationException.class, e -> {
//                    log.error("Configuration error for agentId={}", request.getAgentId());
//                    ErrorCodeResponse error = ErrorCodeResponse.builder()
//                            .code("CONFIGURATION_ERROR")
//                            .message(e.getMessage())
//                            .build();
//                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
//                })
//                .onErrorResume(e -> {
//                    log.error("Unexpected error during game launch", e);
//                    ErrorCodeResponse error = ErrorCodeResponse.builder()
//                            .code("UNKNOWN_ERROR")
//                            .message("Internal server error")
//                            .build();
//                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
//                });
//    }
    @PostMapping(value = "/launch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<LaunchResponse>>> launchGame(
            @Valid @RequestBody LaunchRequest request,
            ServerWebExchange exchange
    ) {
        return integrationService.generateLaunchUrl(request)
                .map(resp -> ResponseEntity.ok(
                        ApiResponse.<LaunchResponse>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Launch URL generated successfully")
                                .data(resp)
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                ))
                .onErrorResume(GoldenEggsIntegrationService.AgentNotFoundException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ApiResponse.<LaunchResponse>builder()
                                        .statusCode(HttpStatus.NOT_FOUND.value())
                                        .success(false)
                                        .message(e.getMessage())
                                        .error("AGENT_NOT_FOUND")
                                        .path(exchange.getRequest().getPath().value())
                                        .timestamp(Instant.now())
                                        .build()
                        ))
                )
                .onErrorResume(GoldenEggsIntegrationService.InvalidInitDataException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                ApiResponse.<LaunchResponse>builder()
                                        .statusCode(HttpStatus.UNAUTHORIZED.value())
                                        .success(false)
                                        .message(e.getMessage())
                                        .error("INVALID_INIT_DATA")
                                        .path(exchange.getRequest().getPath().value())
                                        .timestamp(Instant.now())
                                        .build()
                        ))
                )
                .onErrorResume(GoldenEggsIntegrationService.InvalidConfigurationException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.<LaunchResponse>builder()
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .success(false)
                                        .message(e.getMessage())
                                        .error("CONFIGURATION_ERROR")
                                        .path(exchange.getRequest().getPath().value())
                                        .timestamp(Instant.now())
                                        .build()
                        ))
                )
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.<LaunchResponse>builder()
                                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .success(false)
                                        .message("Internal server error")
                                        .error("UNKNOWN_ERROR")
                                        .path(exchange.getRequest().getPath().value())
                                        .timestamp(Instant.now())
                                        .build()
                        ))
                );
    }

}
