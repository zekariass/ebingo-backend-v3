package com.ebingo.backend.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("daily_commission")
public class DailyCommission {
    @Id
    private Long id;

    @Column("commission_date")
    private LocalDate commissionDate;

    @Column("commission_collected")
    private BigDecimal commissionCollected = BigDecimal.ZERO;

    @Column("game_count")
    private Integer gameCount = 0;

    @Column("total_prize_amount")
    private BigDecimal totalPrizeAmount = BigDecimal.ZERO;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
