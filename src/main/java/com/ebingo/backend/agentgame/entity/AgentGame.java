package com.ebingo.backend.agentgame.entity;

import com.ebingo.backend.agentgame.enums.GameCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Entity representing the association between agents and game categories
 * Allows configuration of which game categories and specific game types are available for each agent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("agent_games")
public class AgentGame {

    @Id
    @Column("id")
    private Long id;

    @Column("agent_id")
    private Long agentId;

    @Column("game_category")
    private GameCategory gameCategory;

    @Column("game_types")
    private String gameTypes;

    @Column("is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
