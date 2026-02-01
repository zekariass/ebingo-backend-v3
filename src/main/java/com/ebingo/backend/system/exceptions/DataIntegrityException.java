package com.ebingo.backend.system.exceptions;

public class DataIntegrityException extends RuntimeException {
    public DataIntegrityException() {
        super("Data integrity exception.");
    }

    public DataIntegrityException(String message) {
        super(message);
    }

    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataIntegrityException(Throwable cause) {
        super(cause);
    }
}
