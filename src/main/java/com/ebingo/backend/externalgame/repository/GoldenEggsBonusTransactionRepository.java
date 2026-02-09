package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.GoldenEggsBonusTransaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GoldenEggsBonusTransactionRepository extends ReactiveCrudRepository<GoldenEggsBonusTransaction, UUID> {

    @Query("SELECT * FROM golden_eggs_bonus_transaction WHERE transaction_id = :transactionId AND status = 'SUCCESS'")
    Mono<GoldenEggsBonusTransaction> findSuccessfulTransaction(UUID transactionId);
}
