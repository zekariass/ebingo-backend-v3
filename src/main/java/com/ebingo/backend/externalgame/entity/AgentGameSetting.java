package com.ebingo.backend.externalgame.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entity representing agent-specific game settings.
 * Allows agents to configure which game modes are enabled for their users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_game_settings")
public class AgentGameSetting {

    @Id
    private Long id;

    @Column("agent_id")
    private Long agentId;

    /**
     * Comma-separated list of enabled game modes.
     * Example: "CLASSIC,TURBO,MEGA"
     */
    @Column("game_modes")
    private String gameModes;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
