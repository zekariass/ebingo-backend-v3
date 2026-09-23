package com.ebingo.backend.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-agent deposit bonus/lock rules applied when crediting DEPOSIT transactions.
 * One row per agent; agent_id is both the PK and FK to agents(id).
 * Agents without a row fall back to the global `deposit.*` defaults in application.yml.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("agent_deposit_config")
public class AgentDepositConfig {

    @Id
    @Column("agent_id")
    private Long agentId;

    /** Deposit bonus = amount * bonusRate + bonusFixed. */
    @Column("bonus_rate")
    private BigDecimal bonusRate;

    @Column("bonus_fixed")
    private BigDecimal bonusFixed;

    /** When true, the deposit bonus is capped at bonusCapAmount. */
    @Column("bonus_cap_enabled")
    private Boolean bonusCapEnabled;

    @Column("bonus_cap_amount")
    private BigDecimal bonusCapAmount;

    /** Lock amount = (amount + bonus) * lockRate + lockFixed. */
    @Column("lock_rate")
    private BigDecimal lockRate;

    @Column("lock_fixed")
    private BigDecimal lockFixed;

    /** When true, the lock amount is capped at lockCapAmount. */
    @Column("lock_cap_enabled")
    private Boolean lockCapEnabled;

    @Column("lock_cap_amount")
    private BigDecimal lockCapAmount;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
