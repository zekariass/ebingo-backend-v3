package com.ebingo.backend.game.service;

import com.ebingo.backend.system.redis.RedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper;

//    private String gameChannel(Long gameId) {
//        return "game:" + gameId;
//    }

    /**
     * Publish an event to the Redis channel for a specific game.
     */
    public Mono<Long> publishEvent(String channel, Map<String, Object> event) {
        try {
            String json = mapper.writeValueAsString(event);
            return redis.convertAndSend(channel, json);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Publish an event to a raw channel name.
     */
    public Mono<Long> publishRaw(String channel, Object event) {
        try {
            String json = mapper.writeValueAsString(event);
            return redis.convertAndSend(channel, json);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    // New user-specific method
    public Mono<Long> publishUserEvent(String userId, Map<String, Object> message) {
        String userChannel = RedisKeys.userChannel(userId);
        String messageJson = null;
        try {
            messageJson = mapper.writeValueAsString(message);
            return redis.convertAndSend(userChannel, messageJson);

        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }


}
