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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("daily_leaderboard")
@Builder
public class DailyLeaderboard {
    @Id
    private Long id;

    @Column("leaderboard_date")
    private LocalDate leaderboardDate;

    @Column("user_id")
    private Long userId;

    @Column("daily_games_played")
    private Integer dailyGamesPlayed;

    @Column("daily_wins")
    private Integer dailyWins;

    @Column("daily_prize")
    private BigDecimal dailyPrize;

    @Column("daily_bets")
    private BigDecimal dailyBets;

    @Column("daily_deposit")
    private BigDecimal dailyDeposit;

    @Column("daily_withdrawal")
    private BigDecimal dailyWithdrawal;

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



