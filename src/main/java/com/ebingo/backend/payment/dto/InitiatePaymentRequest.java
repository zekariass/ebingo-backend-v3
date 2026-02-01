package com.ebingo.backend.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitiatePaymentRequest {
    private String uuid;
    private Long agentId;
    private String phone_number;
    private String amount;
    private String merchant_name;
    private String selected_service;
    private String selected_bank;
}
