package com.ebingo.backend.user.repository;

import com.ebingo.backend.user.entity.UserProfile;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserProfileRepository extends ReactiveCrudRepository<UserProfile, Long> {
    Mono<UserProfile> findByPhoneNumberAndAgentId(String phoneNumber, Long agentId);

    Mono<UserProfile> findByTelegramIdAndAgentId(Long telegramId, Long agentId);

    @Query("SELECT telegram_id FROM user_profile where is_bot = false AND agent_id = :agentId AND is_deleted = false")
    Flux<Long> findAllUserTelegramIdsByAgentId(Long agentId);

    @Query("SELECT * FROM user_profile WHERE is_bot = true AND bot_room_id = :roomId AND is_deleted = false")
    Flux<UserProfile> findBotsByRoom(@Param("roomId") Long roomId);
}
