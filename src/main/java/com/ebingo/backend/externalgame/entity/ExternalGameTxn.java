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
@Table("external_game_txns")
public class ExternalGameTxn implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("action")
    private String action; // BET, WITHDRAW, ROLLBACK

    @Column("provider_transaction_id")
    private UUID providerTransactionId;

    @Column("debit_id")
    private UUID debitId;

    @Column("game_id")
    private UUID gameId;

    @Column("user_id")
    private Long userId;

    @Column("agent_id")
    private Long agentId;

    @Column("operator_id")
    private String operatorId;

    @Column("currency")
    private String currency;

    @Column("game_mode")
    private String gameMode;

    @Column("amount")
    private BigDecimal amount;

    @Column("result")
    private BigDecimal result;

    @Column("coefficient")
    private BigDecimal coefficient;

    @Column("is_finished")
    private Boolean isFinished;

    @Column("status")
    private String status; // SUCCESS, FAILED

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    @Column("wallet_entry_id")
    private Long walletEntryId;

    @Column("response_snapshot")
    private String responseSnapshot;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }
}
