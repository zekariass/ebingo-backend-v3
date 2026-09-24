package com.ebingo.backend.agent.controller;

import com.ebingo.backend.agent.dto.agent.AgentConfigDto;
import com.ebingo.backend.agent.dto.agent.AgentConfigUpdateDto;
import com.ebingo.backend.agent.dto.agent.AgentCreateDto;
import com.ebingo.backend.agent.dto.agent.AgentDepositConfigDto;
import com.ebingo.backend.agent.dto.agent.AgentDto;
import com.ebingo.backend.agent.dto.agent.AgentUpdateDto;
import com.ebingo.backend.agent.service.AgentConfigService;
import com.ebingo.backend.agent.service.AgentDepositConfigService;
import com.ebingo.backend.agent.service.AgentService;
import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/agents")
@Tag(name = "Admin Agent Controller", description = "Admin endpoints for managing agents and their bot configuration")
@RequiredArgsConstructor
@RequireAccessToken
@Slf4j
public class AdminAgentController {

    private final AgentService agentService;
    private final AgentConfigService agentConfigService;
    private final AgentDepositConfigService agentDepositConfigService;

    @PostMapping
    @Operation(summary = "Create agent", description = "Create a new agent; an empty bot config row is created in the same transaction")
    public Mono<ResponseEntity<ApiResponse<AgentDto>>> createAgent(
            @Valid @RequestBody AgentCreateDto dto,
            ServerWebExchange exchange
    ) {
        log.info("Creating agent: {}", dto.getName());
        return agentService.createAgent(dto)
                .map(agent -> ApiResponse.<AgentDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Agent created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(agent)
                        .build()
                )
                .map(agent -> ResponseEntity.status(HttpStatus.CREATED).body(agent));
    }

    @GetMapping
    @Operation(summary = "List agents", description = "Get all agents with pagination; each item includes themeKey for the admin palette badge")
    public Mono<ResponseEntity<ApiResponse<PageResponse<AgentDto>>>> getAllAgents(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field: id, name, createdAt")
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            ServerWebExchange exchange
    ) {
        return agentService.getAllAgents(page, size, sortBy)
                .map(pageResponse -> ApiResponse.<PageResponse<AgentDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agents retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(pageResponse)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{agentId}")
    @Operation(summary = "Update agent", description = "Partial update of an agent; absent/null fields leave existing values unchanged, except themeKey where explicit null resets to the default palette")
    public Mono<ResponseEntity<ApiResponse<AgentDto>>> updateAgent(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            @Valid @RequestBody AgentUpdateDto dto,
            ServerWebExchange exchange
    ) {
        return agentService.updateAgentById(agentId, dto)
                .map(agent -> ApiResponse.<AgentDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agent updated successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(agent)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{agentId}/config")
    @Operation(summary = "Get agent bot config", description = "Fetch an agent's bot configuration for editing")
    public Mono<ResponseEntity<ApiResponse<AgentConfigDto>>> getAgentConfig(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            ServerWebExchange exchange
    ) {
        return agentConfigService.getConfig(agentId)
                .map(config -> ApiResponse.<AgentConfigDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agent config retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(config)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{agentId}/config")
    @Operation(summary = "Upsert agent bot config", description = "Create or replace an agent's bot configuration")
    public Mono<ResponseEntity<ApiResponse<AgentConfigDto>>> upsertAgentConfig(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            @RequestBody AgentConfigUpdateDto dto,
            ServerWebExchange exchange
    ) {
        return agentConfigService.upsertConfig(agentId, dto)
                .map(config -> ApiResponse.<AgentConfigDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agent config saved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(config)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{agentId}/deposit-config")
    @Operation(summary = "Get agent deposit config", description = "Fetch an agent's effective deposit bonus/lock rules (stored row, or global defaults when none exists)")
    public Mono<ResponseEntity<ApiResponse<AgentDepositConfigDto>>> getAgentDepositConfig(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            ServerWebExchange exchange
    ) {
        return agentDepositConfigService.getConfig(agentId)
                .map(config -> ApiResponse.<AgentDepositConfigDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agent deposit config retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(config)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{agentId}/deposit-config")
    @Operation(summary = "Upsert agent deposit config", description = "Create or replace an agent's deposit bonus/lock rules")
    public Mono<ResponseEntity<ApiResponse<AgentDepositConfigDto>>> upsertAgentDepositConfig(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            @Valid @RequestBody AgentDepositConfigDto dto,
            ServerWebExchange exchange
    ) {
        return agentDepositConfigService.upsertConfig(agentId, dto)
                .map(config -> ApiResponse.<AgentDepositConfigDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agent deposit config saved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(config)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{agentId}/deposit-config")
    @Operation(summary = "Delete agent deposit config", description = "Remove an agent's deposit config row, reverting it to the global application.yml defaults")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAgentDepositConfig(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            ServerWebExchange exchange
    ) {
        return agentDepositConfigService.deleteConfig(agentId)
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(
                        ApiResponse.<Void>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Agent deposit config deleted; global defaults now apply")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .build()
                )));
    }
}
