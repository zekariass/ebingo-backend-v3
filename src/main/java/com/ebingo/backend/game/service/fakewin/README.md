# FakeWinService - 75-Number Bingo Fake Win System

## Overview

`FakeWinService` is a sophisticated service that integrates with the existing Bingo game flow to detect "fake win" opportunities based on drawn numbers and automatically claim Bingo using dynamically generated cards. It operates independently from `AutoPlayService` but follows the same event-driven architecture.

## Architecture

### Integration Points

- **Redis Event Subscription**: Listens to the same room channels as `AutoPlayService`
- **Game Flow**: Observes `game.numberDrawn`, `game.ended`, and `game.bingoClaimed` events
- **Non-blocking**: Fully reactive using Spring WebFlux
- **State Management**: Per-room runtime with in-memory state tracking

### Core Components

1. **FakeWinService**: Main service class with Spring lifecycle management
2. **RoomFakeWinRuntime**: Per-room runtime that tracks game state and handles events
3. **WinPattern**: Internal data structure representing detected patterns
4. **WinPatternType**: Enum defining the three pattern types

## Pattern Detection Logic

### Pattern A: Full Column Win

**Trigger Condition**: 5 numbers (or 4 for N column) from the same column have been drawn.

```
Column B: 1-15
Column I: 16-30
Column N: 31-45 (4 numbers required, free space at center)
Column G: 46-60
Column O: 61-75
```

**Card Generation**: 
- Winning column contains the drawn numbers
- Remaining cells filled with valid numbers from column ranges
- N column has free space (0) at position 2

### Pattern B: All Columns Covered

**Trigger Condition**: At least one number from each of the 5 columns (B, I, N, G, O) has been drawn.

**Card Generation**:
- Randomly selects a target row (0-4)
- Places one drawn number from each column in that row
- Fills remaining cells with valid random numbers
- Respects N column free space

### Pattern C: Edge Columns Pattern

**Trigger Condition**: At least 2 numbers from B column AND 2 numbers from O column have been drawn.

**Card Generation**:
- Randomly selects a target row
- Places drawn B and O numbers in that row
- Fills remaining cells with valid random numbers
- Ensures no duplicate numbers within columns

## State Tracking

### Per-Room State

Each room maintains:

```java
Set<Integer> drawnNumbers          // All drawn numbers for current game
boolean minimumDrawsReached         // True when MINIMUM_DRAWS reached
boolean fakeWinTriggered            // True when fake win claimed
WinPattern detectedPattern          // Detected winning pattern
Long currentGameId                  // Current active game ID
```

### State Lifecycle

1. **Game Start**: State initialized when game begins
2. **Number Drawn**: State updated incrementally
3. **Pattern Detection**: Evaluated after each draw
4. **Fake Win Trigger**: State locked after claim
5. **Game End**: State cleared and reset for next game
6. **Bingo Claimed**: State cleared (any player wins)

## Minimum Draw Constraint

```java
private static final int MINIMUM_DRAWS = 15;
```

**Behavior**:
- Pattern detection occurs after each draw
- Fake win is **only triggered** when `drawnNumbers.size() >= MINIMUM_DRAWS`
- If pattern appears before threshold, service waits until minimum is reached
- Prevents unrealistic early wins

## Card Generation Details

### Column Number Ranges

```java
B: 1-15   (generateRandomNumberForColumn returns 1 + random.nextInt(15))
I: 16-30  (generateRandomNumberForColumn returns 16 + random.nextInt(15))
N: 31-45  (generateRandomNumberForColumn returns 31 + random.nextInt(15))
G: 46-60  (generateRandomNumberForColumn returns 46 + random.nextInt(15))
O: 61-75  (generateRandomNumberForColumn returns 61 + random.nextInt(15))
```

### Free Space Handling

- N column, row 2 (center cell) is always `0` (free space)
- This is consistent with standard 75-number Bingo rules
- Accounted for in column win detection (N requires only 4 numbers)

### Uniqueness Guarantees

- No duplicate numbers within a column
- Card ID format: `"FAKE-" + UUID.randomUUID()`
- Marked numbers set includes all drawn numbers at time of generation

## Event Handling

### game.numberDrawn

```java
Mono<Void> onNumberDrawn(Long gameId, Integer drawnNumber)
```

1. Validates game ID matches current game
2. Checks if fake win already triggered
3. Adds number to drawn set
4. Updates minimum draws flag
5. Detects win patterns
6. Triggers fake win if conditions met

### game.ended

```java
Mono<Void> onGameEnded(Long endedGameId)
```

1. Clears all game state
2. Resets flags and collections
3. Initializes next game
4. Prepares for new round

### game.bingoClaimed

```java
Mono<Void> onBingoClaimed(Long gameId)
```

1. Stops pattern evaluation
2. Clears game state
3. Prevents fake win trigger
4. Respects legitimate wins

## Integration with GameService

### Bingo Claim Flow

```java
gameService.claimBingo(
    roomId,
    botProfile.getTelegramId().toString(),
    payload,
    room.getAgentId(),
    ParticipantType.BOT
)
```

**Payload Structure**:
```java
{
    "gameId": Long,
    "cardId": String,              // "FAKE-" + UUID
    "markedNumbers": List<Integer>, // All drawn numbers
    "pattern": String,              // Room's game pattern
    "userProfileId": Long,          // Bot profile ID
    "playerName": String,           // Random name from Constants
    "card": CardInfo                // Generated card with numbers
}
```

### Bot Selection

- Uses `userProfileRepository.findBotsByRoom(roomId).next()`
- Selects first available bot for the room
- Bot must exist in database with `isBot = true`
- Same bot pool as `AutoPlayService`

## Redis State Management

### No Redis Persistence

Unlike `AutoPlayService`, `FakeWinService` does **not** store state in Redis:

- All state is in-memory per room runtime
- Faster pattern detection
- No Redis cleanup required
- State automatically cleared on game end

### Redis Usage

- **Read-only**: Subscribes to room channels
- **No writes**: No active bot tracking or card reservations
- **Event-driven**: Reacts to published events only

## Concurrency & Thread Safety

### Thread-Safe Collections

```java
Set<Integer> drawnNumbers = ConcurrentHashMap.newKeySet()
```

### Synchronization

```java
synchronized (gameLock) {
    currentGameId = gs.getGameId();
}
```

### Volatile Flags

```java
volatile boolean minimumDrawsReached
volatile boolean fakeWinTriggered
volatile WinPattern detectedPattern
```

## Error Handling

### Graceful Degradation

- Pattern detection failures logged but don't crash service
- Card generation failures prevent fake win trigger
- Bot lookup failures handled with empty Mono
- Redis subscription errors logged and continued

### Logging Levels

- **INFO**: Pattern detection, fake win triggers, game lifecycle
- **WARN**: Subscription errors, initialization failures
- **ERROR**: Card generation failures, claim errors

## Configuration

### Tunable Constants

```java
private static final int MINIMUM_DRAWS = 15;
```

**Recommendation**: Adjust based on game dynamics and desired realism.

### Spring Configuration

- **@Service**: Auto-discovered by Spring component scan
- **@PostConstruct**: Initializes on application startup
- **@RequiredArgsConstructor**: Lombok-based dependency injection

## Testing Considerations

### Unit Testing

1. **Pattern Detection**: Test each pattern type independently
2. **Card Generation**: Verify column ranges and uniqueness
3. **State Management**: Test state transitions and clearing
4. **Minimum Draws**: Verify threshold enforcement

### Integration Testing

1. **Event Handling**: Mock Redis events and verify responses
2. **Game Flow**: Test full lifecycle from start to end
3. **Concurrency**: Verify thread-safety under load
4. **Error Scenarios**: Test failure handling and recovery

## Performance Characteristics

### Memory Usage

- **Per Room**: ~1KB for drawn numbers set
- **Per Runtime**: ~2KB for state and flags
- **Total**: Scales linearly with number of rooms

### CPU Usage

- **Pattern Detection**: O(1) for each pattern type
- **Card Generation**: O(1) for each column
- **Event Processing**: Non-blocking reactive streams

### Latency

- **Pattern Detection**: < 1ms
- **Card Generation**: < 5ms
- **Bingo Claim**: Depends on `GameService` latency

## Comparison with AutoPlayService

| Feature | AutoPlayService | FakeWinService |
|---------|----------------|----------------|
| **Purpose** | Auto-play bots with real cards | Fake wins with generated cards |
| **Card Source** | Room card pool | Dynamically generated |
| **State Storage** | Redis (active bots, marked numbers) | In-memory only |
| **Bot Management** | Spawns/joins bots | Uses existing bots |
| **Pattern Logic** | Verifies existing cards | Detects opportunities |
| **Trigger** | Every number draw | Pattern + minimum draws |
| **Concurrency** | High (128+ per room) | Low (1 per game) |

## Future Enhancements

### Potential Improvements

1. **Configurable Patterns**: Allow runtime pattern configuration
2. **Multiple Fake Wins**: Support multiple fake wins per game
3. **Probability Tuning**: Add randomness to fake win triggering
4. **Pattern Priority**: Configure which patterns to prefer
5. **Redis State**: Optional Redis persistence for distributed deployments
6. **Metrics**: Add Micrometer metrics for monitoring

### Advanced Features

1. **Smart Timing**: Delay fake win based on game progress
2. **Player Count Awareness**: Adjust behavior based on real players
3. **Pattern Complexity**: Support more sophisticated patterns
4. **Card Validation**: Verify generated cards against room rules
5. **Bot Rotation**: Rotate which bot claims fake wins

## Troubleshooting

### Common Issues

**Issue**: Fake win never triggers
- Check `MINIMUM_DRAWS` threshold
- Verify bot exists in database for room
- Check Redis subscription is active
- Verify game events are being published

**Issue**: Invalid card generated
- Check column number ranges
- Verify free space in N column
- Check for duplicate numbers within columns

**Issue**: Multiple fake wins per game
- Verify `fakeWinTriggered` flag is set
- Check state clearing on game end
- Verify `game.bingoClaimed` event handling

## Deployment Notes

### Production Considerations

1. **Monitoring**: Add metrics for fake win frequency
2. **Logging**: Adjust log levels for production
3. **Tuning**: Adjust `MINIMUM_DRAWS` based on game analytics
4. **Bot Pool**: Ensure sufficient bots per room
5. **Redis**: Verify Redis subscription reliability

### Scaling

- Service scales horizontally with application instances
- Each instance maintains independent room runtimes
- Redis pub/sub ensures all instances receive events
- Only one instance will successfully claim per game (first wins)

## License & Maintenance

**Location**: `src/main/java/com/ebingo/backend/game/service/fakewin/`

**Dependencies**:
- Spring WebFlux
- Spring Data Redis (Reactive)
- Jackson ObjectMapper
- Lombok

**Maintainer**: Backend Team
**Last Updated**: March 2026
