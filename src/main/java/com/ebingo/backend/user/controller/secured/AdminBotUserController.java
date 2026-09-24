package com.ebingo.backend.user.controller.secured;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.user.dto.BotUserBulkCreateDto;
import com.ebingo.backend.user.dto.BotUserBulkCreateResultDto;
import com.ebingo.backend.user.service.BotUserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/bot-users")
@Tag(name = "Admin Bot User Controller", description = "Admin endpoints for bulk-creating bot user profiles and wallets")
@RequiredArgsConstructor
@RequireAccessToken
@Slf4j
public class AdminBotUserController {

    private final BotUserAdminService botUserAdminService;

    @PostMapping
    @Operation(
            summary = "Bulk-create bot users with wallets",
            description = "Creates `count` bot user profiles and matching wallets in a single transaction. " +
                    "user_profile.id = telegram_id = wallet.id = wallet.user_profile_id, starting at startId " +
                    "and incremented by 1 per record. Phone numbers start at startPhone (Ethiopian format, " +
                    "e.g. 251900017013) and increment by 1. All bots are ACTIVE/PLAYER, is_bot=true, " +
                    "assigned to botRoomId and agentId. Wallets start with initialBalance " +
                    "(default 1,000,000,000) as total_available_balance."
    )
    public Mono<ResponseEntity<ApiResponse<BotUserBulkCreateResultDto>>> createBotUsers(
            @Valid @RequestBody BotUserBulkCreateDto dto,
            ServerWebExchange exchange
    ) {
        log.info("Admin bulk bot user creation requested: startId={}, startPhone={}, count={}, botRoomId={}, agentId={}",
                dto.getStartId(), dto.getStartPhone(), dto.getCount(), dto.getBotRoomId(), dto.getAgentId());
        return botUserAdminService.createBotUsers(dto)
                .map(result -> ApiResponse.<BotUserBulkCreateResultDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Bot users and wallets created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(result)
                        .build()
                )
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
