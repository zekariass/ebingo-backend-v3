package com.ebingo.backend.game.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@ToString
public class GameEndResponse {
    private Long gameId;
    private Long roomId;
    private Long playerId;
    private Long agentId;
    private String playerName;
    private String cardId;
    private String pattern;
    private BigDecimal prizeAmount;
    private LocalDateTime winAt;
    private boolean hasWinner;
    private Set<Integer> markedNumbers;
    private CardInfo card;
}
