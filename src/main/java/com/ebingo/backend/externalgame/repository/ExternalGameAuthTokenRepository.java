package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.ExternalGameAuthToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface ExternalGameAuthTokenRepository extends ReactiveCrudRepository<ExternalGameAuthToken, String> {

    @Query("SELECT * FROM external_game_auth_tokens WHERE token = :token AND status = 'ACTIVE' AND expires_at > :now")
    Mono<ExternalGameAuthToken> findActiveToken(String token, Instant now);

    Mono<ExternalGameAuthToken> findByToken(String token);
}
