package com.ebingo.backend.common.dto;

import com.ebingo.backend.user.dto.UserProfileMinimalDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLeaderboardDto {
//    private Long id;
//
//    private Long userId;
//
//    private int dailyGamesPlayed;
//
//    private int dailyWins;
//
//    private BigDecimal dailyPrize;
//
//    private BigDecimal dailyBets;
//
//    private BigDecimal dailyDeposit;
//
//    private LocalDate leaderboardDate;
//
//    private UserProfileMinimalDto userProfile;
//
//    private Boolean isBot;
//
//    private LocalDateTime createdAt;
//
//    private LocalDateTime updatedAt;

    private Long id;
    private Long userId;
    private UserProfileMinimalDto userProfile;
    private LocalDate leaderboardDate;
    private Integer dailyGamesPlayed;
    private Integer dailyWins;
    private BigDecimal dailyPrize;
    private BigDecimal dailyBets;
    private BigDecimal dailyDeposit;
    private BigDecimal dailyWithdrawal;
    private Boolean isBot;
    private Long agentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
