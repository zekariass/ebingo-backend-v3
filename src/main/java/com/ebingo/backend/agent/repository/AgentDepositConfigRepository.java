package com.ebingo.backend.agent.repository;

import com.ebingo.backend.agent.entity.AgentDepositConfig;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AgentDepositConfigRepository extends ReactiveCrudRepository<AgentDepositConfig, Long> {

    /**
     * Upsert an agent_deposit_config row. Used because agent_id is an assigned PK
     * (not generated), so save() cannot distinguish insert from update.
     */
    @Modifying
    @Query("""
            INSERT INTO agent_deposit_config (agent_id, bonus_rate, bonus_fixed,
                                              bonus_cap_enabled, bonus_cap_amount,
                                              lock_rate, lock_fixed,
                                              lock_cap_enabled, lock_cap_amount,
                                              created_at, updated_at)
            VALUES (:agentId, :bonusRate, :bonusFixed,
                    :bonusCapEnabled, :bonusCapAmount,
                    :lockRate, :lockFixed,
                    :lockCapEnabled, :lockCapAmount,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (agent_id) DO UPDATE SET
                bonus_rate = EXCLUDED.bonus_rate,
                bonus_fixed = EXCLUDED.bonus_fixed,
                bonus_cap_enabled = EXCLUDED.bonus_cap_enabled,
                bonus_cap_amount = EXCLUDED.bonus_cap_amount,
                lock_rate = EXCLUDED.lock_rate,
                lock_fixed = EXCLUDED.lock_fixed,
                lock_cap_enabled = EXCLUDED.lock_cap_enabled,
                lock_cap_amount = EXCLUDED.lock_cap_amount,
                updated_at = CURRENT_TIMESTAMP
            """)
    Mono<Void> upsert(Long agentId,
                      BigDecimal bonusRate,
                      BigDecimal bonusFixed,
                      Boolean bonusCapEnabled,
                      BigDecimal bonusCapAmount,
                      BigDecimal lockRate,
                      BigDecimal lockFixed,
                      Boolean lockCapEnabled,
                      BigDecimal lockCapAmount);
}
