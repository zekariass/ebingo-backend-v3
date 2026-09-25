package com.ebingo.backend.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Per-agent bot configuration consumed by the Telegram bot server.
 * One row per agent; agent_id is both the PK and FK to agents(id).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("agent_config")
public class AgentConfig {

    @Id
    @Column("agent_id")
    private Long agentId;

    @Column("brand_name")
    private String brandName;

    @Column("admin_ids")
    private String adminIds;

    @Column("logo_name")
    private String logoName;

    @Column("support_contact")
    private String supportContact;

    @Column("support_username")
    private String supportUsername;

    @Column("support_channel")
    private String supportChannel;

    /** Raw JSON string of the JSONB bank_details column. */
    @Column("bank_details")
    private String bankDetails;

    /** When true, clients hide player display names for this agent. */
    @Column("hide_name")
    private Boolean hideName;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
