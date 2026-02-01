package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderGetParamsDto {
    private String phoneNumber;
    private Long agentId;
    private PaymentOrderStatus status;
    private TransactionType txnType;
    private int page;
    private int size;
}
