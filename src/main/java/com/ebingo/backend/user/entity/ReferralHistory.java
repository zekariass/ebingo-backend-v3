package com.ebingo.backend.user.entity;

import com.ebingo.backend.user.enums.ReferralStatus;
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
@Table("referral_history")
public class ReferralHistory {

    @Id
    private Long id;

    @Column("referrer_id")
    private Long referrerId;

    @Column("referee_id")
    private Long refereeId;

    @Column("amount")
    private BigDecimal amount;

    @Column("status")
    private ReferralStatus status;

    @Column("failure_reason")
    private String failureReason;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

}
