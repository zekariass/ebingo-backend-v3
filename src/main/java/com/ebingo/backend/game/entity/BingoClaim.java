package com.ebingo.backend.game.entity;

import com.ebingo.backend.game.enums.GamePattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@ToString
@Table("bingo_claims")
@NoArgsConstructor
@AllArgsConstructor
public class BingoClaim {
    @Id
    private Long id;

    @Column("game_id")
    private Long gameId;

    @Column("player_id")
    private Long playerId;

    @Column("agent_id")
    private Long agentId;

    private String card;

    @Column("marked_numbers")
    private String markedNumbers;

    private GamePattern pattern;

    @Column("is_winner")
    private Boolean isWinner;

    @Column("error_message")
    private String error;

    @CreatedDate
    @Column("create_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("update_at")
    private Instant updatedAt;
}


