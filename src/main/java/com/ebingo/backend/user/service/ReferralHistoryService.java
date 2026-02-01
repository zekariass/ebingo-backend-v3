package com.ebingo.backend.user.service;

import com.ebingo.backend.user.entity.ReferralHistory;
import reactor.core.publisher.Mono;

public interface ReferralHistoryService {
    Mono<Boolean> existsByRefereeId(Long userId);

    Mono<ReferralHistory> createHistory(ReferralHistory history);
}
