package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentOrderDto {
    private Long id;
    private Long userId;
    private Long agentId;
    private String txnRef;
    private String providerOrderRef;
    private BigDecimal amount;
    private String currency;
    private PaymentOrderStatus status;
    private String reason;
    private Long paymentMethodId;
    private String instructionsUrl;
    private TransactionType txnType;
    private String metaData;
    private String nonce;
    private String phoneNumber;
    private Long approvedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
