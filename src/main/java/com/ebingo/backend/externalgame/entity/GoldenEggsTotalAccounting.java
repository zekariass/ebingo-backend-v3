package com.ebingo.backend.externalgame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Total accounting for Golden Eggs external games
 * Tracks cumulative revenue, losses, and profit across all time
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("golden_eggs_total_accounting")
public class GoldenEggsTotalAccounting {

    @Id
    private Long id;

    @Column("agent_id")
    private Long agentId;

    @Column("total_bets_count")
    private Long totalBetsCount;

    @Column("total_bets_amount")
    private BigDecimal totalBetsAmount;

    @Column("total_wins_amount")
    private BigDecimal totalWinsAmount;

    @Column("total_loss_amount")
    private BigDecimal totalLossAmount;

    @Column("total_net_profit_amount")
    private BigDecimal totalNetProfitAmount;

    @Column("total_rollback_count")
    private Long totalRollbackCount;

    @Column("total_rollback_amount")
    private BigDecimal totalRollbackAmount;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
