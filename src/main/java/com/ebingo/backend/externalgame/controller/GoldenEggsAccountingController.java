package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.externalgame.entity.GoldenEggsDailyAccounting;
import com.ebingo.backend.externalgame.entity.GoldenEggsTotalAccounting;
import com.ebingo.backend.externalgame.service.GoldenEggsAccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Golden Eggs accounting management
 * Provides endpoints to view and manage accounting data
 */
@RestController
@RequestMapping("/external-games/golden-eggs/accounting")
@RequiredArgsConstructor
@RequireAccessToken
@Slf4j
public class GoldenEggsAccountingController {

    private final GoldenEggsAccountingService accountingService;

    /**
     * Get total accounting summary
     * GET /external-games/golden-eggs/accounting/total?agentId=123
     */
    @GetMapping(value = "/total", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<GoldenEggsTotalAccounting>>> getTotalAccounting(@RequestParam Long agentId) {
        log.info("Request for total accounting: agentId={}", agentId);
        return accountingService.getTotalAccountingSummary(agentId)
                .map(accounting -> ResponseEntity.ok(
                        ApiResponse.<GoldenEggsTotalAccounting>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Total accounting retrieved successfully")
                                .data(accounting)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<GoldenEggsTotalAccounting>builder()
                                .success(false)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("Total accounting not found")
                                .error("NOT_FOUND")
                                .build()
                        ));
    }

    /**
     * Get all daily accounting records
     * GET /external-games/golden-eggs/accounting/daily?agentId=123
     */
    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<List<GoldenEggsDailyAccounting>>>> getAllDailyAccounting(@RequestParam Long agentId) {
        log.info("Request for all daily accounting: agentId={}", agentId);
        return accountingService.getAllDailyAccounting(agentId)
                .collectList()
                .map(records -> ResponseEntity.ok(
                        ApiResponse.<List<GoldenEggsDailyAccounting>>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Daily accounting records retrieved successfully")
                                .data(records)
                                .build()
                ));
    }

    /**
     * Get daily accounting for a specific date
     * GET /external-games/golden-eggs/accounting/daily/{date}?agentId=123
     */
    @GetMapping(value = "/daily/{date}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<GoldenEggsDailyAccounting>>> getDailyAccountingByDate(
            @RequestParam Long agentId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request for daily accounting: agentId={}, date={}", agentId, date);
        return accountingService.getDailyAccounting(agentId, date)
                .map(accounting -> ResponseEntity.ok(
                        ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Daily accounting retrieved successfully")
                                .data(accounting)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(false)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("Daily accounting not found for the specified date")
                                .error("NOT_FOUND")
                                .build()
                        ));
    }

    /**
     * Get daily accounting within a date range
     * GET /external-games/golden-eggs/accounting/daily/range?agentId=123&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping(value = "/daily/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<List<GoldenEggsDailyAccounting>>>> getDailyAccountingByDateRange(
            @RequestParam Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Request for daily accounting range: agentId={}, startDate={}, endDate={}", agentId, startDate, endDate);
        return accountingService.getDailyAccountingByDateRange(agentId, startDate, endDate)
                .collectList()
                .map(records -> ResponseEntity.ok(
                        ApiResponse.<List<GoldenEggsDailyAccounting>>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Daily accounting records retrieved successfully")
                                .data(records)
                                .build()
                ));
    }

    /**
     * Get unsettled daily accounting records
     * GET /external-games/golden-eggs/accounting/daily/unsettled?agentId=123
     */
    @GetMapping(value = "/daily/unsettled", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<List<GoldenEggsDailyAccounting>>>> getUnsettledDailyAccounting(@RequestParam Long agentId) {
        log.info("Request for unsettled daily accounting: agentId={}", agentId);
        return accountingService.getUnsettledDailyAccounting(agentId)
                .collectList()
                .map(records -> ResponseEntity.ok(
                        ApiResponse.<List<GoldenEggsDailyAccounting>>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Unsettled daily accounting records retrieved successfully")
                                .data(records)
                                .build()
                ));
    }

    /**
     * Mark daily accounting as settled
     * PUT /external-games/golden-eggs/accounting/daily/{id}/settle
     */
    @PutMapping(value = "/daily/{id}/settle", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<GoldenEggsDailyAccounting>>> settleDailyAccounting(
            @RequestParam Long agentId,
            @PathVariable Long id) {
        log.info("Request to settle daily accounting: id={}, agentId={}", id, agentId);
        return accountingService.settleDailyAccounting(agentId, id)
                .map(accounting -> ResponseEntity.ok(
                        ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Daily accounting settled successfully")
                                .data(accounting)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(false)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("Daily accounting not found")
                                .error("NOT_FOUND")
                                .build()
                        ));
    }

    /**
     * Mark daily accounting as unsettled
     * PUT /external-games/golden-eggs/accounting/daily/{id}/unsettle
     */
    @PutMapping(value = "/daily/{id}/unsettle", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<GoldenEggsDailyAccounting>>> unsettleDailyAccounting(
            @RequestParam Long agentId,
            @PathVariable Long id) {
        log.info("Request to unsettle daily accounting: id={}, agentId={}", id, agentId);
        return accountingService.unsettleDailyAccounting(agentId, id)
                .map(accounting -> ResponseEntity.ok(
                        ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Daily accounting unsettled successfully")
                                .data(accounting)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(false)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("Daily accounting not found")
                                .error("NOT_FOUND")
                                .build()
                        ));
    }

    /**
     * Get daily accounting by ID
     * GET /external-games/golden-eggs/accounting/daily/id/{id}
     */
    @GetMapping(value = "/daily/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<GoldenEggsDailyAccounting>>> getDailyAccountingById(
            @RequestParam Long agentId,
            @PathVariable Long id) {
        log.info("Request for daily accounting by id: id={}, agentId={}", id, agentId);
        return accountingService.getDailyAccounting(agentId, id)
                .map(accounting -> ResponseEntity.ok(
                        ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(true)
                                .statusCode(HttpStatus.OK.value())
                                .message("Daily accounting retrieved successfully")
                                .data(accounting)
                                .build()
                ))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<GoldenEggsDailyAccounting>builder()
                                .success(false)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .message("Daily accounting not found")
                                .error("NOT_FOUND")
                                .build()
                        ));
    }
}
