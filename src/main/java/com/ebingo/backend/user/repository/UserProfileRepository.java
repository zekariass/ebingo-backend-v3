package com.ebingo.backend.user.repository;

import com.ebingo.backend.user.entity.UserProfile;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface UserProfileRepository extends ReactiveCrudRepository<UserProfile, Long> {
    Mono<UserProfile> findByPhoneNumberAndAgentIdAndIsDeletedFalse(String phoneNumber, Long agentId);

    Mono<UserProfile> findByTelegramIdAndAgentIdAndIsDeletedFalse(Long telegramId, Long agentId);

    @Query("SELECT telegram_id FROM user_profile where is_bot = false AND agent_id = :agentId AND is_deleted = false")
    Flux<Long> findAllUserTelegramIdsByAgentId(Long agentId);

    @Query("SELECT * FROM user_profile WHERE is_bot = true AND bot_room_id = :roomId AND is_deleted = false")
    Flux<UserProfile> findBotsByRoom(@Param("roomId") Long roomId);

    /**
     * Explicit INSERT for bot profiles with a caller-supplied id.
     * Required because R2DBC treats entities with a non-null @Id as updates.
     */
    @Modifying
    @Query("INSERT INTO user_profile (id, telegram_id, referrer_id, first_name, last_name, nickname, " +
            "phone_number, status, \"role\", is_deleted, is_bot, bot_room_id, \"password\", agent_id, created_at, updated_at) " +
            "VALUES (:id, :telegramId, NULL, :firstName, :lastName, :nickname, :phoneNumber, 'ACTIVE', 'PLAYER', " +
            "false, true, :botRoomId, NULL, :agentId, :createdAt, :updatedAt)")
    Mono<Integer> insertBotUser(@Param("id") Long id,
                                @Param("telegramId") Long telegramId,
                                @Param("firstName") String firstName,
                                @Param("lastName") String lastName,
                                @Param("nickname") String nickname,
                                @Param("phoneNumber") String phoneNumber,
                                @Param("botRoomId") Long botRoomId,
                                @Param("agentId") Long agentId,
                                @Param("createdAt") LocalDateTime createdAt,
                                @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Counts existing rows that would collide with a bulk bot insert:
     * id or telegram_id in [startId, endId], or phone_number in [startPhone, endPhone].
     */
    @Query("SELECT COUNT(*) FROM user_profile WHERE " +
            "(id BETWEEN :startId AND :endId) OR " +
            "(telegram_id BETWEEN :startId AND :endId) OR " +
            "(phone_number BETWEEN :startPhone AND :endPhone)")
    Mono<Long> countConflictingBotUsers(@Param("startId") Long startId,
                                        @Param("endId") Long endId,
                                        @Param("startPhone") String startPhone,
                                        @Param("endPhone") String endPhone);
}
