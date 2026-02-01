package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.payment.enums.WithdrawalMode;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WithdrawRequestDto {
    private Long telegramId;
    private Long paymentMethodId;
    private String providerPaymentMethodName;
    private String phoneNumber;
    private BigDecimal amount;
    private String currency;
    private TransactionType txnType;
    private String bankName; // for bank transfers
    private String accountName;  // for bank transfers
    private String accountNumber;  // for bank transfers
    private WithdrawalMode withdrawalMode;

    private String password; // for authentication during bank withdrawal
    private Long agentId;
}

