package com.ebingo.backend.payment.entity;

import com.ebingo.backend.payment.enums.GameTxnStatus;
import com.ebingo.backend.payment.enums.GameTxnType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("game_transaction")
public class GameTransaction {

    @Id
    private Long id;

    @Column("game_id")
    private Long gameId;

    @Column("player_id")
    private Long playerId;

    @Column("agent_id")
    private Long agentId;

    @Column("txn_amount")
    private BigDecimal txnAmount;

    @Column("single_game_fee")
    private BigDecimal singleGameFee;

    @Column("commission_amount")
    private BigDecimal commissionAmount;

    @Column("txn_type")
    private GameTxnType txnType; // e.g., GAME_FEE, PRIZE_PAYOUT, REFUND, DISPUTE

    @Column("txn_status")
    private GameTxnStatus txnStatus; // e.g., PENDING, SUCCESS, FAIL, AWAITING_APPROVAL

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}
