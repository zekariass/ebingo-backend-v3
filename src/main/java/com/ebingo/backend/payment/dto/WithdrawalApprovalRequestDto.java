package com.ebingo.backend.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WithdrawalApprovalRequestDto {
    private Long orderId;
    private Long agentId;
    private boolean approve; // true = approve, false = reject
    private String reason;   // optional message, used on rejection
}
