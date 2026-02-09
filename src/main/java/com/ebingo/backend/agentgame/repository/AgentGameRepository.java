package com.ebingo.backend.agentgame.repository;

import com.ebingo.backend.agentgame.entity.AgentGame;
import com.ebingo.backend.agentgame.enums.GameCategory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for AgentGame entity
 * Provides reactive database operations for agent-game associations
 */
@Repository
public interface AgentGameRepository extends ReactiveCrudRepository<AgentGame, Long> {

    /**
     * Find all games for a specific agent
     *
     * @param agentId the agent ID
     * @return flux of agent games
     */
    Flux<AgentGame> findByAgentId(Long agentId);

    /**
     * Find all enabled games for a specific agent
     *
     * @param agentId the agent ID
     * @param isEnabled enabled status
     * @return flux of agent games
     */
    Flux<AgentGame> findByAgentIdAndIsEnabled(Long agentId, Boolean isEnabled);

    /**
     * Find a specific game category for an agent
     *
     * @param agentId the agent ID
     * @param gameCategory the game category
     * @return mono of agent game
     */
    Mono<AgentGame> findByAgentIdAndGameCategory(Long agentId, GameCategory gameCategory);

    /**
     * Check if an agent has a specific game category enabled
     *
     * @param agentId the agent ID
     * @param gameCategory the game category
     * @param isEnabled enabled status
     * @return mono of boolean
     */
    @Query("SELECT EXISTS(SELECT 1 FROM agent_games WHERE agent_id = :agentId AND game_category = :gameCategory AND is_enabled = :isEnabled)")
    Mono<Boolean> existsByAgentIdAndGameCategoryAndIsEnabled(Long agentId, GameCategory gameCategory, Boolean isEnabled);
}
