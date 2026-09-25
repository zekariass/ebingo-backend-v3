package com.ebingo.backend.agent.repository;

import com.ebingo.backend.agent.entity.AgentConfig;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AgentConfigRepository extends ReactiveCrudRepository<AgentConfig, Long> {

    /**
     * Upsert an agent_config row. Used because agent_id is an assigned PK
     * (not generated), so save() cannot distinguish insert from update.
     */
    @Modifying
    @Query("""
            INSERT INTO agent_config (agent_id, brand_name, admin_ids, logo_name,
                                      support_contact, support_username, support_channel,
                                      bank_details, hide_name, created_at, updated_at)
            VALUES (:agentId, :brandName, :adminIds, :logoName,
                    :supportContact, :supportUsername, :supportChannel,
                    CAST(:bankDetails AS jsonb), COALESCE(:hideName, FALSE),
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (agent_id) DO UPDATE SET
                brand_name = EXCLUDED.brand_name,
                admin_ids = EXCLUDED.admin_ids,
                logo_name = EXCLUDED.logo_name,
                support_contact = EXCLUDED.support_contact,
                support_username = EXCLUDED.support_username,
                support_channel = EXCLUDED.support_channel,
                bank_details = EXCLUDED.bank_details,
                hide_name = EXCLUDED.hide_name,
                updated_at = CURRENT_TIMESTAMP
            """)
    Mono<Void> upsert(Long agentId,
                      String brandName,
                      String adminIds,
                      String logoName,
                      String supportContact,
                      String supportUsername,
                      String supportChannel,
                      String bankDetails,
                      Boolean hideName);
}
