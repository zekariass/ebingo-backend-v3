package com.ebingo.backend.user.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.user.dto.UserIdsResponseDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@Tag(name = "User Profile", description = "User Profile APIs")
@RequestMapping("/api/v1/secured/user-profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }


    @GetMapping("/{telegramId}")
    @Operation(summary = "Get user profile", description = "Get user profile")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> getUserProfile(
            @PathVariable Long telegramId,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {
        System.out.println(">>>>> Fetching user profile for telegramId: " + telegramId + " and agentId: " + agentId);
        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .map(userProfileDto -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("User profile retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfileDto)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @PutMapping("/update-nickname")
    @Operation(summary = "Change user name", description = "Change user name")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> changeName(
            @RequestParam Long telegramId,
            @RequestParam Long agentId,
            @RequestParam String nickName,
            ServerWebExchange exchange
    ) {
        return userProfileService.changeNameOfAgentUser(telegramId, nickName, agentId)
                .map(userProfileDto -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("User nickname changed successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfileDto)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @GetMapping("/user-telegram-ids")
    @Operation(summary = "Get user ids", description = "Get user ids")
    public Mono<ResponseEntity<ApiResponse<UserIdsResponseDto>>> getUserIds(
            @RequestParam Long adminTelegramId,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {

        Mono<UserProfileDto> userProfileMono = userProfileService.getUserProfileByTelegramIdAndAgentId(adminTelegramId, agentId);

        return userProfileMono.flatMap(userProfile -> {
            // Check if user is ADMIN or MODERATOR
            if (userProfile.getRole() == UserRole.ADMIN || userProfile.getRole() == UserRole.MODERATOR || userProfile.getRole() == UserRole.AGENT) {
                // Fetch all user Telegram IDs
                return userProfileService.getUserTelegramIdsOfAgent(agentId)
                        .map(userProfileDto -> ApiResponse.<UserIdsResponseDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("User ids retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(userProfileDto)
                                .build()
                        )
                        .map(ResponseEntity::ok);
            } else {
                // Forbidden for non-admin/moderator
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        ApiResponse.<UserIdsResponseDto>builder()
                                .statusCode(HttpStatus.FORBIDDEN.value())
                                .success(false)
                                .message("Access denied")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(null)
                                .build()
                ));
            }
        });
    }

}
