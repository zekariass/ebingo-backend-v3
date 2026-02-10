package com.ebingo.backend.agent.controller;

import com.ebingo.backend.agent.dto.agent.AgentDto;
import com.ebingo.backend.agent.dto.agent.AgentUpdateDto;
import com.ebingo.backend.agent.service.AgentService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
@Tag(name = "Agent Secured Controller", description = "Agent Secured Controller")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;


    @GetMapping
    @Operation(summary = "Get all agents", description = "Get all agents with pagination")
    public Mono<ResponseEntity<ApiResponse<PageResponse<AgentDto>>>> getAllAgents(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field: id, name, createdAt")
            @RequestParam(required = false, defaultValue = "id") String sortBy,
//            @AuthenticatedTelegramUser TelegramUser user,
            ServerWebExchange exchange
    ) {
        log.info("Fetching all agents, page: {}, size: {}, sortBy: {}", page, size, sortBy);
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


    @GetMapping("/{agentId}")
    @Operation(summary = "Get agent by ID", description = "Get agent by ID")
    public Mono<ResponseEntity<ApiResponse<AgentDto>>> getAgentById(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId
    ) {

//        log.info("=====================>>>>>: Fetching agent with ID: {}", agentId);
        return agentService.getAgentById(agentId)
                .map(agent -> ApiResponse.<AgentDto>builder()
                        .statusCode(200)
                        .success(true)
                        .message("Agent retrieved successfully")
                        .data(agent)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @GetMapping("/active")
    @Operation(summary = "Get all active agents", description = "Get all active agents")
    public Mono<ResponseEntity<ApiResponse<List<AgentDto>>>> getAllActiveAgents(ServerWebExchange exchange) {
        log.info("================================>>: Fetching all active agents");
        return agentService.getAllActiveAgents()
                .collectList()
                .map(agents -> ApiResponse.<List<AgentDto>>builder()
                        .statusCode(200)
                        .success(true)
                        .message("Agents retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .data(agents)
                        .build()
                )
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{agentId}")
    @Operation(summary = "Update agent by ID", description = "Update agent by ID")
    public Mono<ResponseEntity<ApiResponse<AgentDto>>> updateAgentById(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            @Parameter(required = true, description = "Agent update data") @Valid @RequestBody AgentUpdateDto agentUpdateDto,
            ServerWebExchange exchange
    ) {
        log.info("Updating agent with ID: {}", agentId);
        return agentService.updateAgentById(agentId, agentUpdateDto)
                .map(agent -> ResponseEntity.ok(
                        ApiResponse.<AgentDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Agent updated successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(agent)
                                .build()
                ));
    }

    @GetMapping("/search")
    @Operation(summary = "Search agents", description = "Search agents by name, bot username, contact name, phone number, or code")
    public Mono<ResponseEntity<ApiResponse<List<AgentDto>>>> searchAgents(
            @Parameter(required = true, description = "Search term") @RequestParam String searchTerm,
            ServerWebExchange exchange
    ) {
        log.info("Searching agents with term: {}", searchTerm);
        return agentService.searchAgents(searchTerm)
                .collectList()
                .map(agents -> ApiResponse.<List<AgentDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Agents search completed successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(agents)
                        .build()
                )
                .map(ResponseEntity::ok);
    }
}
