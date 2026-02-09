package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.GoldenEggsTotalAccounting;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for Golden Eggs total accounting
 */
@Repository
public interface GoldenEggsTotalAccountingRepository extends ReactiveCrudRepository<GoldenEggsTotalAccounting, Long> {

    /**
     * Get total accounting record for a specific agent
     */
    Mono<GoldenEggsTotalAccounting> findByAgentId(Long agentId);
}
