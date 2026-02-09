package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.GoldenEggsDailyAccounting;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Repository for Golden Eggs daily accounting
 */
@Repository
public interface GoldenEggsDailyAccountingRepository extends ReactiveCrudRepository<GoldenEggsDailyAccounting, Long> {

    /**
     * Find accounting record for a specific agent and date
     */
    Mono<GoldenEggsDailyAccounting> findByAgentIdAndAccountingDate(Long agentId, LocalDate date);

    /**
     * Find all accounting records for an agent ordered by date descending
     */
    Flux<GoldenEggsDailyAccounting> findByAgentIdOrderByAccountingDateDesc(Long agentId);

    /**
     * Find accounting records within a date range for an agent
     */
    Flux<GoldenEggsDailyAccounting> findByAgentIdAndAccountingDateBetween(Long agentId, LocalDate startDate, LocalDate endDate);

    /**
     * Find unsettled accounting records for an agent
     */
    Flux<GoldenEggsDailyAccounting> findByAgentIdAndIsSettledFalseOrderByAccountingDateDesc(Long agentId);

    /**
     * Find settled accounting records for an agent
     */
    Flux<GoldenEggsDailyAccounting> findByAgentIdAndIsSettledTrueOrderByAccountingDateDesc(Long agentId);
}
