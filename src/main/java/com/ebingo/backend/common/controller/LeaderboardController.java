package com.ebingo.backend.common.controller;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.common.dto.DailyLeaderboardDto;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.dto.TotalLeaderboardDto;
import com.ebingo.backend.common.service.DailyLeaderboardService;
import com.ebingo.backend.common.service.TotalLeaderboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@RequireAccessToken
@Tag(name = "Leaderboard Endpoints", description = "Leaderboard related endpoints")
public class LeaderboardController {

    private final DailyLeaderboardService dailyLeaderboardService;
    private final TotalLeaderboardService totalLeaderboardService;

    // -------------------
    // PUBLIC ENDPOINTS
    // -------------------

    @GetMapping("/public/daily")
    public Mono<ResponseEntity<ApiResponse<PageResponse<DailyLeaderboardDto>>>> getDailyLeaderboard(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dailyWins") String orderBy,
            @RequestParam(defaultValue = "false") Boolean includeBots,
            ServerWebExchange exchange
    ) {
        return dailyLeaderboardService.getDailyLeaderboard(agentId, page, size, orderBy, includeBots)
                .map(pageResponse -> ApiResponse.<PageResponse<DailyLeaderboardDto>>builder()
                        .success(true)
                        .statusCode(200)
                        .message("Daily leaderboard fetched successfully")
                        .path(exchange.getRequest().getPath().value())
                        .data(pageResponse)
                        .build())
                .map(ResponseEntity::ok);
    }


    @GetMapping("/public/total")
    public Mono<ResponseEntity<ApiResponse<PageResponse<TotalLeaderboardDto>>>> getTotalLeaderboard(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "totalWins") String orderBy,
            @RequestParam(defaultValue = "false") Boolean includeBots,
            ServerWebExchange exchange
    ) {
        return totalLeaderboardService.getTotalLeaderboard(agentId, page, size, orderBy, includeBots)
                .map(pg -> ApiResponse.<PageResponse<TotalLeaderboardDto>>builder()
                        .success(true)
                        .statusCode(200)
                        .message("Total leaderboard fetched successfully")
                        .path(exchange.getRequest().getPath().value())
                        .data(pg)
                        .build())
                .map(ResponseEntity::ok);
    }

    // -------------------
    // ADMIN ENDPOINTS
    // -------------------

    @GetMapping("/admin/daily")
    public Mono<ResponseEntity<ApiResponse<PageResponse<DailyLeaderboardDto>>>> getDailyLeaderboardAdmin(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "dailyWins") String orderBy,
            @RequestParam(defaultValue = "false") Boolean includeBots,
            ServerWebExchange exchange
    ) {
        return dailyLeaderboardService.getDailyLeaderboard(agentId, page, size, orderBy, includeBots)
                .map(pg -> ApiResponse.<PageResponse<DailyLeaderboardDto>>builder()
                        .success(true)
                        .statusCode(200)
                        .message("Daily leaderboard fetched successfully (admin)")
                        .path(exchange.getRequest().getPath().value())
                        .data(pg)
                        .build())
                .map(ResponseEntity::ok);
    }


    @GetMapping("/admin/total")
    public Mono<ResponseEntity<ApiResponse<PageResponse<TotalLeaderboardDto>>>> getTotalLeaderboardAdmin(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "totalWins") String orderBy,
            @RequestParam(defaultValue = "false") Boolean includeBots,
            ServerWebExchange exchange
    ) {
        return totalLeaderboardService.getTotalLeaderboard(agentId, page, size, orderBy, includeBots)
                .map(pg -> ApiResponse.<PageResponse<TotalLeaderboardDto>>builder()
                        .success(true)
                        .statusCode(200)
                        .message("Total leaderboard fetched successfully (admin)")
                        .path(exchange.getRequest().getPath().value())
                        .data(pg)
                        .build())
                .map(ResponseEntity::ok);
    }
}
