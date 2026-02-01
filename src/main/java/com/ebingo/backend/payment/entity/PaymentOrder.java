package com.ebingo.backend.payment.entity;

import com.ebingo.backend.payment.enums.PaymentOrderStatus;
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
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_order")
public class PaymentOrder {
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("agent_id")
    private Long agentId;

    @Column("txn_ref")
    private String txnRef;

    private String phoneNumber;

    @Column("provider_order_ref")
    private String providerOrderRef; // null if offline

    private BigDecimal amount;
    private String currency;
    private PaymentOrderStatus status;
    private String reason;

    @Column("payment_method_id")
    private Long paymentMethodId;

    @Column("instructions_url")
    private String instructionsUrl; // for offline payments

    @Column("txn_type")
    private TransactionType txnType;

    @Column("nonce")
    private String nonce;

    @Column("meta_data")
    private String metaData;

    @Column("approved_by")
    private Long approvedBy;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;


    public void generateAndSetTxnRef() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        int randomPart = (int) (Math.random() * 900) + 100; // ensures 3-digit number: 100–999

        this.txnRef = "PO-" + uuidPart + randomPart;
    }
}


