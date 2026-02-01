package com.ebingo.backend.game.service.autoplay;

import lombok.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class GlobalAutoPlayManager {

    @Value
    public static class AutoPlayEvent {
        boolean enabled;
        String reason;
    }

    private volatile boolean enabled = true;

    private final Sinks.Many<AutoPlayEvent> sink =
            Sinks.many().replay().latest(); // replay latest event for new subscribers

    public boolean isEnabled() {
        return enabled;
    }

    public Mono<Void> enable(String reason) {
        this.enabled = true;
        sink.tryEmitNext(new AutoPlayEvent(true, reason));
        return Mono.empty();
    }

    public Mono<Void> disable(String reason) {
        this.enabled = false;
        sink.tryEmitNext(new AutoPlayEvent(false, reason));
        return Mono.empty();
    }

    public Flux<AutoPlayEvent> events() {
        return sink.asFlux();
    }
}
