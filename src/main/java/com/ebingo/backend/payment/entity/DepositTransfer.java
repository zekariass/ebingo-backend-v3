package com.ebingo.backend.payment.entity;

import com.ebingo.backend.payment.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@Table("deposit_transfer")
public class DepositTransfer {

    @Id
    private Long id;

    @Column("agent_id")
    private Long agentId;

    @Column("sender_id")
    private Long senderId;

    @Column("receiver_id")
    private Long receiverId;

    private BigDecimal amount;

    @Column("status")
    private TransferStatus status; // e.g., PENDING, SUCCESS, FAIL, AWAITING_APPROVAL, CANCELLED

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}


