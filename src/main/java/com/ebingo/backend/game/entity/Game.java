package com.ebingo.backend.game.entity;

import com.ebingo.backend.game.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@Table("game")
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    @Id
    @Column("id")
    private Long id;

    @Column("room_id")
    private Long roomId;

    @Column("agent_id")
    private Long agentId;

    @Column("joined_players_ids")
    private String joinedPlayersJson;

    @Column("players_count")
    private Integer playersCount;

    @Column("entries_count")
    private Integer entriesCount;

    @Column("drawn_numbers")
    private String drawnNumberJson;

//    @Column("all_card_ids")
//    private String allCardIdsJson;

    @Column("prize_amount")
    private BigDecimal prizeAmount;

    @Column("commission_amount")
    private BigDecimal commissionAmount;

    private Integer capacity;

    private BigDecimal entryFee;

    @Column("started")
    private boolean started = false;

    @Column("ended")
    private boolean ended = false;

    @Column("status")
    private GameStatus status;

    @Column("started_at")
    private LocalDateTime startedAt;

    @Column("ended_at")
    private LocalDateTime endedAt;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;


    @Transient
    private List<String> joinedPlayers = new ArrayList<>();

    @Transient
    private List<Integer> drawnNumbers = new ArrayList<>();

    @Transient
    private List<String> disqualifiedUsers = new ArrayList<>();

    @Transient
    private List<String> allCardIds = new ArrayList<>();
}


