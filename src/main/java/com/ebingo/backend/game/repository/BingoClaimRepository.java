package com.ebingo.backend.game.repository;

import com.ebingo.backend.game.entity.BingoClaim;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BingoClaimRepository extends ReactiveCrudRepository<BingoClaim, Long> {

    @Query(
            "SELECT * FROM bingo_claim WHERE player_id = :id " +
                    "ORDER BY created_at DESC " +
                    "LIMIT :pageSize OFFSET :offset"
    )
    Flux<BingoClaim> findByPlayerIdOrderByCreatedAtDesc(Long id, int pageSize, long offset);

    @Query(
            "SELECT * FROM bingo_claim WHERE player_id = :id " +
                    "ORDER BY id DESC " +
                    "LIMIT :pageSize OFFSET :offset"
    )
    Flux<BingoClaim> findByPlayerIdOrderByIdDesc(Long id, int pageSize, long offset);

    @Query(
            "SELECT * FROM bingo_claim WHERE id = :id " +
                    "AND player_id = :playerId"
    )
    Mono<BingoClaim> findByIdAndPlayerId(Long id, Long playerId);
}
