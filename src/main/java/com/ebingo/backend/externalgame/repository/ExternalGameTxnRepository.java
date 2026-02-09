package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.ExternalGameTxn;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ExternalGameTxnRepository extends ReactiveCrudRepository<ExternalGameTxn, UUID> {

    @Query("SELECT * FROM external_game_txns WHERE action = :action AND provider_transaction_id = :providerTransactionId AND status = 'SUCCESS'")
    Mono<ExternalGameTxn> findSuccessfulTransaction(String action, UUID providerTransactionId);

    Mono<ExternalGameTxn> findByActionAndProviderTransactionId(String action, UUID providerTransactionId);
}
