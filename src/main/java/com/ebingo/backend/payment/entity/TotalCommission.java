package com.ebingo.backend.payment.entity;

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
@Table("total_commission")
public class TotalCommission {
    @Id
    private Long id;

    @Column("total_commission")
    private BigDecimal totalCommission = BigDecimal.ZERO;

    @Column("total_prize")
    private BigDecimal totalPrize = BigDecimal.ZERO;

    @Column("last_withdrawal_amount")
    private BigDecimal lastWithdrawalAmount = BigDecimal.ZERO;

    @Column("total_withdrawal")
    private BigDecimal totalWithdrawal = BigDecimal.ZERO;

    @Column("last_withdrawal_at")
    private Instant lastWithdrawalAt;

    @Column("created_at")
    @CreatedDate
    private Instant createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
