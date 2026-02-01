package com.ebingo.backend.system.exceptions;

import org.springframework.http.HttpStatus;

public class TelegramAuthException extends RuntimeException {

    private final HttpStatus status;

    public TelegramAuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // Default to UNAUTHORIZED if no status provided
    public TelegramAuthException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public TelegramAuthException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public TelegramAuthException(Throwable cause) {
        super(cause);
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
