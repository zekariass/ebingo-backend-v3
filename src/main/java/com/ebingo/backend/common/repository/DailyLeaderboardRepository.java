/// /package com.ebingo.backend.common.repository;
/// /
/// /import com.ebingo.backend.common.entity.DailyLeaderboard;
/// /import org.springframework.data.r2dbc.repository.Query;
/// /import org.springframework.data.repository.reactive.ReactiveCrudRepository;
/// /import reactor.core.publisher.Flux;
/// /import reactor.core.publisher.Mono;
/// /
/// /import java.time.LocalDate;
/// /
/// /public interface DailyLeaderboardRepository extends ReactiveCrudRepository<DailyLeaderboard, Long> {
/// /    Mono<DailyLeaderboard> findByUserIdAndLeaderboardDate(Long userId, LocalDate today);
/// /
/// /    @Query("""
/// /            SELECT *
/// /            FROM daily_leaderboard
/// /            ORDER BY
/// /                CASE WHEN :orderBy = 'daily_games_played' THEN daily_games_played END DESC,
/// /                CASE WHEN :orderBy = 'daily_wins' THEN daily_wins END DESC,
/// /                CASE WHEN :orderBy = 'daily_prize' THEN daily_prize END DESC,
/// /                CASE WHEN :orderBy = 'daily_bets' THEN daily_bets END DESC,
/// /                CASE WHEN :orderBy = 'daily_deposit' THEN daily_deposit END DESC,
/// /                CASE WHEN :orderBy = 'leaderboard_date' THEN leaderboard_date END DESC,
/// /                CASE WHEN :orderBy = 'created_at' THEN created_at END DESC,
/// /                CASE WHEN :orderBy = 'updated_at' THEN updated_at END DESC
/// /            LIMIT :size OFFSET :offset
/// /            """)
/// /    Flux<DailyLeaderboard> findLeaderboard(String orderBy, int size, int offset);
/// /
/// /    @Query("""
/// /            SELECT *
/// /            FROM daily_leaderboard
/// /            WHERE is_bot = :isBot
/// /            ORDER BY
/// /                CASE WHEN :orderBy = 'daily_games_played' THEN daily_games_played END DESC,
/// /                CASE WHEN :orderBy = 'daily_wins' THEN daily_wins END DESC,
/// /                CASE WHEN :orderBy = 'daily_prize' THEN daily_prize END DESC,
/// /                CASE WHEN :orderBy = 'daily_bets' THEN daily_bets END DESC,
/// /                CASE WHEN :orderBy = 'daily_deposit' THEN daily_deposit END DESC,
/// /                CASE WHEN :orderBy = 'leaderboard_date' THEN leaderboard_date END DESC,
/// /                CASE WHEN :orderBy = 'created_at' THEN created_at END DESC,
/// /                CASE WHEN :orderBy = 'updated_at' THEN updated_at END DESC
/// /            LIMIT :size OFFSET :offset
/// /            """)
/// /    Flux<DailyLeaderboard> findLeaderboardByIsBot(String orderBy, int size, int offset, Boolean isBot);
/// /
/// /    @Query("SELECT COUNT(*) FROM daily_leaderboard")
/// /    Mono<Long> countAll();
/// /
/// /    @Query("SELECT COUNT(*) FROM daily_leaderboard WHERE is_bot = :isBot")
/// /    Mono<Long> countByIsBot(Boolean isBot);
/// /
/// /}
//
//
//package com.ebingo.backend.common.repository;
//
//import com.ebingo.backend.common.entity.DailyLeaderboard;
//import org.springframework.data.r2dbc.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
//import reactor.core.publisher.Mono;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//
//public interface DailyLeaderboardRepository extends ReactiveCrudRepository<DailyLeaderboard, Long> {
//
//    Mono<DailyLeaderboard> findByUserIdAndLeaderboardDate(Long userId, LocalDate leaderboardDate);
//
//    // Increment only daily games & bets
//    @Query("""
//            INSERT INTO daily_leaderboard(user_id, agent_id, daily_games_played, daily_bets, is_bot, leaderboard_date, created_at, updated_at)
//            VALUES (:userId, :agentId, :games, :bets, :isBot, :date, NOW(), NOW())
//            ON CONFLICT(user_id, leaderboard_date)
//            DO UPDATE SET
//                daily_games_played = daily_leaderboard.daily_games_played + EXCLUDED.daily_games_played,
//                daily_bets = daily_leaderboard.daily_bets + EXCLUDED.daily_bets,
//                updated_at = NOW()
//            RETURNING *
//            """)
//    Mono<DailyLeaderboard> upsertDailyGamesAndBets(Long userId, Long agentId, int games, BigDecimal bets, Boolean isBot, LocalDate date);
//
//    // Increment only daily wins & prize
//    @Query("""
//            INSERT INTO daily_leaderboard(user_id, daily_wins, daily_prize, is_bot, leaderboard_date, created_at, updated_at)
//            VALUES (:userId, :wins, :prize, :isBot, :date, NOW(), NOW())
//            ON CONFLICT(user_id, leaderboard_date)
//            DO UPDATE SET
//                daily_wins = daily_leaderboard.daily_wins + EXCLUDED.daily_wins,
//                daily_prize = daily_leaderboard.daily_prize + EXCLUDED.daily_prize,
//                updated_at = NOW()
//            RETURNING *
//            """)
//    Mono<DailyLeaderboard> upsertDailyWinsAndPrize(Long userId, int wins, BigDecimal prize, Boolean isBot, LocalDate date);
//
////    // Increment only daily deposit
////    @Query("""
////            INSERT INTO daily_leaderboard(user_id, agent_id, daily_deposit, is_bot, leaderboard_date, created_at, updated_at)
////            VALUES (:userId, :agentId, :deposit, :isBot, :date, NOW(), NOW())
////            ON CONFLICT(user_id, leaderboard_date)
////            DO UPDATE SET
////                daily_deposit = daily_leaderboard.daily_deposit + EXCLUDED.daily_deposit,
////                updated_at = NOW()
////            RETURNING *
////            """)
////    Mono<DailyLeaderboard> upsertDailyDeposit(Long userId, Long agentId, BigDecimal deposit, Boolean isBot, LocalDate date);
//
//    @Query("""
//            INSERT INTO daily_leaderboard(user_id, agent_id, daily_deposit, is_bot, leaderboard_date, created_at, updated_at)
//            VALUES (:userId, :agentId, :amount, :isBot, :leaderboardDate, NOW(), NOW())
//            ON CONFLICT (user_id, agent_id, leaderboard_date)
//            DO UPDATE SET
//              daily_deposit = daily_leaderboard.daily_deposit + EXCLUDED.daily_deposit,
//              updated_at = NOW()
//            RETURNING *
//            """)
//    Mono<DailyLeaderboard> upsertDailyDeposit(Long userId, Long agentId, BigDecimal amount, Boolean isBot, LocalDate leaderboardDate);
//
//
//    @Query("""
//                INSERT INTO daily_leaderboard(
//                    user_id,
//                    agent_id,
//                    daily_withdrawal,
//                    is_bot,
//                    leaderboard_date,
//                    created_at,
//                    updated_at
//                )
//                VALUES (
//                    :userId,
//                    :agentId,
//                    :amount,
//                    :isBot,
//                    :today,
//                    CURRENT_TIMESTAMP,
//                    CURRENT_TIMESTAMP
//                )
//                ON CONFLICT (user_id, agent_id, leaderboard_date)
//                DO UPDATE SET
//                    daily_withdrawal = daily_leaderboard.daily_withdrawal + EXCLUDED.daily_withdrawal,
//                    updated_at = CURRENT_TIMESTAMP
//                RETURNING *
//            """)
//    Mono<DailyLeaderboard> updateDailyForWithdrawal(
//            @Param("agentId") Long agentId,
//            @Param("userId") Long userId,
//            @Param("amount") BigDecimal amount,
//            @Param("today") LocalDate today,
//            @Param("isBot") Boolean isBot
//    );
//
//
//    // Count today
//    @Query("SELECT COUNT(*) FROM daily_leaderboard WHERE leaderboard_date = :date")
//    Mono<Long> countByDate(LocalDate date);
//
//    @Query("SELECT COUNT(*) FROM daily_leaderboard WHERE leaderboard_date = :date AND is_bot = :isBot")
//    Mono<Long> countByDateAndIsBot(LocalDate date, Boolean isBot);
//
//}


package com.ebingo.backend.common.repository;

import com.ebingo.backend.common.entity.DailyLeaderboard;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyLeaderboardRepository extends ReactiveCrudRepository<DailyLeaderboard, Long> {

    // ✅ If leaderboard is per-agent, this is the correct finder
    Mono<DailyLeaderboard> findByUserIdAndAgentIdAndLeaderboardDate(
            Long userId,
            Long agentId,
            LocalDate leaderboardDate
    );

    // ✅ Increment only daily games & bets
    @Query("""
                INSERT INTO daily_leaderboard(
                    user_id,
                    agent_id,
                    daily_games_played,
                    daily_bets,
                    is_bot,
                    leaderboard_date,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :games,
                    :bets,
                    :isBot,
                    :date,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id, leaderboard_date)
                DO UPDATE SET
                    daily_games_played = daily_leaderboard.daily_games_played + EXCLUDED.daily_games_played,
                    daily_bets = daily_leaderboard.daily_bets + EXCLUDED.daily_bets,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<DailyLeaderboard> upsertDailyGamesAndBets(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("games") int games,
            @Param("bets") BigDecimal bets,
            @Param("isBot") Boolean isBot,
            @Param("date") LocalDate date
    );

    // ✅ Increment only daily wins & prize (must include agent_id)
    @Query("""
                INSERT INTO daily_leaderboard(
                    user_id,
                    agent_id,
                    daily_wins,
                    daily_prize,
                    is_bot,
                    leaderboard_date,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :wins,
                    :prize,
                    :isBot,
                    :date,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id, leaderboard_date)
                DO UPDATE SET
                    daily_wins = daily_leaderboard.daily_wins + EXCLUDED.daily_wins,
                    daily_prize = daily_leaderboard.daily_prize + EXCLUDED.daily_prize,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<DailyLeaderboard> upsertDailyWinsAndPrize(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("wins") int wins,
            @Param("prize") BigDecimal prize,
            @Param("isBot") Boolean isBot,
            @Param("date") LocalDate date
    );

    // ✅ Increment only daily deposit
    @Query("""
                INSERT INTO daily_leaderboard(
                    user_id,
                    agent_id,
                    daily_deposit,
                    is_bot,
                    leaderboard_date,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :amount,
                    :isBot,
                    :leaderboardDate,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id, leaderboard_date)
                DO UPDATE SET
                    daily_deposit = daily_leaderboard.daily_deposit + EXCLUDED.daily_deposit,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<DailyLeaderboard> upsertDailyDeposit(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("amount") BigDecimal amount,
            @Param("isBot") Boolean isBot,
            @Param("leaderboardDate") LocalDate leaderboardDate
    );

    // ✅ Increment only daily withdrawal
    @Query("""
                INSERT INTO daily_leaderboard(
                    user_id,
                    agent_id,
                    daily_withdrawal,
                    is_bot,
                    leaderboard_date,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :amount,
                    :isBot,
                    :today,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id, leaderboard_date)
                DO UPDATE SET
                    daily_withdrawal = daily_leaderboard.daily_withdrawal + EXCLUDED.daily_withdrawal,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<DailyLeaderboard> upsertDailyWithdrawal(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("amount") BigDecimal amount,
            @Param("isBot") Boolean isBot,
            @Param("today") LocalDate today
    );

    // ✅ Count by date (optionally include agent)
    @Query("SELECT COUNT(*) FROM daily_leaderboard WHERE leaderboard_date = :date")
    Mono<Long> countByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(*) FROM daily_leaderboard WHERE leaderboard_date = :date AND is_bot = :isBot")
    Mono<Long> countByDateAndIsBot(
            @Param("date") LocalDate date,
            @Param("isBot") Boolean isBot
    );

    // ✅ Optional: count per agent per day (often what you really want)
    @Query("SELECT COUNT(*) FROM daily_leaderboard WHERE leaderboard_date = :date AND agent_id = :agentId")
    Mono<Long> countByDateAndAgentId(
            @Param("date") LocalDate date,
            @Param("agentId") Long agentId
    );

    @Query("""
                SELECT COUNT(*)
                FROM daily_leaderboard
                WHERE leaderboard_date = :date
                  AND agent_id = :agentId
                  AND is_bot = :isBot
            """)
    Mono<Long> countByDateAndAgentIdAndIsBot(
            @Param("date") LocalDate date,
            @Param("agentId") Long agentId,
            @Param("isBot") Boolean isBot
    );
}
