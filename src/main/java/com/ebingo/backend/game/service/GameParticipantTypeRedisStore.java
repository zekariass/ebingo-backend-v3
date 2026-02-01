package com.ebingo.backend.game.service;

import com.ebingo.backend.game.enums.ParticipantType;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface GameParticipantTypeRedisStore {

    Mono<Boolean> put(long gameId, String cardId, ParticipantType type);

    Mono<Long> putCards(long gameId, Iterable<String> cardIds, ParticipantType type);

    Mono<ParticipantType> get(long gameId, String cardId);

    Mono<Boolean> deleteGame(long gameId);

    Mono<Boolean> expire(long gameId, Duration ttl);
}
