package com.ebingo.backend.game.mappers;

import com.ebingo.backend.game.dto.GameEndResponse;

import java.util.HashMap;
import java.util.Map;

public final class GameEndResponseMapper {
    public static Map<String, Object> toMap(GameEndResponse res) {
        Map<String, Object> respMap = new HashMap<>();
        respMap.put("gameId", res.getGameId());
        respMap.put("roomId", res.getRoomId());
        respMap.put("agentId", res.getAgentId());
        respMap.put("playerId", res.getPlayerId());
        respMap.put("playerName", res.getPlayerName());
        respMap.put("cardId", res.getCardId());
        respMap.put("pattern", res.getPattern());
        respMap.put("prizeAmount", res.getPrizeAmount());
        respMap.put("winAt", res.getWinAt());
        respMap.put("hasWinner", res.isHasWinner());
        respMap.put("markedNumbers", res.getMarkedNumbers());

        if (res.getCard() != null) {
            respMap.put("card", res.getCard());
        }

        return respMap;
    }
}
