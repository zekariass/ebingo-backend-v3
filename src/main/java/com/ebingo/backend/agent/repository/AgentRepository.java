package com.ebingo.backend.agent.repository;

import com.ebingo.backend.agent.entity.Agent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AgentRepository extends ReactiveCrudRepository<Agent, Long> {
    Flux<Agent> findByIsActiveTrue();

    // Pagination queries
    @Query("SELECT * FROM agents ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<Agent> findAllPaged(int limit, long offset);

    @Query("SELECT * FROM agents ORDER BY name ASC LIMIT :limit OFFSET :offset")
    Flux<Agent> findAllPagedSortedByName(int limit, long offset);

    @Query("SELECT * FROM agents ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Agent> findAllPagedSortedByCreatedAt(int limit, long offset);

    @Query("SELECT * FROM agents WHERE is_active = true ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<Agent> findActiveAgentsPaged(int limit, long offset);

    @Query("SELECT COUNT(*) FROM agents")
    Mono<Long> countAll();

    @Query("SELECT COUNT(*) FROM agents WHERE is_active = true")
    Mono<Long> countActiveAgents();

    // Search agents by multiple fields
    @Query("SELECT * FROM agents WHERE " +
            "LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(bot_username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(contact_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(phone_number) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Flux<Agent> searchAgents(String searchTerm);
}
