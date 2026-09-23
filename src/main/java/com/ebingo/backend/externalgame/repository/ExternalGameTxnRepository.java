package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.ExternalGameTxn;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ExternalGameTxnRepository extends ReactiveCrudRepository<ExternalGameTxn, UUID> {

    @Query("SELECT * FROM external_game_txns WHERE action = :action AND provider_transaction_id = :providerTransactionId AND status = 'SUCCESS' ORDER BY created_at DESC LIMIT 1")
    Mono<ExternalGameTxn> findSuccessfulTransaction(String action, UUID providerTransactionId);

    @Query("SELECT * FROM external_game_txns WHERE action = :action AND provider_transaction_id = :providerTransactionId ORDER BY created_at DESC LIMIT 1")
    Mono<ExternalGameTxn> findByActionAndProviderTransactionId(String action, UUID providerTransactionId);

    @Query("SELECT * FROM external_game_txns WHERE action = 'BET' AND provider_transaction_id = :debitId AND status = 'SUCCESS' ORDER BY created_at DESC LIMIT 1")
    Mono<ExternalGameTxn> findSuccessfulBetByDebitId(UUID debitId);

    @Query("SELECT * FROM external_game_txns WHERE action = 'WITHDRAW' AND debit_id = :debitId AND status = 'SUCCESS' ORDER BY created_at DESC LIMIT 1")
    Mono<ExternalGameTxn> findSuccessfulWithdrawByDebitId(UUID debitId);
}
