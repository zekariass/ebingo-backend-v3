package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RoomUpdateDto {

    @NotNull(message = "Room ID must not be null")
    private Long id;

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

    private Integer minDraws;

    private Integer maxDraws;

    private Boolean fakeWinEnabled;


}
