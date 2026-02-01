package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.GameTransactionDto;
import com.ebingo.backend.payment.entity.GameTransaction;

public final class GameTransactionMapper {

    public static GameTransactionDto toDto(GameTransaction gameTransaction) {
        if (gameTransaction == null) return null;
        return GameTransactionDto.builder()
                .id(gameTransaction.getId())
                .agentId(gameTransaction.getAgentId())
                .gameId(gameTransaction.getGameId())
                .playerId(gameTransaction.getPlayerId())
                .txnAmount(gameTransaction.getTxnAmount())
                .singleGameFee(gameTransaction.getSingleGameFee())
                .commissionAmount(gameTransaction.getCommissionAmount())
                .txnType(gameTransaction.getTxnType())
                .txnStatus(gameTransaction.getTxnStatus())
                .createdAt(gameTransaction.getCreatedAt())
                .updatedAt(gameTransaction.getUpdatedAt())
                .build();
    }


    public static GameTransaction toEntity(GameTransactionDto gameTransactionDto) {
        if (gameTransactionDto == null) return null;
        GameTransaction gameTransaction = new GameTransaction();
        gameTransaction.setId(gameTransactionDto.getId());
        gameTransaction.setAgentId(gameTransactionDto.getAgentId());
        gameTransaction.setGameId(gameTransactionDto.getGameId());
        gameTransaction.setPlayerId(gameTransactionDto.getPlayerId());
        gameTransaction.setTxnAmount(gameTransactionDto.getTxnAmount());
        gameTransaction.setSingleGameFee(gameTransactionDto.getSingleGameFee());
        gameTransaction.setCommissionAmount(gameTransactionDto.getCommissionAmount());
        gameTransaction.setTxnType(gameTransactionDto.getTxnType());
        gameTransaction.setTxnStatus(gameTransactionDto.getTxnStatus());
        gameTransaction.setCreatedAt(gameTransactionDto.getCreatedAt());
        gameTransaction.setUpdatedAt(gameTransactionDto.getUpdatedAt());
        return gameTransaction;
    }
}
