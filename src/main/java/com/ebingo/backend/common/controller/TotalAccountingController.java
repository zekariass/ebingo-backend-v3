package com.ebingo.backend.common.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.dto.TotalAccountingDto;
import com.ebingo.backend.common.dto.TotalAccountingUpdateDto;
import com.ebingo.backend.common.service.TotalAccountingService;
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
@RequestMapping("/api/v1/accounting/total")
@Tag(name = "Total Accounting Controller", description = "Total Agent Accounting Management")
@RequiredArgsConstructor
@RequireAccessToken
@Slf4j
public class TotalAccountingController {

    private final TotalAccountingService totalAccountingService;

    @GetMapping("/{id}")
    @Operation(summary = "Get total accounting by ID", description = "Retrieve a single total accounting record by ID")
    public Mono<ResponseEntity<ApiResponse<TotalAccountingDto>>> getById(
            @Parameter(required = true, description = "Total Accounting ID") @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        log.info("Fetching total accounting with ID: {}", id);
        return totalAccountingService.getById(id)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<TotalAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Total accounting retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }

    @GetMapping
    @Operation(summary = "Get all total accounting records", description = "Retrieve paginated list of total accounting records with sorting")
    public Mono<ResponseEntity<ApiResponse<PageResponse<TotalAccountingDto>>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field: id, netIncome, lastSettledAt, createdAt, updatedAt")
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            ServerWebExchange exchange
    ) {
        log.info("Fetching all total accounting records, page: {}, size: {}, sortBy: {}", page, size, sortBy);
        return totalAccountingService.getAll(page, size, sortBy)
                .map(pageResponse -> ResponseEntity.ok(
                        ApiResponse.<PageResponse<TotalAccountingDto>>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Total accounting records retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(pageResponse)
                                .build()
                ));
    }

    @GetMapping("/agent/{agentId}")
    @Operation(summary = "Get total accounting by agent ID", description = "Retrieve total accounting record for a specific agent")
    public Mono<ResponseEntity<ApiResponse<TotalAccountingDto>>> getByAgentId(
            @Parameter(required = true, description = "Agent ID") @PathVariable Long agentId,
            ServerWebExchange exchange
    ) {
        log.info("Fetching total accounting for agent: {}", agentId);
        return totalAccountingService.getByAgentId(agentId)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<TotalAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Total accounting retrieved successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update total accounting by ID", description = "Update a total accounting record")
    public Mono<ResponseEntity<ApiResponse<TotalAccountingDto>>> updateById(
            @Parameter(required = true, description = "Total Accounting ID") @PathVariable Long id,
            @Parameter(required = true, description = "Total accounting update data")
            @Valid @RequestBody TotalAccountingUpdateDto updateDto,
            ServerWebExchange exchange
    ) {
        log.info("Updating total accounting with ID: {}", id);
        return totalAccountingService.updateById(id, updateDto)
                .map(dto -> ResponseEntity.ok(
                        ApiResponse.<TotalAccountingDto>builder()
                                .statusCode(HttpStatus.OK.value())
                                .success(true)
                                .message("Total accounting updated successfully")
                                .path(exchange.getRequest().getPath().value())
                                .timestamp(Instant.now())
                                .data(dto)
                                .build()
                ));
    }
}
