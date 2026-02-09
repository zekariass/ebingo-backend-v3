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

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("external_game_auth_tokens")
public class ExternalGameAuthToken implements Persistable<String> {

    @Id
    @Column("token")
    private String token;

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

    @Column("expires_at")
    private Instant expiresAt;

    @Column("status")
    private String status; // ACTIVE, REVOKED, EXPIRED

    @Column("created_at")
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public String getId() {
        return token;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
