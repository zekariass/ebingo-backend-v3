package com.ebingo.backend.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflineDepositRequestDto {
    @NotNull(message = "Phone Number is required")
    private Long telegramId;

    private Long agentId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String paymentMethodCode;

    private Long paymentMethodId;
    private String instructionsUrl;

    @NotNull(message = "Payment Provider Reference is required")
    private String paymentProviderRef;
    private Map<String, Object> metadata;
}
