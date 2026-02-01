package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.DailyLeaderboardDto;
import com.ebingo.backend.common.dto.PageResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface DailyLeaderboardService {
    Mono<Void> incrementDailyStats(Long telegramId, BigDecimal betAmount, Long agentId);

    Mono<Void> incrementDailyWinsAndPrize(Long userId, BigDecimal payout, BigDecimal singleGameFee, Long agentId);

    Mono<Void> incrementDailyDeposit(Long userId, BigDecimal depositAmount, Long agentId);

    Mono<PageResponse<DailyLeaderboardDto>> getDailyLeaderboard(Long agentId, int page, int size, String orderBy, Boolean includeBots);

    Mono<Void> updateForWithdrawal(Long agentId, Long userId, BigDecimal amount, Boolean isBot);
}
