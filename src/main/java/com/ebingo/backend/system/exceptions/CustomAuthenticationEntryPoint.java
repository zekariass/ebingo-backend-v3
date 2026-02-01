//package com.ebingo.backend.system.exceptions;
/// /
/// /import com.ebingo.backend.common.dto.ApiResponse;
/// /import com.fasterxml.jackson.databind.ObjectMapper;
/// /import org.springframework.http.HttpStatus;
/// /import org.springframework.security.core.AuthenticationException;
/// /import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
/// /import org.springframework.stereotype.Component;
/// /import org.springframework.web.server.ServerWebExchange;
/// /import reactor.core.publisher.Mono;
/// /
/// /@Component
/// /public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
/// /
/// /    private final ObjectMapper objectMapper = new ObjectMapper();
/// /
/// /    @Override
/// /    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
/// /
/// /        // CORS headers
/// /        String origin = exchange.getRequest().getHeaders().getOrigin();
/// /        if (origin != null) {
/// /            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", origin);
/// /        } else {
/// /            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
/// /        }
/// /        exchange.getResponse().getHeaders().add("Access-Control-Allow-Credentials", "true");
/// /        exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
/// /        exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
/// /        exchange.getResponse().getHeaders().add("Vary", "Origin");
/// /
/// /        ApiResponse<Object> apiResponse = ApiResponse.builder()
/// /                .success(false)
/// /                .statusCode(HttpStatus.UNAUTHORIZED.value())
/// /                .message(authException.getMessage())
/// /                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
/// /                .path(exchange.getRequest().getPath().value())
/// /                .build();
/// /
/// /        byte[] bytes;
/// /        try {
/// /            bytes = objectMapper.writeValueAsBytes(apiResponse);
/// /        } catch (Exception e) {
/// /            bytes = "{}".getBytes();
/// /        }
/// /
/// /        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
/// /        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
/// /
/// /        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
/// /                .bufferFactory().wrap(bytes)));
/// /    }
/// /}
/// /
//
//
//import com.ebingo.backend.common.dto.ApiResponse;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.nio.charset.StandardCharsets;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
//
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
//
//        ApiResponse<Object> apiResponse = ApiResponse.builder()
//                .success(false)
//                .statusCode(HttpStatus.UNAUTHORIZED.value())
//                .message("Unauthorized") // generic for security
//                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
//                .path(exchange.getRequest().getPath().value())
//                .build();
//
//        byte[] bytes;
//        try {
//            bytes = objectMapper.writeValueAsBytes(apiResponse);
//        } catch (Exception e) {
//            log.error("Failed to serialize auth response", e);
//            bytes = "{}".getBytes(StandardCharsets.UTF_8);
//        }
//
//        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//        exchange.getResponse().getHeaders().set("Content-Type", "application/json");
//
//        return exchange.getResponse().writeWith(
//                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
//        );
//    }
//}
