package com.ebingo.backend.game.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GameMetrics {
    private final Counter cardSelections;
    private final Counter claims;
    private final Gauge activeGames;

    private final AtomicInteger activeGamesCount = new AtomicInteger(0);

    public GameMetrics(MeterRegistry registry) {
        this.cardSelections = Counter.builder("bingo_card_selections").register(registry);
        this.claims = Counter.builder("bingo_claims").register(registry);
        this.activeGames = Gauge.builder("bingo_active_games", activeGamesCount, AtomicInteger::get).register(registry);
    }

    public void incSelections() { cardSelections.increment(); }
    public void incClaims() { claims.increment(); }
    public void setActiveGames(int count) { activeGamesCount.set(count); }
}

