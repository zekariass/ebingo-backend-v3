package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RoomCreateDto {
    @NotNull(message = "Agent id must not be null")
    private Long agentId;

    @NotBlank(message = "Room name must not be blank")
    private String name;

    @NotNull(message = "Capacity must not be null")
    private Integer capacity;

    @NotNull(message = "Minimum players must not be null")
    private Integer minPlayers;

    @NotNull(message = "Entry fee must not be null")
    private BigDecimal entryFee;

    @NotNull(message = "Room pattern must not be null")
    private GamePattern pattern;

    private BigDecimal commissionRate;

    private Boolean botAllowed;

    private Integer minBots;

    @Max(value = 100, message = "Max bots must be less than or equal to 100")
    @Min(value = 0, message = "Max bots must be greater than or equal to 0")
    private Integer maxBots;

    @Max(value = 2, message = "Max cards must be less than or equal to 2")
    @Min(value = 1, message = "Max cards must be greater than or equal to 1")
    private Integer maxCards;

    private RoomStatus status;

    private Integer minDraws;

    private Integer maxDraws;

    private Boolean fakeWinEnabled;
}
