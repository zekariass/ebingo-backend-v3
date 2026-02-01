package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.BingoClaimDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BingoClaimService {
    Mono<BingoClaimDto> createBingoClaim(BingoClaimDto bingoClaim);

    Flux<BingoClaimDto> getPaginatedBingoClaim(Long telegramId, Long agentId, Integer page, Integer size, String sortBy);

    Mono<BingoClaimDto> getBingoClaimById(Long telegramId, Long id, Long agentId);
}
