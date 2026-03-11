package com.ebingo.backend.game.service.fakewin;

import com.ebingo.backend.common.Constants;
import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.dto.RoomInternalDto;
import com.ebingo.backend.game.enums.BingoColumn;
import com.ebingo.backend.game.enums.GamePattern;
import com.ebingo.backend.game.enums.ParticipantType;
import com.ebingo.backend.game.service.GameService;
import com.ebingo.backend.game.service.RoomService;
import com.ebingo.backend.system.redis.RedisKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.ReactiveSubscription.Message;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FakeWinService {

    private final RoomService roomService;
    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final int DEFAULT_MIN_DRAWS = 6;
    private static final int DEFAULT_MAX_DRAWS = 10;
    private static final SecureRandom random = new SecureRandom();

    private final Map<Long, RoomFakeWinRuntime> runtimes = new ConcurrentHashMap<>();

    private String fakeWinDrawnNumbersKey(Long roomId, Long gameId) {
        return "fakewin:room:" + roomId + ":game:" + gameId + ":drawnNumbers";
    }

    private String fakeWinTriggeredKey(Long roomId, Long gameId) {
        return "fakewin:room:" + roomId + ":game:" + gameId + ":triggered";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("FakeWinService initializing after application ready...");

        Mono.delay(Duration.ofSeconds(2))
                .then(roomService.getAllRoomsWIthCardPoolForAutoService()
                        .filter(RoomInternalDto::getBotAllowed)
                        .filter(room -> Boolean.TRUE.equals(room.getFakeWinEnabled()))
                        .doOnNext(this::ensureRoomRuntime)
                        .then())
                .subscribe(
                        v -> log.info("FakeWinService bootstrap complete"),
                        e -> log.error("FakeWinService bootstrap failure", e)
                );
    }

    private void ensureRoomRuntime(RoomInternalDto room) {
        runtimes.compute(room.getId(), (roomId, existing) -> {
            if (existing != null) {
                existing.refreshRoom(room);
                return existing;
            }
            RoomFakeWinRuntime rt = new RoomFakeWinRuntime(room);
            rt.start();
            return rt;
        });
    }

    private final class RoomFakeWinRuntime {
        private volatile RoomInternalDto room;
        private final Long roomId;

        @SuppressWarnings("unused")
        private volatile Disposable roomSubscription;

        private volatile Long currentGameId;
        private volatile Integer targetDrawCount;
        private final Object gameLock = new Object();

        RoomFakeWinRuntime(RoomInternalDto initial) {
            this.room = initial;
            this.roomId = initial.getId();
            this.targetDrawCount = calculateTargetDrawCount();
        }

        void refreshRoom(RoomInternalDto updated) {
            this.room = updated;
        }

        int calculateTargetDrawCount() {
            Integer minDraws = room.getMinDraws() != null ? room.getMinDraws() : DEFAULT_MIN_DRAWS;
            Integer maxDraws = room.getMaxDraws() != null ? room.getMaxDraws() : DEFAULT_MAX_DRAWS;
            
            if (minDraws.equals(maxDraws)) {
                return minDraws;
            }
            
            return minDraws + random.nextInt(maxDraws - minDraws + 1);
        }

        void start() {
            String channel = RedisKeys.roomChannel(roomId);

            roomSubscription = redisTemplate.listenToChannel(channel)
                    .map(Message::getMessage)
                    .map(String::valueOf)
                    .map(FakeWinService.this::parsePayload)
                    .flatMap(this::handleRoomEvent)
                    .onErrorContinue((err, o) -> log.warn("Room {} FakeWin subscription error: {}", roomId, err.getMessage()))
                    .subscribe();

            log.info("FakeWinService started listening to room {} channel", roomId);
        }

        Mono<Void> handleRoomEvent(Map<String, Object> evt) {
            Object typeObj = evt.get("type");
            if (!(typeObj instanceof String type)) return Mono.empty();

            Object payloadObj = evt.get("payload");
            if (!(payloadObj instanceof Map<?, ?> payload)) return Mono.empty();

            Long payloadRoomId = convertToLong(payload.get("roomId"));
            if (payloadRoomId != null && !Objects.equals(payloadRoomId, roomId)) return Mono.empty();

            if ("game.ended".equals(type)) {
                Long endedGameId = convertToLong(payload.get("gameId"));
                if (endedGameId == null) return Mono.empty();
                return onGameEnded(endedGameId);
            }

            if ("game.numberDrawn".equals(type)) {
                Long gameId = convertToLong(payload.get("gameId"));
                Integer number = convertToInteger(payload.get("number"));
                if (gameId == null || number == null) return Mono.empty();
                return onNumberDrawn(gameId, number);
            }

            if ("game.bingoClaimed".equals(type)) {
                Long gameId = convertToLong(payload.get("gameId"));
                if (gameId == null) return Mono.empty();
                return onBingoClaimed(gameId);
            }

            return Mono.empty();
        }

        Mono<Void> onGameEnded(Long endedGameId) {
            log.info("Room {}: FakeWin game {} ended — clearing Redis state", roomId, endedGameId);

            synchronized (gameLock) {
                if (Objects.equals(currentGameId, endedGameId)) {
                    currentGameId = null;
                    targetDrawCount = calculateTargetDrawCount();
                }
            }

            return clearRedisState(endedGameId);
        }

        Mono<Void> onBingoClaimed(Long gameId) {
            boolean shouldClear;
            synchronized (gameLock) {
                shouldClear = currentGameId != null && Objects.equals(currentGameId, gameId);
                if (shouldClear) {
                    // Reset state for next game
                    currentGameId = null;
                    targetDrawCount = calculateTargetDrawCount();
                }
            }

            if (!shouldClear) {
                return Mono.empty();
            }

            log.info("Room {} game {}: Bingo claimed, stopping FakeWin evaluation and clearing Redis state", roomId, gameId);
            return clearRedisState(gameId);
        }

        Mono<Void> onNumberDrawn(Long gameId, Integer drawnNumber) {
            if (!Boolean.TRUE.equals(room.getFakeWinEnabled())) {
                return Mono.empty();
            }

            synchronized (gameLock) {
                if (currentGameId == null) {
                    currentGameId = gameId;
                    targetDrawCount = calculateTargetDrawCount();
                    log.info("FakeWinService tracking new game {} for room {} with target draw count {}", gameId, roomId, targetDrawCount);
                } else if (!Objects.equals(currentGameId, gameId)) {
                    log.debug("Room {}: Ignoring draw for game {} (tracking game {})", roomId, gameId, currentGameId);
                    return Mono.empty();
                }
            }

            final int currentTargetDraws = targetDrawCount;

            String triggeredKey = fakeWinTriggeredKey(roomId, gameId);
            String drawnKey = fakeWinDrawnNumbersKey(roomId, gameId);

            return redisTemplate.opsForValue().get(triggeredKey)
                    .defaultIfEmpty("0")
                    .flatMap(triggered -> {
                        if ("1".equals(triggered)) {
                            log.debug("Room {} game {}: FakeWin already triggered, skipping draw {}", roomId, gameId, drawnNumber);
                            return Mono.<Void>empty();
                        }

                        // Add drawn number and check count
                        return redisTemplate.opsForSet().add(drawnKey, drawnNumber.toString())
                                .flatMap(added -> redisTemplate.opsForSet().size(drawnKey))
                                .flatMap(drawCount -> {
                                    log.debug("Room {} game {}: Draw #{} - number {}", roomId, gameId, drawCount, drawnNumber);

                                    if (drawCount < currentTargetDraws) {
                                        return Mono.<Void>empty();
                                    }

                                    // Target draws reached, fetch all drawn numbers and attempt fake win
                                    return redisTemplate.opsForSet().members(drawnKey)
                                            .collectList()
                                            .flatMap(drawnList -> {
                                                Set<Integer> drawnNumbers = drawnList.stream()
                                                        .map(Integer::parseInt)
                                                        .collect(java.util.stream.Collectors.toSet());

                                                log.info("Room {} game {}: Target draws {} reached ({} numbers), attempting fake win",
                                                        roomId, gameId, currentTargetDraws, drawnNumbers.size());

                                                return triggerFakeWin(gameId, drawnNumbers);
                                            });
                                });
                    });
        }

        Map<BingoColumn, List<Integer>> categorizeDrawnNumbers(Set<Integer> drawnNumbers) {
            Map<BingoColumn, List<Integer>> result = new EnumMap<>(BingoColumn.class);
            for (BingoColumn col : BingoColumn.values()) {
                result.put(col, new ArrayList<>());
            }

            for (Integer num : drawnNumbers) {
                BingoColumn col = getColumnForNumber(num);
                if (col != null) {
                    result.get(col).add(num);
                }
            }

            return result;
        }

        BingoColumn getColumnForNumber(int number) {
            if (number >= 1 && number <= 15) return BingoColumn.B;
            if (number >= 16 && number <= 30) return BingoColumn.I;
            if (number >= 31 && number <= 45) return BingoColumn.N;
            if (number >= 46 && number <= 60) return BingoColumn.G;
            if (number >= 61 && number <= 75) return BingoColumn.O;
            return null;
        }

        Mono<Void> triggerFakeWin(Long gameId, Set<Integer> drawnNumbers) {
            String triggeredKey = fakeWinTriggeredKey(roomId, gameId);

            return redisTemplate.opsForValue().setIfAbsent(triggeredKey, "1", Duration.ofHours(1))
                    .flatMap(wasSet -> {
                        if (Boolean.TRUE.equals(wasSet)) {
                            log.info("Room {} game {}: FakeWin lock acquired, proceeding with claim", roomId, gameId);

                            String fakeUserId = getFakeUserId();

                            CardInfo generatedCard = generateWinningCard(drawnNumbers);
                            if (generatedCard == null) {
                                log.warn("Room {} game {}: Could not generate winning card, releasing lock for retry on next draw", roomId, gameId);
                                return redisTemplate.delete(triggeredKey).then();
                            }

                            Map<String, Object> payload = buildBingoPayload(gameId, generatedCard, fakeUserId, room.getPattern());

                            log.info("Room {} game {}: FakeWin claiming bingo with user {} pattern {} card {} marked {}",
                                    roomId, gameId, fakeUserId, room.getPattern(), generatedCard.getNumbers(), generatedCard.getMarked());

                            // Add fake user to game players list and mark numbers before claiming
                            String playersKey = RedisKeys.gamePlayersKey(gameId);
                            String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, fakeUserId, generatedCard.getCardId());

                            return redisTemplate.opsForSet().add(playersKey, fakeUserId)
                                    .then(Mono.defer(() -> {
                                        String[] markedArray = generatedCard.getMarked().stream()
                                                .map(String::valueOf)
                                                .toArray(String[]::new);
                                        if (markedArray.length == 0) {
                                            return Mono.just(0L);
                                        }
                                        return redisTemplate.opsForSet().add(markedKey, markedArray);
                                    }))
                                    .then(Mono.defer(() ->
                                            gameService.claimBingo(roomId, fakeUserId, payload, room.getAgentId(), ParticipantType.BOT)
                                    ))
                                    .doOnSuccess(v -> log.info("Room {} game {}: FakeWin bingo claimed successfully", roomId, gameId))
                                    .doOnError(e -> log.error("Room {} game {}: FakeWin bingo claim failed: {}", roomId, gameId, e.getMessage()))
                                    .onErrorResume(e -> {
                                        log.error("Room {} game {}: FakeWin trigger error: {}", roomId, gameId, e.getMessage());
                                        return Mono.empty();
                                    })
                                    .then();
                        } else {
                            log.debug("Room {} game {}: FakeWin already triggered by another thread, skipping", roomId, gameId);
                            return Mono.empty();
                        }
                    });
        }

        String getFakeTelegramId() {
            if (room.getFakeWinnerTelegramId() != null) {
                return String.valueOf(room.getFakeWinnerTelegramId());
            }
            return "1000017052";
        }

        String getFakeUserId() {
            if (room.getFakeWinnerId() != null) {
                return String.valueOf(room.getFakeWinnerId());
            }
            return getFakeTelegramId();
        }

        CardInfo generateWinningCard(Set<Integer> drawnNumbers) {
            Map<BingoColumn, List<Integer>> drawnByColumn = categorizeDrawnNumbers(drawnNumbers);

            // All drawn numbers are placed on the card and marked
            // Winning strategy is randomly chosen: ROW or DIAGONAL
            boolean hasB = !drawnByColumn.get(BingoColumn.B).isEmpty();
            boolean hasI = !drawnByColumn.get(BingoColumn.I).isEmpty();
            boolean hasN = !drawnByColumn.get(BingoColumn.N).isEmpty();
            boolean hasG = !drawnByColumn.get(BingoColumn.G).isEmpty();
            boolean hasO = !drawnByColumn.get(BingoColumn.O).isEmpty();

            boolean canWinMiddleRow = hasB && hasI && hasG && hasO;
            boolean canWinOtherRow = canWinMiddleRow && hasN;
            // Diagonal needs: B, I, G, O (N center is free space)
            boolean canWinDiagonal = hasB && hasI && hasG && hasO;

            if (!canWinMiddleRow && !canWinDiagonal) {
                log.warn("Room {}: Cannot build winning card - missing columns B={} I={} N={} G={} O={}",
                        roomId, hasB, hasI, hasN, hasG, hasO);
                return null;
            }

            // Randomly pick strategy: ROW or DIAGONAL
            // ~40% chance diagonal, ~60% chance row
            boolean useDiagonal = canWinDiagonal && random.nextInt(5) < 2;

            String winType;
            // winPositions[colIndex] = row index for the winning cell in that column
            int[] winPositions = new int[5]; // B=0, I=1, N=2, G=3, O=4

            if (useDiagonal) {
                // Two diagonals: top-left to bottom-right (0,1,2,3,4) or top-right to bottom-left (4,3,2,1,0)
                boolean topLeft = random.nextBoolean();
                if (topLeft) {
                    winPositions = new int[]{0, 1, 2, 3, 4};
                    winType = "DIAGONAL_TL_BR";
                } else {
                    winPositions = new int[]{4, 3, 2, 1, 0};
                    winType = "DIAGONAL_TR_BL";
                }
            } else {
                // ROW strategy
                int winningRow;
                if (canWinOtherRow) {
                    winningRow = random.nextInt(5);
                } else {
                    winningRow = 2; // middle row (N is free)
                }
                for (int i = 0; i < 5; i++) {
                    winPositions[i] = winningRow;
                }
                winType = "ROW_" + winningRow;
            }

            BingoColumn[] columns = BingoColumn.values();
            Map<BingoColumn, List<Integer>> cardNumbers = new EnumMap<>(BingoColumn.class);

            for (int colIdx = 0; colIdx < columns.length; colIdx++) {
                BingoColumn col = columns[colIdx];
                List<Integer> drawnInCol = new ArrayList<>(drawnByColumn.get(col));
                List<Integer> colList = new ArrayList<>(Collections.nCopies(5, 0));
                int winRow = winPositions[colIdx];

                // 1) Place a drawn number in the winning position
                if (col == BingoColumn.N && winRow == 2) {
                    // Free space — already 0, no drawn number needed
                } else {
                    int winNum = drawnInCol.remove(0);
                    colList.set(winRow, winNum);
                }

                // 2) Fill remaining slots with other drawn numbers from this column
                for (int row = 0; row < 5; row++) {
                    if (row == winRow) continue;
                    if (col == BingoColumn.N && row == 2) continue;
                    if (drawnInCol.isEmpty()) break;
                    colList.set(row, drawnInCol.remove(0));
                }

                // 3) Fill any remaining empty slots with random non-drawn numbers
                Set<Integer> usedInCol = new HashSet<>(colList);
                usedInCol.remove(0);
                for (int row = 0; row < 5; row++) {
                    if (col == BingoColumn.N && row == 2) continue;
                    if (colList.get(row) != 0) continue;
                    int num;
                    int attempts = 0;
                    do {
                        num = generateRandomNumberForColumn(col);
                        attempts++;
                    } while ((usedInCol.contains(num) || drawnNumbers.contains(num)) && attempts < 200);
                    colList.set(row, num);
                    usedInCol.add(num);
                }

                cardNumbers.put(col, colList);
            }

            // Marked = all drawn numbers (they are all on the card now)
            Set<Integer> marked = new LinkedHashSet<>(drawnNumbers);

            CardInfo card = new CardInfo();
            card.setCardId("FAKE-" + UUID.randomUUID());
            card.setNumbers(cardNumbers);
            card.setMarked(marked);

            log.info("Room {}: Generated winning card via {} - marked {} numbers: {}", roomId, winType, marked.size(), marked);

            return card;
        }

        int generateRandomNumberForColumn(BingoColumn column) {
            return switch (column) {
                case B -> 1 + random.nextInt(15);
                case I -> 16 + random.nextInt(15);
                case N -> 31 + random.nextInt(15);
                case G -> 46 + random.nextInt(15);
                case O -> 61 + random.nextInt(15);
            };
        }

        Mono<Void> clearRedisState(Long gameId) {
            String drawnKey = fakeWinDrawnNumbersKey(roomId, gameId);
            String triggeredKey = fakeWinTriggeredKey(roomId, gameId);

            log.info("Room {} game {}: Clearing FakeWin Redis state", roomId, gameId);

            return Mono.when(
                    redisTemplate.delete(drawnKey),
                    redisTemplate.delete(triggeredKey)
            )
            .doOnSuccess(v -> log.info("Room {} game {}: FakeWin Redis state cleared successfully", roomId, gameId))
            .doOnError(e -> log.error("Room {} game {}: Failed to clear FakeWin Redis state: {}", roomId, gameId, e.getMessage()))
            .then();
        }

        Map<String, Object> buildBingoPayload(Long gameId, CardInfo cardInfo, String fakeUserId, GamePattern pattern) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("gameId", gameId);
            payload.put("cardId", cardInfo.getCardId());
            payload.put("markedNumbers", new ArrayList<>(cardInfo.getMarked()));
            payload.put("pattern", pattern.name());
            payload.put("userProfileId", Long.parseLong(fakeUserId));
            payload.put("playerName", Constants.getRandomName());
            payload.put("card", cardInfo);
            return payload;
        }
    }

    private Map<String, Object> parsePayload(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse Redis message JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Long convertToLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer convertToInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
