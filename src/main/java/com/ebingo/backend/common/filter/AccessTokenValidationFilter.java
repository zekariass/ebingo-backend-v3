package com.ebingo.backend.common.filter;

import com.ebingo.backend.common.annotation.RequireAccessToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AccessTokenValidationFilter implements WebFilter {

    private final RequestMappingHandlerMapping handlerMapping;
    private final String expectedAccessToken;

    public AccessTokenValidationFilter(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            @Value("${endpoints.access-token}") String expectedAccessToken) {
        this.handlerMapping = handlerMapping;
        this.expectedAccessToken = expectedAccessToken;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return handlerMapping.getHandler(exchange)
                .filter(handler -> handler instanceof HandlerMethod)
                .cast(HandlerMethod.class)
                .flatMap(handlerMethod -> {
                    if (!requiresAccessToken(handlerMethod)) {
                        return chain.filter(exchange);
                    }

                    String accessToken = exchange.getRequest().getHeaders().getFirst("X-Access-Token");

                    if (expectedAccessToken.equals(accessToken)) {
                        return chain.filter(exchange);
                    }

                    log.warn("Access token validation failed for path: {}", exchange.getRequest().getPath().value());
                    return writeUnauthorizedResponse(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private boolean requiresAccessToken(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(RequireAccessToken.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequireAccessToken.class);
    }

    private Mono<Void> writeUnauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"statusCode\":401,\"success\":false,\"message\":\"Invalid or missing access token\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
