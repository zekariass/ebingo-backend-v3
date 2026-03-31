package com.ebingo.backend.system.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.system.dto.SystemConfigDto;
import com.ebingo.backend.system.dto.SystemConfigUpdateDto;
import com.ebingo.backend.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
@Tag(name = "System Config Endpoints", description = "Endpoints for managing system configurations")
@RequireAccessToken
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @Operation(summary = "Get System Configurations", description = "Retrieve system configuration settings")
    public Mono<ResponseEntity<ApiResponse<List<SystemConfigDto>>>> getSystemConfigs(
//            @AuthenticatedTelegramUser TelegramUser user,
            @RequestParam Long agentId,
            ServerWebExchange exchange
    ) {

        return systemConfigService.getAllSystemConfigs(agentId)
                .collectList()
                .map(configs -> ResponseEntity.ok(
                        ApiResponse.<List<SystemConfigDto>>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("System configurations are retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(configs)
                                .build()
                ));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update System Configurations", description = "Update system configuration settings")
    public Mono<ResponseEntity<ApiResponse<SystemConfigDto>>> updateSystemConfigById(
            @PathVariable Long id,
            @RequestBody SystemConfigUpdateDto SystemConfigUpdateDto,
//            @AuthenticatedTelegramUser TelegramUser user,
            ServerWebExchange exchange
    ) {

        return systemConfigService.updateSystemConfigById(id, SystemConfigUpdateDto)
                .map(configs -> ResponseEntity.ok(
                        ApiResponse.<SystemConfigDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("System configurations are updated successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(configs)
                                .build()
                ));
    }
}
