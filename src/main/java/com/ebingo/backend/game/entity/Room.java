package com.ebingo.backend.game.entity;

import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("room")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    private Long id;

    @Column("agent_id")
    private Long agentId;

    private String name;

    private Integer capacity;

    @Column("min_players")
    private Integer minPlayers;

    @Column("entry_fee")
    private BigDecimal entryFee;

    private GamePattern pattern;

    private RoomStatus status;

    private String cardPoolJson;

    private String allCardIdsJson;

    private Boolean botAllowed;

    private Integer minBots;

    private Integer maxBots;

    // new
    private Integer minDraws;

    private Integer maxDraws;

    private Long fakeWinnerId; // pk

    private Long fakeWinnerTelegramId;
    
    private Boolean fakeWinEnabled;
    //    ======

    private BigDecimal commissionRate;

    @Column("max_cards")
    private Integer maxCards = 1;

    @Column("created_by")
    private Long createdBy;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}

