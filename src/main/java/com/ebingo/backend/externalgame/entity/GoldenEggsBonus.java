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
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("golden_eggs_bonus")
public class GoldenEggsBonus {

    @Id
    @Column("bonus_id")
    private UUID bonusId;

    @Column("user_id")
    private Long userId;

    @Column("agent_id")
    private Long agentId;

    @Column("sub_operator_id")
    private UUID subOperatorId;

    @Column("game_modes")
    private String gameModes; // JSON array of game modes

    @Column("currency")
    private String currency;

    @Column("type")
    private String type; // FREEBET

    @Column("status")
    private String status; // CREATED, ACTIVE, PENDING_COMPLETE, COMPLETED, EXPIRED, CANCELLED, EXPIRED_WHEN_ACTIVE

    @Column("bonus_quantity")
    private Integer bonusQuantity;

    @Column("bonus_available")
    private Integer bonusAvailable;

    @Column("win_sum")
    private BigDecimal winSum;

    @Column("freebet_config")
    private String freebetConfig; // JSON string of FreebetConfig

    @Column("expires_at")
    private Instant expiresAt;

    @Column("expires_when_active_at")
    private Instant expiresWhenActiveAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
