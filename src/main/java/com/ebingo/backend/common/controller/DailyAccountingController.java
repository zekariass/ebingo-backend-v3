package com.ebingo.backend.common.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.dto.DailyAccountingDto;
import com.ebingo.backend.common.dto.DailyAccountingUpdateDto;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.service.DailyAccountingService;
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
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounting/daily")
@Tag(name = "Daily Accounting Controller", description = "Daily Agent Accounting Management")
@RequiredArgsConstructor
@RequireAccessToken
@Slf4j
public class DailyAccountingController {

    private final DailyAccountingService dailyAccountingService;

    @GetMapping("/{id}")
    @Operation(summary = "Get daily accounting by ID", description = "Retrieve a single daily accounting record by ID")
    public Mono<ResponseEntity<ApiResponse<DailyAccountingDto>>> getById(
            @Parameter(required = true, description = "Daily Accounting ID") @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        log.info("Fetching daily accounting with ID: {}", id);
        return dailyAccountingService.getById(id)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<DailyAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Daily accounting retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }

    @GetMapping
    @Operation(summary = "Get today's daily accounting records for all agents",
            description = "Retrieve paginated list of today's daily accounting records for all agents")
    public Mono<ResponseEntity<ApiResponse<PageResponse<DailyAccountingDto>>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            ServerWebExchange exchange
    ) {
        log.info("Fetching today's  daily accounting records for all agents, page: {}, size: {}", page, size);
        return dailyAccountingService.getTodayRecordsForAllAgents(page, size)
                .map(pageResponse -> ResponseEntity.ok(
                        ApiResponse.<PageResponse<DailyAccountingDto>>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Today's daily accounting records retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(pageResponse)
                                .build()
                ));
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Get daily accounting by agent ID", description = "Retrieve paginated daily accounting records for a specific agent")
    public Mono<ResponseEntity<ApiResponse<PageResponse<DailyAccountingDto>>>> getByAgentId(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            ServerWebExchange exchange
    ) {
        log.info("Fetching daily accounting for agent: {}, page: {}, size: {}", agentId, page, size);
        return dailyAccountingService.getByAgentId(agentId, page, size)
                .map(pageResponse -> ResponseEntity.ok(
                        ApiResponse.<PageResponse<DailyAccountingDto>>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Daily accounting records retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(pageResponse)
                                .build()
                ));
    }

    @GetMapping("/agent/{agentId}/today")
    @Operation(summary = "Get today's daily accounting for agent",
            description = "Retrieve today's daily accounting record for a specific agent. Returns a single record.")
    public Mono<ResponseEntity<ApiResponse<DailyAccountingDto>>> getTodayRecordForAgent(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            ServerWebExchange exchange
    ) {
        log.info("Fetching today's daily accounting record for agent: {}", agentId);
        return dailyAccountingService.getTodayRecordForAgent(agentId)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<DailyAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Today's daily accounting record retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update daily accounting by ID", description = "Update a daily accounting record")
    public Mono<ResponseEntity<ApiResponse<DailyAccountingDto>>> updateById(
            @Parameter(required = true, description = "Daily Accounting ID") @PathVariable Long id,
            @Parameter(required = true, description = "Daily accounting update data")
            @Valid @RequestBody DailyAccountingUpdateDto updateDto,
            ServerWebExchange exchange
    ) {
        log.info("Updating daily accounting with ID: {}", id);
        return dailyAccountingService.updateById(id, updateDto)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<DailyAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Daily accounting updated successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }

    @GetMapping("/agent/{agentId}/date-range")
    @Operation(summary = "Get daily accounting by agent and date range",
            description = "Retrieve paginated daily accounting records for a specific agent within a date range. Dates cannot be in the future.")
    public Mono<ResponseEntity<ApiResponse<PageResponse<DailyAccountingDto>>>> getByAgentIdAndDateRange(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            @Parameter(required = true, description = "Start date (YYYY-MM-DD)") @RequestParam LocalDate startDate,
            @Parameter(required = true, description = "End date (YYYY-MM-DD)") @RequestParam LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            ServerWebExchange exchange
    ) {
        log.info("Fetching daily accounting for agent: {} from {} to {}, page: {}, size: {}",
                agentId, startDate, endDate, page, size);
        return dailyAccountingService.getByAgentIdAndDateRange(agentId, startDate, endDate, page, size)
                .map(pageResponse -> ResponseEntity.ok(
                        ApiResponse.<PageResponse<DailyAccountingDto>>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Daily accounting records retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(pageResponse)
                                .build()
                ));
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's daily accounting for all agents (Admin)",
            description = "Retrieve paginated list of today's daily accounting records for all agents")
    public Mono<ResponseEntity<ApiResponse<PageResponse<DailyAccountingDto>>>> getTodayRecordsForAllAgents(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            ServerWebExchange exchange
    ) {
        log.info("Fetching today's daily accounting records for all agents, page: {}, size: {}", page, size);
        return dailyAccountingService.getTodayRecordsForAllAgents(page, size)
                .map(pageResponse -> ResponseEntity.ok(
                        ApiResponse.<PageResponse<DailyAccountingDto>>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Today's daily accounting records retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(pageResponse)
                                .build()
                ));
    }

    @PutMapping("/{id}/settle")
    @Operation(summary = "Settle daily accounting (Admin)",
            description = "Mark a daily accounting record as settled. Can only settle records that are not from today and not already settled.")
    public Mono<ResponseEntity<ApiResponse<DailyAccountingDto>>> settleById(
            @Parameter(required = true, description = "Daily Accounting ID") @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        log.info("Settling daily accounting with ID: {}", id);
        return dailyAccountingService.settleById(id)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<DailyAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Daily accounting settled successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }
}