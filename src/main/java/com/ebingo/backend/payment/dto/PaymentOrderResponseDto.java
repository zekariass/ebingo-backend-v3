package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentOrderResponseDto {
    private Long orderId;
    private Long agentId;
    private String txnRef;
    private PaymentOrderStatus status;
    private BigDecimal amount;
    private String providerUuid;
    private String checkoutUrl;     // for addispay
    private String instructionsUrl; // for offline methods
    private JsonNode checkData; // additional data like QR code info
    private Long paymentMethodId;
}
