package com.ebingo.backend.game.mappers;

import com.ebingo.backend.game.entity.Game;
import com.ebingo.backend.game.state.GameState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class GameMapper {
    public static Game toEntity(GameState gs, ObjectMapper objectMapper) {
        if (gs == null) {
            return null;
        }

        Game game = new Game();
        game.setRoomId(gs.getRoomId());

        // Defensive copies
        List<String> joinedPlayers = gs.getJoinedPlayers() != null ? new ArrayList<>(gs.getJoinedPlayers()) : List.of();
        List<Integer> drawnNumbers = gs.getDrawnNumbers() != null ? new ArrayList<>(gs.getDrawnNumbers()) : List.of();
//        List<String> allCardIds = gs.getAllCardIds() != null ? new ArrayList<>(gs.getAllCardIds()) : List.of();

        try {
//            ObjectMapper objectMapper = new ObjectMapper();
            game.setJoinedPlayersJson(objectMapper.writeValueAsString(joinedPlayers));
            game.setDrawnNumberJson(objectMapper.writeValueAsString(drawnNumbers));
//            game.setAllCardIdsJson(objectMapper.writeValueAsString(allCardIds));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing game lists to JSON", e);
        }

        game.setAgentId(gs.getAgentId());
        game.setStarted(gs.isStarted());
        game.setEnded(gs.isEnded());
        game.setStatus(gs.getStatus());
        game.setPlayersCount(joinedPlayers.size());
        game.setEntriesCount(gs.getAllSelectedCardsIds() != null ? gs.getAllSelectedCardsIds().size() : 0);
        game.setPrizeAmount(BigDecimal.ZERO);
        game.setCommissionAmount(BigDecimal.ZERO);
        game.setEntryFee(BigDecimal.valueOf(gs.getEntryFee()).setScale(2, RoundingMode.HALF_UP));
        game.setCapacity(gs.getCapacity());

        return game;
    }


    public static Game toEntity(GameState gs, Game game, ObjectMapper objectMapper) {
        if (gs == null) {
            return null;
        }
        if (game == null) {
            game = new Game();
        }

        // Safely copy lists
        List<String> joinedPlayers = gs.getJoinedPlayers() != null ? new ArrayList<>(gs.getJoinedPlayers()) : List.of();
        List<Integer> drawnNumbers = gs.getDrawnNumbers() != null ? new ArrayList<>(gs.getDrawnNumbers()) : List.of();
//        List<String> allCardIds = gs.getAllCardIds() != null ? new ArrayList<>(gs.getAllCardIds()) : List.of();

        try {
            game.setJoinedPlayersJson(objectMapper.writeValueAsString(joinedPlayers));
            game.setDrawnNumberJson(objectMapper.writeValueAsString(drawnNumbers));
//            game.setAllCardIdsJson(objectMapper.writeValueAsString(allCardIds));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize game lists to JSON", e);
        }

        if (gs.isEnded()) {
            game.setEndedAt(LocalDateTime.now());
        }

        game.setStarted(gs.isStarted());
        game.setEnded(gs.isEnded());
        game.setStatus(gs.getStatus());
        game.setPlayersCount(!joinedPlayers.isEmpty() ? joinedPlayers.size() : 1);
        game.setEntriesCount(gs.getAllSelectedCardsIds() != null ? gs.getAllSelectedCardsIds().size() : 0);
        game.setCapacity(gs.getCapacity());

        // Financial fields
        BigDecimal entryFee = BigDecimal.valueOf(gs.getEntryFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commissionRate = BigDecimal.valueOf(gs.getCommissionRate());
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Invalid commission rate: " + commissionRate);
        }

        BigDecimal entriesCount = BigDecimal.valueOf(game.getEntriesCount());
        BigDecimal playerJackpotRate = BigDecimal.ONE.subtract(commissionRate);

        game.setEntryFee(entryFee);
        game.setPrizeAmount(entriesCount.multiply(entryFee).multiply(playerJackpotRate).setScale(2, RoundingMode.HALF_UP));
        game.setCommissionAmount(entriesCount.multiply(entryFee).multiply(commissionRate).setScale(2, RoundingMode.HALF_UP));

        return game;
    }

}
