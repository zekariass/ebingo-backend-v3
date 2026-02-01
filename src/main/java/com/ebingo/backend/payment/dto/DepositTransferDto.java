package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.TransferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DepositTransferDto {
    private Long id;
    private Long senderId;
    private Long agentId;
    private Long receiverId;
    private BigDecimal amount;
    private TransferStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
