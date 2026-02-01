package com.ebingo.backend.game.mappers;

import com.ebingo.backend.game.dto.BingoClaimCreateDto;
import com.ebingo.backend.game.dto.BingoClaimDto;
import com.ebingo.backend.game.entity.BingoClaim;

public final class BingoClaimMapper {

    public static BingoClaimDto toDto(BingoClaim claim) {
        if (claim == null) return null;
        return BingoClaimDto.builder()
                .id(claim.getId())
                .gameId(claim.getGameId())
                .playerId(claim.getPlayerId())
                .agentId(claim.getAgentId())
                .card(claim.getCard())
                .markedNumbers(claim.getMarkedNumbers())
                .pattern(claim.getPattern())
                .isWinner(claim.getIsWinner())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .error(claim.getError())
                .build();
    }

    public static BingoClaim toEntity(BingoClaimDto dto) {
        if (dto == null) return null;
        BingoClaim claim = new BingoClaim();
        claim.setId(dto.getId());
        claim.setGameId(dto.getGameId());
        claim.setPlayerId(dto.getPlayerId());
        claim.setAgentId(dto.getAgentId());
        claim.setCard(dto.getCard());
        claim.setMarkedNumbers(dto.getMarkedNumbers());
        claim.setPattern(dto.getPattern());
        claim.setIsWinner(dto.getIsWinner());
        claim.setCreatedAt(dto.getCreatedAt());
        claim.setUpdatedAt(dto.getUpdatedAt());
        claim.setError(dto.getError());
        return claim;
    }


    public static BingoClaim toEntity(BingoClaimCreateDto dto) {
        if (dto == null) return null;
        BingoClaim claim = new BingoClaim();
        claim.setGameId(dto.getGameId());
        claim.setAgentId(dto.getAgentId());
        claim.setCard(dto.getCard());
        claim.setMarkedNumbers(dto.getMarkedNumbers());
        claim.setPattern(dto.getPattern());
        claim.setIsWinner(dto.getIsWinner());
        claim.setError(dto.getError());
        return claim;
    }
}
