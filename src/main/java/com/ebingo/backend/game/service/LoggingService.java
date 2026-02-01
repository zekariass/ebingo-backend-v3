package com.ebingo.backend.game.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class LoggingService {
    public void logEvent(String type, Map<String, Object> payload) {
        log.info("event={} payload={}", type, payload);
    }
}

