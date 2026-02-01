package com.ebingo.backend.payment.enums;

public enum PaymentOrderStatus {
    PENDING, // created but not yet processed
    INITIATED,
    AWAITING_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED,
    REJECTED

}