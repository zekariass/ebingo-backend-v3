package com.ebingo.backend.common.mapper;

import com.ebingo.backend.common.dto.DailyLeaderboardDto;
import com.ebingo.backend.common.entity.DailyLeaderboard;
import com.ebingo.backend.user.dto.UserProfileMinimalDto;
import io.r2dbc.spi.Row;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DailyLeaderboardMapper {

    public static DailyLeaderboardDto toDto(DailyLeaderboard dailyLeaderboard, UserProfileMinimalDto userProfile) {
        return DailyLeaderboardDto.builder()
                .id(dailyLeaderboard.getId())
                .userId(dailyLeaderboard.getUserId())
                .dailyGamesPlayed(dailyLeaderboard.getDailyGamesPlayed())
                .dailyWins(dailyLeaderboard.getDailyWins())
                .dailyBets(dailyLeaderboard.getDailyBets())
                .dailyPrize(dailyLeaderboard.getDailyPrize())
                .dailyDeposit(dailyLeaderboard.getDailyDeposit())
                .leaderboardDate(dailyLeaderboard.getLeaderboardDate())
                .userProfile(userProfile)
                .isBot(dailyLeaderboard.getIsBot())
                .createdAt(dailyLeaderboard.getCreatedAt())
                .updatedAt(dailyLeaderboard.getUpdatedAt())
                .build();
    }

    public static DailyLeaderboard toEntity(DailyLeaderboardDto dailyLeaderboard) {
        DailyLeaderboard entity = new DailyLeaderboard();
        entity.setId(dailyLeaderboard.getId());
        entity.setUserId(dailyLeaderboard.getUserId());
        entity.setDailyGamesPlayed(dailyLeaderboard.getDailyGamesPlayed());
        entity.setDailyWins(dailyLeaderboard.getDailyWins());
        entity.setDailyBets(dailyLeaderboard.getDailyBets());
        entity.setDailyPrize(dailyLeaderboard.getDailyPrize());
        entity.setDailyDeposit(dailyLeaderboard.getDailyDeposit());
        entity.setLeaderboardDate(dailyLeaderboard.getLeaderboardDate());
        entity.setIsBot(dailyLeaderboard.getIsBot());
        entity.setCreatedAt(dailyLeaderboard.getCreatedAt());
        entity.setUpdatedAt(dailyLeaderboard.getUpdatedAt());
        return entity;
    }


    // Map R2DBC Row -> DailyLeaderboard entity
    public static DailyLeaderboard fromRow(Row row) {
        if (row == null) return null;

        return DailyLeaderboard.builder()
                .id(row.get("id", Long.class))
                .userId(row.get("user_id", Long.class))
                .dailyGamesPlayed(safeInt(row, "daily_games_played"))
                .dailyWins(safeInt(row, "daily_wins"))
                .dailyPrize(safeBigDecimal(row, "daily_prize"))
                .dailyBets(safeBigDecimal(row, "daily_bets"))
                .dailyDeposit(safeBigDecimal(row, "daily_deposit"))
                .leaderboardDate(row.get("leaderboard_date", LocalDate.class))
                .isBot(safeBoolean(row, "is_bot"))
                .createdAt(row.get("created_at", LocalDateTime.class))
                .updatedAt(row.get("updated_at", LocalDateTime.class))
                .build();
    }

    // Helper methods for null-safe mapping
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

}
