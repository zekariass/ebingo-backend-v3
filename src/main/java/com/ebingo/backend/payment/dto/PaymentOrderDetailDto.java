package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.user.dto.UserProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderDetailDto {
    private Long id;
    private Long userId;
    private String txnRef;
    private Long agentId;
    private String providerOrderRef;
    private BigDecimal amount;
    private String currency;
    private PaymentOrderStatus status;
    private String reason;
    private Long paymentMethodId;
    private String instructionsUrl;
    private TransactionType txnType;
    private String metaData;
    private String nonce;
    private String phoneNumber;
    private UserProfileDto userProfile;
    private PaymentMethodMinimalDto paymentMethod;
    private WalletDto wallet;
    private Long approvedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
