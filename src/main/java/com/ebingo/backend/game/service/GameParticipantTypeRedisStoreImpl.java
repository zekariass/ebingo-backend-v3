package com.ebingo.backend.game.service;

import com.ebingo.backend.game.enums.ParticipantType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameParticipantTypeRedisStoreImpl implements GameParticipantTypeRedisStore {

    private final ReactiveStringRedisTemplate redis;

    private static final String KEY_PREFIX = "game:";
    private static final String KEY_SUFFIX = ":participants";


    private String key(long gameId) {
        return KEY_PREFIX + gameId + KEY_SUFFIX;
    }

    private static String enc(ParticipantType t) {
        return t.name(); // "REAL" / "BOT"
    }

    private static ParticipantType dec(Object v) {
        if (v == null) return null;
        return ParticipantType.valueOf(v.toString());
    }

    @Override
    public Mono<Boolean> put(long gameId, String cardId, ParticipantType type) {
        return redis.opsForHash().put(key(gameId), cardId, enc(type));
    }

    /**
     * Store many cardIds for the same participant type.
     * Returns how many cardIds were processed (attempted).
     * <p>
     * Uses putAll for efficiency (single round trip).
     */
    @Override
    public Mono<Long> putCards(long gameId, Iterable<String> cardIds, ParticipantType type) {
        String k = key(gameId);

        return Flux.fromIterable(cardIds)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collectList()
                .flatMap(list -> {
                    if (list.isEmpty()) return Mono.just(0L);

                    Map<String, String> payload = new LinkedHashMap<>();
                    String value = enc(type);
                    for (String cardId : list) {
                        payload.put(cardId, value);
                    }

                    return redis.opsForHash()
                            .putAll(k, payload)
                            // putAll returns Mono<Boolean> (success), but not count
                            .thenReturn((long) list.size());
                });
    }

    @Override
    public Mono<ParticipantType> get(long gameId, String cardId) {
        return redis.opsForHash()
                .get(key(gameId), cardId)
                .map(GameParticipantTypeRedisStoreImpl::dec);
        // if missing -> Mono.empty()
    }

    @Override
    public Mono<Boolean> deleteGame(long gameId) {
        return redis.delete(key(gameId)).map(n -> n > 0);
    }

    @Override
    public Mono<Boolean> expire(long gameId, Duration ttl) {
        return redis.expire(key(gameId), ttl);
    }
}
