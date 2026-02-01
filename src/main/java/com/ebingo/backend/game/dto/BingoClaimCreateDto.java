package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.GamePattern;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BingoClaimCreateDto {

    @NotNull(message = "Game id is required")
    private Long gameId;

    @NotNull(message = "Player id is required")
    private Long playerId;

    @NotNull(message = "Agent id is required")
    private Long agentId;

    @NotNull(message = "Player name is required")
    private String playerName;

    @NotNull(message = "Card is required")
    private String card;

    @NotNull(message = "Marked numbers are required")
    private String markedNumbers;

    @NotNull(message = "Pattern is required")
    private GamePattern pattern;

    private String error;

    private Boolean isWinner = false;
}
