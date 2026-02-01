package com.ebingo.backend.payment.entity;

import com.ebingo.backend.payment.enums.TransactionStatus;
import com.ebingo.backend.payment.enums.TransactionType;
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
@Table("transactions")
public class Transaction {

    @Id
    private Long id;

    @Column("player_id")
    private Long playerId;

    @Column("order_id")
    private Long orderId;

    @Column("agent_id")
    private Long agentId;

    @Column("txn_ref")
    private String txnRef;

    @Column("payment_method_id")
    private Long paymentMethodId;

    @Column("txn_type")
    private TransactionType txnType;

    @Column("txn_amount")
    private BigDecimal txnAmount;

    private TransactionStatus status;

    @Column("meta_data")
    private String metaData; // JSON string

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}

