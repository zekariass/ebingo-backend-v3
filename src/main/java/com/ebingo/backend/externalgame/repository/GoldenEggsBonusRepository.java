package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.GoldenEggsBonus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GoldenEggsBonusRepository extends ReactiveCrudRepository<GoldenEggsBonus, UUID> {

    @Query("SELECT * FROM golden_eggs_bonus WHERE sub_operator_id = :subOperatorId AND user_id = :userId")
    Flux<GoldenEggsBonus> findBySubOperatorIdAndUserId(UUID subOperatorId, Long userId);

    @Query("SELECT * FROM golden_eggs_bonus WHERE sub_operator_id = :subOperatorId")
    Flux<GoldenEggsBonus> findBySubOperatorId(UUID subOperatorId);

    @Query("SELECT * FROM golden_eggs_bonus WHERE bonus_id = :bonusId AND sub_operator_id = :subOperatorId")
    Mono<GoldenEggsBonus> findByBonusIdAndSubOperatorId(UUID bonusId, UUID subOperatorId);
}
