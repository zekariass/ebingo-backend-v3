package com.ebingo.backend.system.exceptions;

import com.ebingo.backend.common.dto.ApiResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleRuntimeException(RuntimeException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), exchange);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalStateException(IllegalStateException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalArgumentException(IllegalArgumentException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(NullPointerException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleNullPointerException(NullPointerException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, "Null pointer exception occurred", exchange);
    }


    @org.springframework.web.bind.annotation.ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(WebExchangeBindException ex) {

        // Convert field errors to a map of field -> error message
        Map<String, String> fieldErrors = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage()
                ));

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .error("Bad Request")
                .errors(fieldErrors)
                .path(ex.getBindingResult().getTarget() != null
                        ? ex.getBindingResult().getTarget().toString()
                        : null)
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return buildApiResponse(errors, HttpStatus.BAD_REQUEST, "Validation failed", exchange);
    }


    @ExceptionHandler(DuplicateUserException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleDuplicateUserException(DuplicateUserException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    @ExceptionHandler(DataIntegrityException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleDataIntegrityException(DataIntegrityException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleUnauthorizedException(UnauthorizedException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.FORBIDDEN, ex.getMessage(), exchange);
    }

    @ExceptionHandler(UserCreationException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleUserCreationException(UserCreationException ex, ServerWebExchange exchange) {
        String rootMessage = getRootCauseMessage(ex);

        HttpStatus status = (rootMessage.contains("uq_") || rootMessage.contains("unique constraint"))
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        return buildApiResponse(null, status, rootMessage, exchange);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleResourceNotFoundException(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleInsufficientBalanceException(InsufficientBalanceException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(BadRequestException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleBadRequestException(BadRequestException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }
//
//    @ExceptionHandler(AuthorizationDeniedException.class)
//    public Mono<ResponseEntity<ApiResponse<Object>>> handleAuthorizationDeniedException(AuthorizationDeniedException ex, ServerWebExchange exchange) {
//        return buildApiResponse(null, HttpStatus.FORBIDDEN, ex.getMessage(), exchange);
//    }


//    @ExceptionHandler(AuthenticationException.class)
//    public Mono<ResponseEntity<ApiResponse<Object>>> handleAuthenticationException(AuthorizationDeniedException ex, ServerWebExchange exchange) {
//        return buildApiResponse(null, HttpStatus.UNAUTHORIZED, ex.getMessage(), exchange);
//    }


    // Handle BadSqlGrammarException specifically
    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadSqlGrammar(BadSqlGrammarException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Database query failed due to invalid SQL syntax.")
                .error("Internal Server Error")
                .errors(Collections.singletonMap("sqlError", ex.getMessage()))
                .path("") // You can inject current path if needed
                .timestamp(Instant.now())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle general DataAccessException
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccess(DataAccessException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Database operation failed.")
                .error("Internal Server Error")
                .errors(Collections.singletonMap("dbError", ex.getMostSpecificCause().getMessage()))
                .path("")
                .timestamp(Instant.now())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Catch-all handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(ex.getMessage())
                .error("Internal Server Error")
                .errors(Collections.singletonMap("exception", ex.getClass().getSimpleName()))
                .path("")
                .timestamp(Instant.now())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    @ExceptionHandler(TelegramAuthException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleTelegramAuthException(TelegramAuthException ex, ServerWebExchange exchange) {
        return buildApiResponse(null, HttpStatus.UNAUTHORIZED, ex.getMessage(), exchange);
    }

    // ---------------- Handle Data Integrity Violations ----------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {

        String detailedMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Data integrity violation. Please check your input.")
                .error("Bad Request")
                .errors(Collections.singletonMap("dataIntegrityError", detailedMessage))
                .path("") // optionally inject request path
                .timestamp(Instant.now())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private Mono<ResponseEntity<ApiResponse<Object>>> buildApiResponse(Map<String, String> errors, HttpStatus status, String message, ServerWebExchange exchange) {
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .statusCode(status.value())
                .error(status.getReasonPhrase())
                .errors(errors)
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .success(false)
                .timestamp(Instant.now())
                .build();

        return Mono.just(ResponseEntity.status(status).body(apiResponse));
    }

}
