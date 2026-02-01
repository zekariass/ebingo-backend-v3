package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.WithdrawalMode;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WithdrawalResponseDto {
    private Long agentId;
    private String data;
    private PaymentOrderStatus status;
    private String detail;
    private String message;
    private WithdrawalMode withdrawalMode;
}
