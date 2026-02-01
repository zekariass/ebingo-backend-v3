//package com.ebingo.backend.system.exceptions;
//
//import com.ebingo.backend.common.dto.ApiResponse;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Component
//public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Override
//    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
//
//        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
//                .success(false)
//                .statusCode(HttpStatus.FORBIDDEN.value())
//                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
//                .message(denied.getMessage())
//                .path(exchange.getRequest().getPath().value())
//                .build();
//
//        byte[] bytes;
//        try {
//            bytes = objectMapper.writeValueAsBytes(apiResponse);
//        } catch (Exception e) {
//            bytes = "{}".getBytes();
//        }
//
//        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
//        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
//
//        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
//                .bufferFactory().wrap(bytes)));
//    }
//}
