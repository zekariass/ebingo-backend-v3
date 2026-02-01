package com.ebingo.backend.common.mapper;

import com.ebingo.backend.common.dto.TotalLeaderboardDto;
import com.ebingo.backend.common.entity.TotalLeaderboard;
import com.ebingo.backend.user.dto.UserProfileMinimalDto;
import io.r2dbc.spi.Row;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TotalLeaderboardMapper {
    public static TotalLeaderboardDto toDto(TotalLeaderboard totalLeaderboard, UserProfileMinimalDto userProfile) {
        return TotalLeaderboardDto.builder()
                .id(totalLeaderboard.getId())
                .userId(totalLeaderboard.getUserId())
                .totalGamesPlayed(totalLeaderboard.getTotalGamesPlayed())
                .totalWins(totalLeaderboard.getTotalWins())
                .totalBets(totalLeaderboard.getTotalBets())
                .totalPrize(totalLeaderboard.getTotalPrize())
                .totalDeposit(totalLeaderboard.getTotalDeposit())
                .userProfile(userProfile)
                .isBot(totalLeaderboard.getIsBot())
                .createdAt(totalLeaderboard.getCreatedAt())
                .updatedAt(totalLeaderboard.getUpdatedAt())
                .build();
    }

    public static TotalLeaderboard toEntity(TotalLeaderboardDto dto) {
        TotalLeaderboard entity = new TotalLeaderboard();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setTotalGamesPlayed(dto.getTotalGamesPlayed());
        entity.setTotalWins(dto.getTotalWins());
        entity.setTotalPrize(dto.getTotalPrize());
        entity.setTotalBets(dto.getTotalBets());
        entity.setTotalDeposit(dto.getTotalDeposit());
        entity.setIsBot(dto.getIsBot());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }


    private static int safeInt(Row row, String column) {
        Integer value = row.get(column, Integer.class);
        return value != null ? value : 0;
    }

    private static BigDecimal safeBigDecimal(Row row, String column) {
        BigDecimal value = row.get(column, BigDecimal.class);
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean safeBoolean(Row row, String column) {
        Boolean value = row.get(column, Boolean.class);
        return value != null && value;
    }

    public static TotalLeaderboard fromRow(Row row) {
        return TotalLeaderboard.builder()
                .id(row.get("id", Long.class))
                .userId(row.get("user_id", Long.class))
                .totalGamesPlayed(safeInt(row, "total_games_played"))
                .totalWins(safeInt(row, "total_wins"))
                .totalPrize(safeBigDecimal(row, "total_prize"))
                .totalBets(safeBigDecimal(row, "total_bets"))
                .totalDeposit(safeBigDecimal(row, "total_deposit"))
                .isBot(safeBoolean(row, "is_bot"))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .build();
    }


}
