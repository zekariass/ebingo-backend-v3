package com.ebingo.backend.agent.dto.agent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Per-agent deposit bonus/lock rules.
 * Shape mirrors the `deposit.*` block in application.yml so the admin UI can
 * round-trip the same structure. Used for both read and upsert (PUT) bodies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDepositConfigDto {

    private Long agentId;

    @Valid
    @NotNull
    private AmountRule bonusAmount;

    @Valid
    @NotNull
    private AmountRule lockAmount;

    /**
     * A rate/fixed rule with an optional cap.
     * result = base * rate + fixed, capped at max.amount when max.isCapped is true.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmountRule {

        @NotNull
        @DecimalMin(value = "0.0")
        private BigDecimal rate;

        @NotNull
        @DecimalMin(value = "0.0")
        private BigDecimal fixed;

        @Valid
        @NotNull
        private Cap max;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cap {

        @NotNull
        private Boolean isCapped;

        @NotNull
        @DecimalMin(value = "0.0")
        private BigDecimal amount;
    }
}
