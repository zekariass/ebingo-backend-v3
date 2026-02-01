package com.ebingo.backend.system.redis;

import com.ebingo.backend.game.dto.CardInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // ===== Redis message listener (for pub/sub) =====
    @Bean
    public ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }

    // ===== Generic Object template =====
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ebingo.backend.game.state.")
                .allowIfSubType("com.ebingo.backend.game.dto.")
                .allowIfSubType("com.ebingo.backend.user.dto.")
                .allowIfSubType("com.ebingo.backend.payment.dto.")
                .allowIfSubType("com.ebingo.backend.system.dto.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .build();

        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Object.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Object> context = builder
                .key(new StringRedisSerializer())
                .value(serializer)
                .hashKey(new StringRedisSerializer())
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    // ===== String-only template (primary) =====
    @Bean
    public ReactiveRedisTemplate<String, String> redisStringTemplate(
            ReactiveRedisConnectionFactory factory) {

        RedisSerializationContext<String, String> context =
                RedisSerializationContext.<String, String>newSerializationContext(new StringRedisSerializer())
                        .key(new StringRedisSerializer())
                        .value(new StringRedisSerializer())
                        .hashKey(new StringRedisSerializer())
                        .hashValue(new StringRedisSerializer())
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    // ===== ReactiveSetOperations for String =====
    @Bean
    public ReactiveSetOperations<String, String> reactiveStringSetOperations(
            @Qualifier("redisStringTemplate") ReactiveRedisTemplate<String, String> stringRedisTemplate) {
        return stringRedisTemplate.opsForSet();
    }

    // ===== CardInfo template and hash operations =====
    @Bean
    public ReactiveRedisTemplate<String, CardInfo> cardInfoRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<CardInfo> serializer =
                new Jackson2JsonRedisSerializer<>(CardInfo.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, CardInfo> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, CardInfo> context = builder
                .key(new StringRedisSerializer())
                .value(serializer)
                .hashKey(new StringRedisSerializer())
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveHashOperations<String, String, CardInfo> cardInfoHashOperations(
            @Qualifier("cardInfoRedisTemplate") ReactiveRedisTemplate<String, CardInfo> cardInfoRedisTemplate) {
        return cardInfoRedisTemplate.opsForHash();
    }

    // ===== ReactiveHashOperations for Object =====
    @Bean
    public ReactiveHashOperations<String, String, Object> objectHashOperations(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForHash();
    }

    // ===== ReactiveSetOperations for Object =====
    @Bean
    public ReactiveSetOperations<String, Object> objectSetOperations(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForSet();
    }

    // ===== Long template and set operations =====
    @Bean
    public ReactiveRedisTemplate<String, Long> longRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        RedisSerializationContext<String, Long> context =
                RedisSerializationContext.<String, Long>newSerializationContext(keySerializer)
                        .key(keySerializer)
                        .value(new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Long.class))
                        .hashKey(keySerializer)
                        .hashValue(new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Long.class))
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    @Bean
    public ReactiveSetOperations<String, Long> longSetOperations(
            @Qualifier("longRedisTemplate") ReactiveRedisTemplate<String, Long> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForSet();
    }
}
