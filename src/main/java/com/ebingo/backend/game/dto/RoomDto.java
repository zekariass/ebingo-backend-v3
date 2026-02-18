package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.RoomStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RoomDto {
    private Long id;

    private Long agentId;

    private String name;

    private Integer capacity;

    private Integer minPlayers;

    private BigDecimal entryFee;

    private GamePattern pattern;

    private RoomStatus status;

    private BigDecimal commissionRate;

    private Boolean botAllowed;

    private Integer minBots;

    private Integer maxBots;

    private Integer maxCards;

    private Long createdBy;

    private Instant createdAt;

    private Instant updatedAt;
}
