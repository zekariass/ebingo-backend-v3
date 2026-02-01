package com.ebingo.backend.game.state;

import com.ebingo.backend.game.enums.GameStatus;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the state of a single Bingo game in a room.
 */
@Data
@ToString
public class GameState {

    private Long gameId;
    private Long roomId;
    private Long agentId;
    // Players in the game
    private final Set<String> joinedPlayers = ConcurrentHashMap.newKeySet();

    // Numbers that have been drawn in the game (order matters)
    private final Set<Integer> drawnNumbers = new LinkedHashSet<>();

    // Read from Player State
    private Set<String> userSelectedCardsIds = new LinkedHashSet<>();
    private Set<String> allSelectedCardsIds = new LinkedHashSet<>();

    // Game status flags
    private volatile boolean started = false;       // only one writer -> fine as volatile
    private volatile boolean ended = false;
    private volatile GameStatus status = GameStatus.READY;
    private Instant statusUpdatedAt;

    private Boolean stopNumberDrawing = false;
    private Boolean claimRequested = false;

    //    private Instant countdownStartTime;
    private Instant countdownEndTime;
    private Long countdownDurationSeconds = 0L;
    private Long backendEpochMillis = 0L;
    private Double commissionRate = 0.0;
    private Double entryFee = 0.0;
    private Integer capacity = 0;

    public void setJoinedPlayers(Set<String> userIds) {
        joinedPlayers.clear();
        joinedPlayers.addAll(userIds);
    }

    public void setDrawnNumber(LinkedHashSet<Integer> nums) {
        drawnNumbers.clear();
        drawnNumbers.addAll(nums);
    }

}
