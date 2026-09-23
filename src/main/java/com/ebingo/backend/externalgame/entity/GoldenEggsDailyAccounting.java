package com.ebingo.backend.externalgame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Daily accounting for Golden Eggs external games
 * Tracks revenue, losses, and profit for each day
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("golden_eggs_daily_accounting")
public class GoldenEggsDailyAccounting {

    @Id
    private Long id;

    @Column("agent_id")
    private Long agentId;

    @Column("accounting_date")
    private LocalDate accountingDate;

    @Column("daily_bets_count")
    private Long dailyBetsCount;

    @Column("daily_bets_amount")
    private BigDecimal dailyBetsAmount;

    @Column("daily_wins_amount")
    private BigDecimal dailyWinsAmount;

    @Column("daily_loss_amount")
    private BigDecimal dailyLossAmount;

    @Column("daily_net_profit_amount")
    private BigDecimal dailyNetProfitAmount;

    @Column("daily_rollback_count")
    private Long dailyRollbackCount;

    @Column("daily_rollback_amount")
    private BigDecimal dailyRollbackAmount;

    @Column("is_settled")
    private Boolean isSettled;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Version
    @Column("version")
    private Long version;
}
