package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.dto.TotalLeaderboardDto;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface TotalLeaderboardService {

    Mono<Void> incrementTotalStats(Long telegramId, BigDecimal betAmount, Long agentId);

    Mono<Void> incrementTotalWinsAndPrize(Long userId, BigDecimal prizeAmount, BigDecimal singleGameFee, Long agentId);

    Mono<Void> incrementTotalDeposit(Long userId, BigDecimal depositAmount, Long agentId);

    Mono<PageResponse<TotalLeaderboardDto>> getTotalLeaderboard(Long agentId, int page, int size, String orderBy, Boolean includeBots);

    Mono<Void> updateForWithdrawal(Long agentId, Long id, BigDecimal amount, Boolean isBot);
}
