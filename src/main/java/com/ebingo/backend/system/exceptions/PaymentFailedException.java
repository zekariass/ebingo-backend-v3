package com.ebingo.backend.system.exceptions;

import java.math.BigDecimal;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(Long userId, BigDecimal entryFee) {
        super("Payment failed for user " + userId + " (amount: " + entryFee + ")");
    }

    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentFailedException(Throwable cause) {
        super(cause);
    }
}