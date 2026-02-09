package com.ebingo.backend.externalgame.repository;

import com.ebingo.backend.externalgame.entity.ExternalGameSession;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface ExternalGameSessionRepository extends ReactiveCrudRepository<ExternalGameSession, String> {

    @Query("SELECT * FROM external_game_sessions WHERE session_token = :sessionToken AND status = 'ACTIVE' AND expires_at > :now")
    Mono<ExternalGameSession> findActiveSession(String sessionToken, Instant now);

    Mono<ExternalGameSession> findBySessionToken(String sessionToken);
}
