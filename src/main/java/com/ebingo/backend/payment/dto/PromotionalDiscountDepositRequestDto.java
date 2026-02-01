package com.ebingo.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionalDiscountDepositRequestDto {
    private Long userTelegramId;
    private Long agentId;
    private BigDecimal amount;
}
