package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.RoomStatus;
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

    private Integer maxBots;
    
    private RoomStatus status;
}
