package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.DepositTransferDto;
import com.ebingo.backend.payment.entity.DepositTransfer;

public final class DepositTransferMapper {
    public static DepositTransferDto toDto(DepositTransfer depositTransfer) {
        return DepositTransferDto.builder()
                .id(depositTransfer.getId())
                .agentId(depositTransfer.getAgentId())
                .senderId(depositTransfer.getSenderId())
                .receiverId(depositTransfer.getReceiverId())
                .amount(depositTransfer.getAmount())
                .status(depositTransfer.getStatus())
                .createdAt(depositTransfer.getCreatedAt())
                .updatedAt(depositTransfer.getUpdatedAt())
                .build();
    }


//    public static DepositTransfer toEntity(DepositTransferRequestDto depositTransferDto) {
//        DepositTransfer depositTransfer = new DepositTransfer();
//        depositTransfer.setReceiverId(depositTransferDto.getReceiverId());
//        depositTransfer.setAmount(depositTransferDto.getAmount());
//        return depositTransfer;
//    }
}
