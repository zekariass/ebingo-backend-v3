package com.ebingo.backend.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InitiateDepositRequest {
    private Long paymentMethodId;
    private BigDecimal amount;
    private Long agentId;
}
