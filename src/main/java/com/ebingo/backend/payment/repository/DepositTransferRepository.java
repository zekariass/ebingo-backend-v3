package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.DepositTransfer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DepositTransferRepository extends ReactiveCrudRepository<DepositTransfer, Long> {

    @Query("SELECT * FROM deposit_transfer " +
            "WHERE sender_id = :id " +
            "ORDER BY amount DESC " +
            "LIMIT :pageSize OFFSET :offset")
    Flux<DepositTransfer> findBySenderIdOrderByAmountDesc(Long id, int pageSize, long offset);

    @Query("SELECT * FROM deposit_transfer " +
            "WHERE sender_id = :id " +
            "ORDER BY created_at DESC " +
            "LIMIT :pageSize OFFSET :offset")
    Flux<DepositTransfer> findBySenderIdOrderByCreatedAtDesc(Long id, int pageSize, long offset);

    @Query("SELECT * FROM deposit_transfer " +
            "WHERE sender_id = :id " +
            "ORDER BY id DESC " +
            "LIMIT :pageSize OFFSET :offset")
    Flux<DepositTransfer> findBySenderIdOrderByIdDesc(Long id, int pageSize, long offset);

    @Query("SELECT * FROM deposit_transfer " +
            "WHERE id = :id " +
            "AND sender_id = :id1")
    Mono<DepositTransfer> findByIdAndSenderId(Long id, Long id1);
}
