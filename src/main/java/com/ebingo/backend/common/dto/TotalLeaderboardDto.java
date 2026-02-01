package com.ebingo.backend.common.dto;

import com.ebingo.backend.user.dto.UserProfileMinimalDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalLeaderboardDto {
//    private Long id;
//    private Long userId;
//    private int totalGamesPlayed;
//    private int totalWins;
//    private BigDecimal totalPrize;
//    private BigDecimal totalBets;
//    private BigDecimal totalDeposit;
//    private UserProfileMinimalDto userProfile;
//    private Boolean isBot;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;

    private Long id;
    private Long userId;
    private UserProfileMinimalDto userProfile;
    private Integer totalGamesPlayed;
    private Integer totalWins;
    private BigDecimal totalPrize;
    private BigDecimal totalBets;
    private BigDecimal totalDeposit;
    private BigDecimal totalWithdrawal;
    private Boolean isBot;
    private Long agentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
