package com.ebingo.backend.user.service;

import com.ebingo.backend.user.entity.ReferralHistory;
import com.ebingo.backend.user.repository.ReferralHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralHistoryServiceImpl implements ReferralHistoryService {

    private final ReferralHistoryRepository referralHistoryRepository;

    @Override
    public Mono<Boolean> existsByRefereeId(Long userId) {
        if (userId == null) {
            log.warn("existsByRefereeId called with null userId");
            return Mono.just(false);
        }
        return referralHistoryRepository.existsByRefereeId(userId)
                .doOnNext(exists -> log.debug("Referral exists for refereeId {}: {}", userId, exists))
                .onErrorResume(e -> {
                    log.error("Error checking referral existence for refereeId {}: {}", userId, e.getMessage());
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<ReferralHistory> createHistory(ReferralHistory history) {
        if (history == null) {
            log.warn("Attempted to save null ReferralHistory");
            return Mono.empty();
        }

        return referralHistoryRepository.save(history)
                .doOnSuccess(saved -> log.info("Referral history saved: referrer={} referee={} status={}",
                        saved.getReferrerId(), saved.getRefereeId(), saved.getStatus()))
                .doOnError(e -> log.error("Failed to save referral history: {}", e.getMessage()));
    }
}
