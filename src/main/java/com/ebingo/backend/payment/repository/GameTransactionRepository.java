package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.GameTransaction;
import com.ebingo.backend.payment.enums.GameTxnStatus;
import com.ebingo.backend.payment.enums.GameTxnType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GameTransactionRepository extends ReactiveCrudRepository<GameTransaction, Long> {

    Mono<GameTransaction> findFirstByPlayerIdAndGameIdAndTxnTypeAndTxnStatusOrderByCreatedAtDesc(Long userId, Long gameId, GameTxnType type, GameTxnStatus txnStatus);

    @Query(
            "SELECT * FROM game_transaction " +
                    "WHERE player_id = :id " +
                    "AND game_id = :gameId " +
                    "AND txn_type = :gameTxnType " +
                    "AND txn_status = :txnStatus " +
                    "ORDER BY id " +
                    "DESC LIMIT 1"
    )
    Mono<GameTransaction> findByPlayerIdAndGameIdAndTxnTypeAndTxnStatus(Long id, Long gameId, GameTxnType gameTxnType, GameTxnStatus txnStatus);

    @Query(
            "SELECT * FROM game_transaction " +
                    "WHERE player_id = :userProfileId " +
                    "ORDER BY txn_amount " +
                    "DESC LIMIT :pageSize OFFSET :offset"
    )
    Flux<GameTransaction> findByPlayerIdOrderByTxnAmountDesc(Long userProfileId, int pageSize, long offset);

    @Query(
            "SELECT * FROM game_transaction " +
                    "WHERE player_id = :userProfileId " +
                    "ORDER BY created_at " +
                    "DESC LIMIT :pageSize OFFSET :offset"
    )
    Flux<GameTransaction> findByPlayerIdOrderByCreatedAtDesc(Long userProfileId, int pageSize, long offset);

    @Query(
            "SELECT * FROM game_transaction " +
                    "WHERE player_id = :userProfileId " +
                    "ORDER BY id " +
                    "DESC LIMIT :pageSize OFFSET :offset"
    )
    Flux<GameTransaction> findByPlayerIdOrderByIdDesc(Long userProfileId, int pageSize, long offset);

    Mono<GameTransaction> findByIdAndPlayerId(Long txnId, Long id);

    Mono<GameTransaction> findFirstByIdAndTxnTypeAndTxnStatusOrderByCreatedAtDesc(Long originalTransactionId, GameTxnType gameTxnType, GameTxnStatus gameTxnStatus);
}
