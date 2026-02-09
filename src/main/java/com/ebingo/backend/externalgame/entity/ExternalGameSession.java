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
@Table("external_game_sessions")
public class ExternalGameSession implements Persistable<String> {

    @Id
    @Column("session_token")
    private String sessionToken;

    @Column("auth_token")
    private String authToken;

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
    private String status; // ACTIVE, EXPIRED

    @Column("created_at")
    private Instant createdAt;

    @Column("last_seen_at")
    private Instant lastSeenAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public String getId() {
        return sessionToken;
    }

    @Override
    public boolean isNew() {
        return isNew || createdAt == null;
    }
}
