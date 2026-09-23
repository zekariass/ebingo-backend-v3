package com.ebingo.backend.externalgame.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("golden_eggs_bonus_transaction")
public class GoldenEggsBonusTransaction implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("bonus_id")
    private UUID bonusId;

    @Column("transaction_id")
    private UUID transactionId;

    @Column("user_id")
    private Long userId;

    @Column("agent_id")
    private Long agentId;

    @Column("action")
    private String action; // bonus-complete, bonus-expired-when-active

    @Column("currency")
    private String currency;

    @Column("win_sum")
    private BigDecimal winSum;

    @Column("game_mode")
    private String gameMode;

    @Column("status")
    private String status; // SUCCESS, FAILED

    @Column("created_at")
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
