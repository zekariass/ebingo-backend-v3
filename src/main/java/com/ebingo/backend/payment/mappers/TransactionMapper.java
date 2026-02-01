package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.InitiateDepositRequest;
import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.entity.Transaction;
import com.ebingo.backend.payment.enums.TransactionStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.user.dto.UserProfileDto;

public final class TransactionMapper {
    public static TransactionDto toDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .playerId(transaction.getPlayerId())
                .agentId(transaction.getAgentId())
//                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .paymentMethodId(transaction.getPaymentMethodId())
                .txnType(transaction.getTxnType())
                .txnAmount(transaction.getTxnAmount())
                .txnRef(transaction.getTxnRef())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    public static Transaction toEntity(InitiateDepositRequest depositRequest, UserProfileDto up) {
        Transaction transaction = new Transaction();
        transaction.setPlayerId(up.getId());
        transaction.setAgentId(depositRequest.getAgentId());
        transaction.setPaymentMethodId(depositRequest.getPaymentMethodId());
        transaction.setTxnType(TransactionType.DEPOSIT);
        transaction.setTxnAmount(depositRequest.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);

        return transaction;
    }
}
