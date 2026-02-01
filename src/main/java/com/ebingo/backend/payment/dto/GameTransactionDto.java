package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.GameTxnStatus;
import com.ebingo.backend.payment.enums.GameTxnType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GameTransactionDto {
    private Long id;
    private Long gameId;
    private Long playerId;
    private Long agentId;
    private BigDecimal txnAmount;
    private BigDecimal singleGameFee;
    private BigDecimal commissionAmount;
    private GameTxnType txnType;
    private GameTxnStatus txnStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
