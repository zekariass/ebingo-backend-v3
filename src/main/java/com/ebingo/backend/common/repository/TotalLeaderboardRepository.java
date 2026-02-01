//package com.ebingo.backend.common.repository;
//
//import com.ebingo.backend.common.entity.TotalLeaderboard;
//import org.springframework.data.r2dbc.repository.Query;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//public interface TotalLeaderboardRepository extends ReactiveCrudRepository<TotalLeaderboard, Long> {
//
//    Mono<TotalLeaderboard> findByUserId(Long userId);
//
//    // Fetch leaderboard with dynamic sorting
//    @Query("""
//            SELECT *
//            FROM total_leaderboard
//            ORDER BY
//                CASE WHEN :orderBy = 'total_games_played' THEN total_games_played END DESC,
//                CASE WHEN :orderBy = 'total_wins' THEN total_wins END DESC,
//                CASE WHEN :orderBy = 'total_prize' THEN total_prize END DESC,
//                CASE WHEN :orderBy = 'total_bets' THEN total_bets END DESC,
//                CASE WHEN :orderBy = 'total_deposit' THEN total_deposit END DESC,
//                CASE WHEN :orderBy = 'created_at' THEN created_at END DESC,
//                CASE WHEN :orderBy = 'updated_at' THEN updated_at END DESC
//            LIMIT :size OFFSET :offset
//            """)
//    Flux<TotalLeaderboard> findLeaderboard(String orderBy, int size, int offset);
//
//    // Count all entries
//    @Query("SELECT COUNT(*) FROM total_leaderboard")
//    Mono<Long> countAll();
//
//    // Fetch leaderboard filtered by isBot with dynamic sorting
//    @Query("""
//            SELECT *
//            FROM total_leaderboard
//            WHERE is_bot = :isBot
//            ORDER BY
//                CASE WHEN :orderBy = 'total_games_played' THEN total_games_played END DESC,
//                CASE WHEN :orderBy = 'total_wins' THEN total_wins END DESC,
//                CASE WHEN :orderBy = 'total_prize' THEN total_prize END DESC,
//                CASE WHEN :orderBy = 'total_bets' THEN total_bets END DESC,
//                CASE WHEN :orderBy = 'total_deposit' THEN total_deposit END DESC,
//                CASE WHEN :orderBy = 'created_at' THEN created_at END DESC,
//                CASE WHEN :orderBy = 'updated_at' THEN updated_at END DESC
//            LIMIT :size OFFSET :offset
//            """)
//    Flux<TotalLeaderboard> findLeaderboardByIsBot(String orderBy, int size, int offset, Boolean isBot);
//
//    // Count entries filtered by isBot
//    @Query("SELECT COUNT(*) FROM total_leaderboard WHERE is_bot = :isBot")
//    Mono<Long> countByIsBot(Boolean isBot);
//}

package com.ebingo.backend.common.repository;

import com.ebingo.backend.common.entity.TotalLeaderboard;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface TotalLeaderboardRepository extends ReactiveCrudRepository<TotalLeaderboard, Long> {

    // ✅ Multi-tenant correct finder
    Mono<TotalLeaderboard> findByUserIdAndAgentId(Long userId, Long agentId);

    // ✅ Increment only total games and total bets
    @Query("""
                INSERT INTO total_leaderboard(
                    user_id,
                    agent_id,
                    total_games_played,
                    total_bets,
                    is_bot,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :games,
                    :bets,
                    :isBot,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id)
                DO UPDATE SET
                    total_games_played = total_leaderboard.total_games_played + EXCLUDED.total_games_played,
                    total_bets = total_leaderboard.total_bets + EXCLUDED.total_bets,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<TotalLeaderboard> upsertTotalGamesAndBets(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("games") int games,
            @Param("bets") BigDecimal bets,
            @Param("isBot") Boolean isBot
    );

    // ✅ Increment only total wins and total prize
    @Query("""
                INSERT INTO total_leaderboard(
                    user_id,
                    agent_id,
                    total_wins,
                    total_prize,
                    is_bot,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :wins,
                    :prize,
                    :isBot,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id)
                DO UPDATE SET
                    total_wins = total_leaderboard.total_wins + EXCLUDED.total_wins,
                    total_prize = total_leaderboard.total_prize + EXCLUDED.total_prize,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<TotalLeaderboard> upsertTotalWinsAndPrize(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("wins") int wins,
            @Param("prize") BigDecimal prize,
            @Param("isBot") Boolean isBot
    );

    // ✅ Increment only total deposit
    @Query("""
                INSERT INTO total_leaderboard(
                    user_id,
                    agent_id,
                    total_deposit,
                    is_bot,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :amount,
                    :isBot,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id)
                DO UPDATE SET
                    total_deposit = total_leaderboard.total_deposit + EXCLUDED.total_deposit,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<TotalLeaderboard> upsertTotalDeposit(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("amount") BigDecimal amount,
            @Param("isBot") Boolean isBot
    );

    // ✅ Increment only total withdrawal
    @Query("""
                INSERT INTO total_leaderboard(
                    user_id,
                    agent_id,
                    total_withdrawal,
                    is_bot,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    :agentId,
                    :amount,
                    :isBot,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, agent_id)
                DO UPDATE SET
                    total_withdrawal = total_leaderboard.total_withdrawal + EXCLUDED.total_withdrawal,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING *
            """)
    Mono<TotalLeaderboard> upsertTotalWithdrawal(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("amount") BigDecimal amount,
            @Param("isBot") Boolean isBot
    );
}
