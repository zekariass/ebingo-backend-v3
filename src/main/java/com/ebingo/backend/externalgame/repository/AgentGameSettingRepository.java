package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.AgentGameSetting;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for agent game settings.
 */
@Repository
public interface AgentGameSettingRepository extends ReactiveCrudRepository<AgentGameSetting, Long> {

    /**
     * Find settings by agent ID.
     */
    Mono<AgentGameSetting> findByAgentId(Long agentId);

    /**
     * Delete settings by agent ID.
     */
    Mono<Void> deleteByAgentId(Long agentId);
}
