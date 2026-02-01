package com.ebingo.backend.game.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class BingoGameMetricsRedisService {

    private final ReactiveStringRedisTemplate redis;

    // Redis key + fields
    private static final String F_BOT_CARDS = "botCardsCount";
    private static final String F_REAL_CARDS = "realUserCardsCount";
    private static final String F_REAL_MONEY = "realMoneyAmount";

    /**
     * CREATE-OR-GET (atomic):
     * - Ensures fields exist (default "0") without overwriting existing values.
     * - Returns current values.
     * <p>
     * KEYS[1] = key
     */
    private static final DefaultRedisScript<List> CREATE_OR_GET_LUA = new DefaultRedisScript<>(
            """
                    local k = KEYS[1]
                    
                    -- Set defaults only if fields are missing
                    if redis.call('HEXISTS', k, 'botCardsCount') == 0 then
                      redis.call('HSET', k, 'botCardsCount', '0')
                    end
                    if redis.call('HEXISTS', k, 'realUserCardsCount') == 0 then
                      redis.call('HSET', k, 'realUserCardsCount', '0')
                    end
                    if redis.call('HEXISTS', k, 'realMoneyAmount') == 0 then
                      redis.call('HSET', k, 'realMoneyAmount', '0')
                    end
                    
                    local bot = redis.call('HGET', k, 'botCardsCount') or "0"
                    local real = redis.call('HGET', k, 'realUserCardsCount') or "0"
                    local money = redis.call('HGET', k, 'realMoneyAmount') or "0"
                    
                    return { bot, real, money }
                    """,
            List.class
    );

    /**
     * ADD/DEDUCT (atomic):
     * - Applies deltas to all fields in one script.
     * - Prevents values going below zero (important under concurrency).
     * - Returns updated values.
     * <p>
     * ARGV[1] botCardsDelta (integer)
     * ARGV[2] realCardsDelta (integer)
     * ARGV[3] realMoneyDelta (decimal)
     */
    private static final DefaultRedisScript<List> ADD_DELTAS_LUA = new DefaultRedisScript<>(
            """
                    local k = KEYS[1]
                    local botDelta = tonumber(ARGV[1]) or 0
                    local realDelta = tonumber(ARGV[2]) or 0
                    local moneyDelta = tonumber(ARGV[3]) or 0
                    
                    -- Ensure fields exist
                    local botCur = tonumber(redis.call('HGET', k, 'botCardsCount') or "0")
                    local realCur = tonumber(redis.call('HGET', k, 'realUserCardsCount') or "0")
                    local moneyCur = tonumber(redis.call('HGET', k, 'realMoneyAmount') or "0")
                    
                    local botNew = botCur + botDelta
                    local realNew = realCur + realDelta
                    local moneyNew = moneyCur + moneyDelta
                    
                    -- Guard against negative results (concurrency-safe constraint)
                    if botNew < 0 then
                      return redis.error_reply("botCardsCount would go negative")
                    end
                    if realNew < 0 then
                      return redis.error_reply("realUserCardsCount would go negative")
                    end
                    if moneyNew < 0 then
                      return redis.error_reply("realMoneyAmount would go negative")
                    end
                    
                    -- Write back atomically
                    redis.call('HSET', k, 'botCardsCount', tostring(botNew))
                    redis.call('HSET', k, 'realUserCardsCount', tostring(realNew))
                    redis.call('HSET', k, 'realMoneyAmount', tostring(moneyNew))
                    
                    return { tostring(botNew), tostring(realNew), tostring(moneyNew) }
                    """,
            List.class
    );

    /**
     * SET ABSOLUTE VALUES (atomic):
     * - Overwrites all three fields in one script.
     * - Validates non-negative.
     * - Returns updated values.
     * <p>
     * ARGV[1] botCards (integer)
     * ARGV[2] realCards (integer)
     * ARGV[3] realMoney (decimal)
     */
    private static final DefaultRedisScript<List> SET_METRICS_LUA = new DefaultRedisScript<>(
            """
                    local k = KEYS[1]
                    local bot = tonumber(ARGV[1]) or 0
                    local real = tonumber(ARGV[2]) or 0
                    local money = tonumber(ARGV[3]) or 0
                    
                    if bot < 0 then return redis.error_reply("botCardsCount must be >= 0") end
                    if real < 0 then return redis.error_reply("realUserCardsCount must be >= 0") end
                    if money < 0 then return redis.error_reply("realMoneyAmount must be >= 0") end
                    
                    redis.call('HSET', k, 'botCardsCount', tostring(bot))
                    redis.call('HSET', k, 'realUserCardsCount', tostring(real))
                    redis.call('HSET', k, 'realMoneyAmount', tostring(money))
                    
                    return { tostring(bot), tostring(real), tostring(money) }
                    """,
            List.class
    );

    public BingoGameMetricsRedisService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    // ----------------------------------------------------------------------
    // Public API (all atomic)
    // ----------------------------------------------------------------------

    /**
     * Atomic init + read. Safe under concurrent calls.
     */
    public Mono<Metrics> createOrGet(long gameId) {
        String key = key(gameId);
        return redis.execute(CREATE_OR_GET_LUA, List.of(key))
                .single()
                .map(res -> toMetrics(gameId, res));
    }

    /**
     * Atomic add/subtract (with non-negative protection).
     */
    public Mono<Metrics> addDeltas(long gameId, long botCardsDelta, long realUserCardsDelta, BigDecimal realMoneyDelta) {
        if (realMoneyDelta == null) realMoneyDelta = BigDecimal.ZERO;

        String key = key(gameId);
        return redis.execute(
                        ADD_DELTAS_LUA,
                        List.of(key),
                        Long.toString(botCardsDelta),
                        Long.toString(realUserCardsDelta),
                        realMoneyDelta.toPlainString()
                )
                .single()
                .map(res -> toMetrics(gameId, res));
    }

    /**
     * Convenience wrappers
     */
    public Mono<Metrics> addBotCards(long gameId, long delta) {
        return addDeltas(gameId, delta, 0, BigDecimal.ZERO);
    }

    public Mono<Metrics> addRealUserCards(long gameId, long delta) {
        return addDeltas(gameId, 0, delta, BigDecimal.ZERO);
    }

    public Mono<Metrics> addRealMoneyAmount(long gameId, BigDecimal delta) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>: ADDING REAL MONEY TO GAME: {}", delta);
        return addDeltas(gameId, 0, 0, delta);
    }

    /**
     * Atomic absolute set.
     */
    public Mono<Metrics> setMetrics(long gameId, long botCardsCount, long realUserCardsCount, BigDecimal realMoneyAmount) {
        if (realMoneyAmount == null) realMoneyAmount = BigDecimal.ZERO;

        String key = key(gameId);
        return redis.execute(
                        SET_METRICS_LUA,
                        List.of(key),
                        Long.toString(botCardsCount),
                        Long.toString(realUserCardsCount),
                        realMoneyAmount.toPlainString()
                )
                .single()
                .map(res -> toMetrics(gameId, res));
    }

    /**
     * Atomic read (no script needed; reading is safe).
     */
//    public Mono<Metrics> getMetrics(long gameId) {
//        // If you want reads to also auto-init missing fields, just call createOrGet(gameId) instead.
//        String key = key(gameId);
//        return redis.opsForHash()
//                .multiGet(key, List.of(F_BOT_CARDS, F_REAL_CARDS, F_REAL_MONEY))
//                .map(res -> new Metrics(
//                        gameId,
//                        parseLong(res.getFirst()),
//                        parseLong(res.get(1)),
//                        parseBigDecimal(res.get(2))
//                ));
//    }
    public Mono<Metrics> getMetrics(long gameId) {
        String key = key(gameId);

        return redis.opsForHash()
                .multiGet(key, List.of(F_BOT_CARDS, F_REAL_CARDS, F_REAL_MONEY))
                .map(res -> {
                    Object botCards = !res.isEmpty() ? res.get(0) : null;
                    Object realCards = res.size() > 1 ? res.get(1) : null;
                    Object realMoney = res.size() > 2 ? res.get(2) : null;

                    return new Metrics(
                            gameId,
                            parseLong(botCards),
                            parseLong(realCards),
                            parseBigDecimal(realMoney)
                    );
                });
    }


    public Mono<Boolean> cleanupGame(long gameId) {
        return redis.delete(key(gameId)).map(deleted -> deleted != null && deleted > 0);
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private static String key(long gameId) {
        return "game:" + gameId + ":bingo";
    }

    private static Metrics toMetrics(long gameId, List res) {
        return new Metrics(
                gameId,
                parseLong(res.get(0)),
                parseLong(res.get(1)),
                parseBigDecimal(res.get(2))
        );
    }

    private static long parseLong(Object v) {
        if (v == null) return 0L;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return 0L;
        return Long.parseLong(s);
    }

    private static BigDecimal parseBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(s);
    }

    public record Metrics(
            long gameId,
            long botCardsCount,
            long realUserCardsCount,
            BigDecimal realMoneyAmount
    ) {
    }
}
