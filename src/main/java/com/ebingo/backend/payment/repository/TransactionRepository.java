package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {
    // CreatedAt sorting
    @Query("SELECT * FROM transactions " +
            "WHERE player_id = :userProfileId " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByPlayerIdOrderByCreatedAtDesc(Long userProfileId, int limit, long offset);

    // TxnAmount sorting
    @Query("SELECT * FROM transactions " +
            "WHERE player_id = :userProfileId " +
            "ORDER BY txn_amount DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByPlayerIdOrderByTxnAmountDesc(Long userProfileId, int limit, long offset);

    // Default (by id)
    @Query("SELECT * FROM transactions " +
            "WHERE player_id = :userProfileId " +
            "ORDER BY id DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByPlayerIdOrderByIdDesc(Long userProfileId, int limit, long offset);

    @Query("SELECT * FROM transactions " +
            "WHERE id = :id " +
            "AND player_id = :userProfileId")
    Mono<Transaction> findByIdAndPlayerId(Long id, Long userProfileId);


    @Query("SELECT * FROM transactions WHERE status = :status AND txn_type = :name ORDER BY txn_amount DESC LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByStatusAndTxnTypeOrderByTxnAmountDesc(String status, String name, int limit, long offset);

    @Query("SELECT * FROM transactions WHERE status = :status AND txn_type = :name ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByStatusAndTxnTypeOrderByCreatedAtDesc(String status, String name, int limit, long offset);

    @Query("SELECT * FROM transactions WHERE status = :status AND txn_type = :name ORDER BY id DESC LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByStatusAndTxnTypeOrderByIdDesc(String status, String name, int limit, long offset);

    Mono<Transaction> findByTxnRef(String txnRef);
}
