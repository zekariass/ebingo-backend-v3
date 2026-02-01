//package com.ebingo.backend.common.service;
//
//import com.ebingo.backend.common.dto.DailyLeaderboardDto;
//import com.ebingo.backend.common.dto.PageResponse;
//import com.ebingo.backend.common.entity.DailyLeaderboard;
//import com.ebingo.backend.common.mapper.DailyLeaderboardMapper;
//import com.ebingo.backend.common.repository.DailyLeaderboardRepository;
//import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
//import com.ebingo.backend.user.dto.UserProfileMinimalDto;
//import com.ebingo.backend.user.repository.UserProfileRepository;
//import com.ebingo.backend.user.service.UserProfileService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class DailyLeaderboardServiceImpl implements DailyLeaderboardService {
//
//    private final DailyLeaderboardRepository repository;
//    private final UserProfileRepository userProfileRepository;
//    private final UserProfileService userProfileService;
//
//    @Override
//    public Mono<Void> incrementDailyStats(Long telegramId, BigDecimal betAmount) {
//        LocalDate today = LocalDate.now();
//        log.info("======================Incrementing daily stats for telegramId: {}, today: {}", telegramId, today);
//
//        return userProfileRepository.findByTelegramId(telegramId)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + telegramId)))
//                .flatMap(user -> {
//                    Long dbUserId = user.getId();
//
//                    return repository.findByUserIdAndLeaderboardDate(dbUserId, today)
//                            .flatMap(db -> {
//                                db.setDailyGamesPlayed(db.getDailyGamesPlayed() + 1);
//                                db.setDailyBets(db.getDailyBets().add(betAmount));
//                                db.setUpdatedAt(LocalDateTime.now());
//                                return repository.save(db);
//                            })
//                            .switchIfEmpty(
//                                    createDailyForStats(dbUserId, today, betAmount, user.getIsBot())
//                            );
//                })
//                .then();
//    }
//
//    private Mono<DailyLeaderboard> createDailyForStats(Long dbUserId, LocalDate today, BigDecimal betAmount, Boolean isBot) {
//        log.info("=======================Creating new daily stats for userId: {}, today: {}", dbUserId, today);
//
//        DailyLeaderboard dl = new DailyLeaderboard(
//                null,
//                dbUserId,
//                1,                    // daily games
//                0,                    // daily wins
//                BigDecimal.ZERO,      // daily prize
//                betAmount,            // daily bets
//                BigDecimal.ZERO,      // daily deposit
//                today,
//                isBot,
//                LocalDateTime.now(),
//                LocalDateTime.now()
//        );
//
//        return repository.save(dl);
//    }
//
//
//    @Override
//    public Mono<Void> incrementDailyWinsAndPrize(Long userId, BigDecimal payout, BigDecimal betAmount) {
//
//        LocalDate today = LocalDate.now();
//
//        return repository.findByUserIdAndLeaderboardDate(userId, today)
//                .flatMap(db -> {
//                    db.setDailyWins(db.getDailyWins() + 1);
//                    db.setDailyPrize(db.getDailyPrize().add(payout));
//                    db.setUpdatedAt(LocalDateTime.now());
//                    return repository.save(db);  // Mono<DailyLeaderboard>
//                })
//                .switchIfEmpty(
//                        createDailyForWin(userId, today, payout, betAmount) // Mono<DailyLeaderboard>
//                )
//                .then(); // final return: Mono<Void>
//    }
//
//    private Mono<DailyLeaderboard> createDailyForWin(Long userId,
//                                                     LocalDate today,
//                                                     BigDecimal payout,
//                                                     BigDecimal betAmount) {
//
//        return userProfileRepository.findById(userId)
//                .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + userId)))
//                .flatMap(user -> {
//
//                    DailyLeaderboard dl = new DailyLeaderboard(
//                            null,
//                            user.getId(),
//                            1,                   // dailyGamesPlayed = 1
//                            1,                   // dailyWins = 1
//                            payout,              // dailyPrize
//                            betAmount,           // dailyBets
//                            BigDecimal.ZERO,     // dailyDeposit
//                            today,
//                            user.getIsBot(),     // <<<<<<<< set isBot
//                            LocalDateTime.now(),
//                            LocalDateTime.now()
//                    );
//
//                    return repository.save(dl);
//                });
//    }
//
//
//    @Override
//    public Mono<Void> incrementDailyDeposit(Long userId, BigDecimal depositAmount) {
//        LocalDate today = LocalDate.now();
//
//        return repository.findByUserIdAndLeaderboardDate(userId, today)
//                .flatMap(db -> {
//                    db.setDailyDeposit(db.getDailyDeposit().add(depositAmount));
//                    db.setUpdatedAt(LocalDateTime.now());
//                    return repository.save(db);
//                })
//                .switchIfEmpty(
//                        createDailyForDeposit(userId, today, depositAmount)
//                )
//                .then();
//    }
//
//    private Mono<DailyLeaderboard> createDailyForDeposit(Long userId,
//                                                         LocalDate today,
//                                                         BigDecimal depositAmount) {
//
//        return userProfileRepository.findById(userId)
//                .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + userId)))
//                .flatMap(user -> {
//
//                    DailyLeaderboard dl = new DailyLeaderboard(
//                            null,
//                            user.getId(),
//                            0,                  // dailyGamesPlayed
//                            0,                  // dailyWins
//                            BigDecimal.ZERO,    // dailyPrize
//                            BigDecimal.ZERO,    // dailyBets
//                            depositAmount,      // dailyDeposit
//                            today,
//                            user.getIsBot(),    // <<<<<<<< set isBot
//                            LocalDateTime.now(),
//                            LocalDateTime.now()
//                    );
//
//                    return repository.save(dl);
//                });
//    }
//
//
//    @Override
//    public Mono<PageResponse<DailyLeaderboardDto>> getDailyLeaderboard(int page, int size, String orderBy, Boolean includeBots) {
//
//        int offset = (page - 1) * size;
//
//        Flux<DailyLeaderboard> leaderboardFlux;
//        Mono<Long> countMono;
//
//        if (includeBots != null) {
//            // Filter by isBot
//            leaderboardFlux = repository.findLeaderboardByIsBot(orderBy, size, offset, includeBots);
//            countMono = repository.countByIsBot(includeBots);
//        } else {
//            // Include all users
//            leaderboardFlux = repository.findLeaderboard(orderBy, size, offset);
//            countMono = repository.countAll();
//        }
//
//        // Enrich each leaderboard entry with user profile
//        Flux<DailyLeaderboardDto> dtoFlux = leaderboardFlux
//                .flatMap(this::enrichDailyWithUserProfile);
//
//        return dtoFlux
//                .collectList()
//                .zipWith(countMono)
//                .map(tuple -> new PageResponse<>(
//                        tuple.getT1(),  // List<DailyLeaderboardDto>
//                        page,
//                        size,
//                        tuple.getT2()   // total count
//                ));
//    }
//
//
//    private Mono<DailyLeaderboardDto> enrichDailyWithUserProfile(DailyLeaderboard entity) {
//        return userProfileService.getUserProfileMinimal(entity.getUserId())
//                .defaultIfEmpty(new UserProfileMinimalDto()) // fallback if profile deleted
//                .map(userProfile -> DailyLeaderboardMapper.toDto(entity, userProfile));
//    }
//
//
//}


package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.DailyLeaderboardDto;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.entity.DailyLeaderboard;
import com.ebingo.backend.common.mapper.DailyLeaderboardMapper;
import com.ebingo.backend.common.repository.DailyLeaderboardRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.dto.UserProfileMinimalDto;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyLeaderboardServiceImpl implements DailyLeaderboardService {

    private final DailyLeaderboardRepository repository;
    private final UserProfileService userProfileService;
    private final DatabaseClient databaseClient;

    private static final ZoneOffset DEFAULT_ZONE = ZoneOffset.UTC;

    private LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    @Override
    public Mono<Void> incrementDailyStats(Long telegramId, BigDecimal betAmount, Long agentId) {
        LocalDate today = today();

        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + telegramId + " agentId: " + agentId)))
                .flatMap(user -> {

//                    if (user.getIsBot()) {
//                        log.info("Skipping daily stats increment for bot user: {}", telegramId);
//                        return Mono.empty();
//                    }

                    return repository.upsertDailyGamesAndBets(
                            user.getId(),
                            agentId,
                            1,
                            betAmount,
                            user.getIsBot(),
                            today
                    );
                })
                .then();
    }

    @Override
    public Mono<Void> incrementDailyWinsAndPrize(Long userId, BigDecimal payout, BigDecimal singleGameFee, Long agentId) {
        LocalDate today = today();

        return userProfileService.getUserProfileMinimal(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + userId)))
                .flatMap(user -> repository.upsertDailyWinsAndPrize(
                        userId,
                        agentId,
                        1,
                        payout,
                        user.getIsBot(),
                        today
                ))
                .then();
    }


    @Override
    public Mono<Void> incrementDailyDeposit(Long userId, BigDecimal depositAmount, Long agentId) {
        LocalDate today = today();

        return userProfileService.getUserProfileMinimal(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + userId)))
                .flatMap(user -> repository.upsertDailyDeposit(
                        userId,
                        agentId,
                        depositAmount,
                        user.getIsBot(),
                        today
                ))
                .then();
    }


//    @Override
//    public Mono<PageResponse<DailyLeaderboardDto>> getDailyLeaderboard(
//            Long agentId, int page,
//            int size,
//            String orderBy,
//            Boolean includeBots
//    ) {
//        int offset = (page - 1) * size;
//        LocalDate today = today();
//
//        String column = switch (orderBy.toLowerCase()) {
//            case "dailygamesplayed" -> "daily_games_played";
//            case "dailywins" -> "daily_wins";
//            case "dailyprize" -> "daily_prize";
//            case "dailybets" -> "daily_bets";
//            case "dailydeposit" -> "daily_deposit";
//            case "createdat" -> "created_at";
//            case "updatedat" -> "updated_at";
//            default -> "daily_wins";
//        };
//
//        // ===== Build WHERE conditions =====
//        List<String> conditions = new ArrayList<>();
//
//        // Always filter by today's leaderboard date
//        conditions.add("leaderboard_date = :today");
//
//        // Only include rows where the ORDER BY column > 0
//        conditions.add(column + " > 0");
//
//        // Optional agent filtering
//        conditions.add("agent_id = :agentId");
//
//        // Optional bot filtering
//        if (Boolean.FALSE.equals(includeBots)) {
//            conditions.add("is_bot = FALSE");
//        }
//
//        String whereClause = " WHERE " + String.join(" AND ", conditions);
//
//        // ===== Final SELECT query =====
//        String sql =
//                "SELECT * FROM daily_leaderboard" +
//                        whereClause +
//                        " ORDER BY " + column + " DESC" +
//                        " LIMIT :size OFFSET :offset";
//
//        Flux<DailyLeaderboard> leaderboardFlux = databaseClient.sql(sql)
//                .bind("today", today)
//                .bind("size", size)
//                .bind("offset", offset)
//                .map((row, meta) -> DailyLeaderboardMapper.fromRow(row))
//                .all();
//
//        // ===== Matching COUNT query =====
//        String countSql =
//                "SELECT COUNT(*) FROM daily_leaderboard" + whereClause;
//
//        Mono<Long> countMono = databaseClient.sql(countSql)
//                .bind("today", today)
//                .map((row, meta) -> row.get(0, Long.class))
//                .one();
//
//        Flux<DailyLeaderboardDto> dtoFlux =
//                leaderboardFlux.flatMap(this::enrichDailyWithUserProfile);
//
//        return dtoFlux.collectList()
//                .zipWith(countMono)
//                .map(t ->
//                        new PageResponse<>(t.getT1(), page, size, t.getT2())
//                );
//    }


    @Override
    public Mono<PageResponse<DailyLeaderboardDto>> getDailyLeaderboard(
            Long agentId,
            int page,
            int size,
            String orderBy,
            Boolean includeBots
    ) {
        // ---- Validate / normalize paging ----
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        // ---- "today" in Europe/London ----
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));

        // ---- Normalize and whitelist orderBy -> DB column ----
        String order = (orderBy == null ? "dailywins" : orderBy).toLowerCase();

        String column = switch (order) {
            case "dailygamesplayed" -> "daily_games_played";
            case "dailywins" -> "daily_wins";
            case "dailyprize" -> "daily_prize";
            case "dailybets" -> "daily_bets";
            case "dailydeposit" -> "daily_deposit";
            case "createdat" -> "created_at";
            case "updatedat" -> "updated_at";
            default -> "daily_wins";
        };

        boolean numericOrderColumn = switch (column) {
            case "daily_games_played", "daily_wins", "daily_prize", "daily_bets", "daily_deposit" -> true;
            default -> false; // created_at, updated_at
        };

        // ---- Build WHERE conditions ----
        List<String> conditions = new ArrayList<>();

        conditions.add("leaderboard_date = :today");
        conditions.add("agent_id = :agentId");

        // Only include rows where the ORDER BY numeric column > 0
        if (numericOrderColumn) {
            conditions.add(column + " > 0");
        }

        if (Boolean.FALSE.equals(includeBots)) {
            conditions.add("is_bot = FALSE");
        }

        String whereClause = " WHERE " + String.join(" AND ", conditions);

        // ---- SELECT query ----
        String sql =
                "SELECT * FROM daily_leaderboard" +
                        whereClause +
                        " ORDER BY " + column + " DESC" +
                        " LIMIT :size OFFSET :offset";

        Flux<DailyLeaderboard> leaderboardFlux = databaseClient.sql(sql)
                .bind("today", today)
                .bind("agentId", agentId)
                .bind("size", safeSize)
                .bind("offset", offset)
                .map((row, meta) -> DailyLeaderboardMapper.fromRow(row))
                .all();

        // ---- COUNT query (must match the same WHERE + binds) ----
        String countSql =
                "SELECT COUNT(*) FROM daily_leaderboard" + whereClause;

        Mono<Long> countMono = databaseClient.sql(countSql)
                .bind("today", today)
                .bind("agentId", agentId)
                .map((row, meta) -> {
                    Number n = row.get(0, Number.class);
                    return n == null ? 0L : n.longValue();
                })
                .one();

        // ---- Enrich + build response ----
        Flux<DailyLeaderboardDto> dtoFlux =
                leaderboardFlux.flatMap(this::enrichDailyWithUserProfile);

        return dtoFlux.collectList()
                .zipWith(countMono)
                .map(t -> new PageResponse<>(t.getT1(), safePage, safeSize, t.getT2()));
    }

    @Override
    public Mono<Void> updateForWithdrawal(Long agentId, Long userId, BigDecimal amount, Boolean isBot) {
        return repository.upsertDailyWithdrawal(userId, agentId, amount, isBot, today())
                .then();
    }

    private Mono<DailyLeaderboardDto> enrichDailyWithUserProfile(DailyLeaderboard entity) {
        return userProfileService.getUserProfileMinimal(entity.getUserId())
                .defaultIfEmpty(new UserProfileMinimalDto())
                .map(userProfile -> DailyLeaderboardMapper.toDto(entity, userProfile));
    }
}
