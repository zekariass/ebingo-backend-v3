package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.Wallet;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface WalletRepository extends ReactiveCrudRepository<Wallet, Long> {

    @Query("SELECT * FROM wallet WHERE user_profile_id = :userProfileId")
    Mono<Wallet> findByUserProfileId(Long userProfileId);
}

