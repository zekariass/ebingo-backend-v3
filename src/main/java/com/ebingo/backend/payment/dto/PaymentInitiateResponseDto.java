package com.ebingo.backend.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentInitiateResponseDto {
    private String message;
    private String details;
    private Integer statusCode;
    private Object data;
    private Long agentId;
}
