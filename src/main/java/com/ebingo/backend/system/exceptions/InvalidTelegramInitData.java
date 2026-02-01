package com.ebingo.backend.system.exceptions;

public class InvalidTelegramInitData extends RuntimeException {

    public InvalidTelegramInitData() {
        super("Invalid Telegram initialization data");
    }

    public InvalidTelegramInitData(String message) {
        super(message);
    }

    public InvalidTelegramInitData(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTelegramInitData(Throwable cause) {
        super(cause);
    }
}

