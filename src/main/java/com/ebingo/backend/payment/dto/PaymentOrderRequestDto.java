package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderRequestDto {
    private Long userId;
    private Long agentId;
    private BigDecimal amount;
    private String currency;
    private Long paymentMethodId; // e.g. ADDISPAY, TELEBIRR
    private String reason;
    private TransactionType txnType;
    private String phoneNumber;
    private Map<String, Object> metadata;
}
