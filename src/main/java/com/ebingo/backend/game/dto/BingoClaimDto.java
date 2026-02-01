package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.GamePattern;
import lombok.*;

import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BingoClaimDto {
    private Long id;
    private Long gameId;
    private Long playerId;
    private Long agentId;
    private String card;
    private String markedNumbers;
    private GamePattern pattern;
    private Boolean isWinner;
    private Instant createdAt;
    private Instant updatedAt;
    private String error;
}
