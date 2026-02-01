package com.ebingo.backend.user.repository;

import com.ebingo.backend.user.entity.ReferralHistory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ReferralHistoryRepository extends ReactiveCrudRepository<ReferralHistory, Long> {

    /**
     * Check if a user has already been a referee in any referral.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM referral_history WHERE referee_id = :refereeId)")
    Mono<Boolean> existsByRefereeId(Long refereeId);
}
