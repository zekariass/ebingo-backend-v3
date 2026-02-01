package com.ebingo.backend.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("total_leaderboard")
@Builder
public class TotalLeaderboard {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("total_games_played")
    private Integer totalGamesPlayed;

    @Column("total_wins")
    private Integer totalWins;

    @Column("total_prize")
    private BigDecimal totalPrize;

    @Column("total_bets")
    private BigDecimal totalBets;

    @Column("total_deposit")
    private BigDecimal totalDeposit;

    @Column("total_withdrawal")
    private BigDecimal totalWithdrawal;

    @Column("is_bot")
    private Boolean isBot;

    @Column("agent_id")
    private Long agentId;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}

