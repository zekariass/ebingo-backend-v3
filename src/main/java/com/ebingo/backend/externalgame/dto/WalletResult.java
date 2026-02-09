package com.ebingo.backend.externalgame.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResult {
    private boolean success;
    private String balance;
    private BigDecimal balanceAmount;
    private String errorCode;
    private String errorMessage;
    private Long walletEntryId;
}
