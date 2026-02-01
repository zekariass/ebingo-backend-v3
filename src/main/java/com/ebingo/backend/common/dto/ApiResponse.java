package com.ebingo.backend.common.dto;



import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final int statusCode;
    private final String message;
    private final String error;
    private final Map<String, String> errors;
    private final String path;
    private final T data;
    @Builder.Default
    private final Instant timestamp = Instant.now();
}
