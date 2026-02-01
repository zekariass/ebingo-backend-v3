package com.ebingo.backend.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentInitiateRequestDto {
    private String uuid;
    private String phoneNumber;
    private String encryptedTotalAmount;
    private String merchantName;
    private String selectedService;
    private String selectedBank;
    private Long agentId;
}
