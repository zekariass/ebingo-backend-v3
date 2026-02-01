//package com.ebingo.backend.common.service;
//
//import com.ebingo.backend.common.dto.PageResponse;
//import com.ebingo.backend.common.dto.TotalLeaderboardDto;
//import com.ebingo.backend.common.entity.TotalLeaderboard;
//import com.ebingo.backend.common.mapper.TotalLeaderboardMapper;
//import com.ebingo.backend.common.repository.TotalLeaderboardRepository;
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
//import java.time.LocalDateTime;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class TotalLeaderboardServiceImpl implements TotalLeaderboardService {
//
//    private final TotalLeaderboardRepository repository;
//    private final UserProfileService userProfileService;
//    private final UserProfileRepository userProfileRepository;
//
//    @Override
//    public Mono<Void> incrementTotalStats(Long telegramId, BigDecimal betAmount) {
//
//        return userProfileRepository.findByTelegramId(telegramId)
//                .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + telegramId)))
//                .flatMap(user -> {
//                    Long dbUserId = user.getId();
//
//                    return repository.findByUserId(dbUserId)
//                            .flatMap(db -> {
//                                db.setTotalGamesPlayed(db.getTotalGamesPlayed() + 1);
//                                db.setTotalBets(db.getTotalBets().add(betAmount));
//                                db.setUpdatedAt(LocalDateTime.now());
//                                return repository.save(db);
//                            })
//                            .switchIfEmpty(
//                                    createNewTotal(dbUserId, betAmount, user.getIsBot())
//                            );
//                })
//                .then();
//    }
//
//    private Mono<TotalLeaderboard> createNewTotal(Long dbUserId, BigDecimal betAmount, Boolean isBot) {
//
//        TotalLeaderboard tl = new TotalLeaderboard(
//                null,
//                dbUserId,
//                1,                   // totalGamesPlayed
//                0,                   // totalWins
//                BigDecimal.ZERO,     // totalPrize
//                betAmount,           // totalBets
//                BigDecimal.ZERO,     // totalDeposit
//                isBot,               // set isBot correctly
//                LocalDateTime.now(),
//                LocalDateTime.now()
//        );
//
//        return repository.save(tl);
//    }
//
//
//    @Override
//    public Mono<Void> incrementTotalWinsAndPrize(Long userId, BigDecimal prizeAmount, BigDecimal betAmount) {
//
//        return repository.findByUserId(userId)
//                .flatMap(db -> {
//                    db.setTotalWins(db.getTotalWins() + 1);
//                    db.setTotalPrize(db.getTotalPrize().add(prizeAmount));
//                    db.setUpdatedAt(LocalDateTime.now());
//                    return repository.save(db);
//                })
//                .switchIfEmpty(
//                        createTotalForWin(userId, prizeAmount, betAmount)
//                )
//                .then();
//    }
//
//    private Mono<TotalLeaderboard> createTotalForWin(Long userId,
//                                                     BigDecimal prizeAmount,
//                                                     BigDecimal betAmount) {
//
//        return userProfileRepository.findById(userId)
//                .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + userId)))
//                .flatMap(user -> {
//
//                    TotalLeaderboard tl = new TotalLeaderboard(
//                            null,
//                            user.getId(),
//                            1,                    // totalGamesPlayed
//                            1,                    // totalWins
//                            prizeAmount,          // totalPrize
//                            betAmount,            // totalBets
//                            BigDecimal.ZERO,      // totalDeposit
//                            user.getIsBot(),      // <<<<<<<< set isBot
//                            LocalDateTime.now(),
//                            LocalDateTime.now()
//                    );
//
//                    return repository.save(tl);
//                });
//    }
//
//
//    @Override
//    public Mono<Void> incrementTotalDeposit(Long userId, BigDecimal depositAmount) {
//        return repository.findByUserId(userId)
//                .flatMap(db -> {
//                    db.setTotalDeposit(db.getTotalDeposit().add(depositAmount));
//                    db.setUpdatedAt(LocalDateTime.now());
//                    return repository.save(db);
//                })
//                .switchIfEmpty(
//                        createTotalForDeposit(userId, depositAmount)
//                )
//                .then();
//    }
//
//
//    private Mono<TotalLeaderboard> createTotalForDeposit(Long userId, BigDecimal depositAmount) {
//
//        return userProfileRepository.findById(userId)
//                .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + userId)))
//                .flatMap(user -> {
//
//                    TotalLeaderboard tl = new TotalLeaderboard(
//                            null,
//                            userId,
//                            0,                  // totalGamesPlayed
//                            0,                  // totalWins
//                            BigDecimal.ZERO,    // totalPrize
//                            BigDecimal.ZERO,    // totalBets
//                            depositAmount,      // totalDeposit
//                            user.getIsBot(),    // <<<<<<<< set isBot
//                            LocalDateTime.now(),
//                            LocalDateTime.now()
//                    );
//
//                    return repository.save(tl);
//                });
//    }
//
//
//    @Override
//    public Mono<PageResponse<TotalLeaderboardDto>> getTotalLeaderboard(int page, int size, String orderBy, Boolean includeBots) {
//
//        int offset = (page - 1) * size;
//
//        Flux<TotalLeaderboard> leaderboardFlux;
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
//        Flux<TotalLeaderboardDto> dtoFlux = leaderboardFlux
//                .flatMap(this::enrichTotalWithUserProfile);
//
//        return dtoFlux
//                .collectList()
//                .zipWith(countMono)
//                .map(tuple -> new PageResponse<>(
//                        tuple.getT1(),  // List<TotalLeaderboardDto>
//                        page,
//                        size,
//                        tuple.getT2()   // total count
//                ));
//    }
//
//
//    private Mono<TotalLeaderboardDto> enrichTotalWithUserProfile(TotalLeaderboard entity) {
//        return userProfileService.getUserProfileMinimal(entity.getUserId())
//                .defaultIfEmpty(new UserProfileMinimalDto()) // fallback if profile deleted
//                .map(userProfile -> TotalLeaderboardMapper.toDto(entity, userProfile));
//    }
//
//
//}


package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.dto.TotalLeaderboardDto;
import com.ebingo.backend.common.entity.TotalLeaderboard;
import com.ebingo.backend.common.mapper.TotalLeaderboardMapper;
import com.ebingo.backend.common.repository.TotalLeaderboardRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TotalLeaderboardServiceImpl implements TotalLeaderboardService {

    private final TotalLeaderboardRepository repository;
    private final UserProfileService userProfileService;
    private final DatabaseClient databaseClient;

    // -----------------------------
    // Increment methods
    // -----------------------------

    @Override
    public Mono<Void> incrementTotalStats(Long telegramId, BigDecimal betAmount, Long agentId) {
        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + telegramId)))
                .flatMap(user -> repository.upsertTotalGamesAndBets(
                        user.getId(),
                        agentId,
                        1,
                        betAmount,
                        user.getIsBot()
                ))
                .then();
    }

    @Override
    public Mono<Void> incrementTotalWinsAndPrize(Long userId, BigDecimal prizeAmount, BigDecimal singleGameFee, Long agentId) {
        return userProfileService.getUserProfileMinimal(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + userId)))
                .flatMap(user -> repository.upsertTotalWinsAndPrize(
                        userId,
                        agentId,
                        1,
                        prizeAmount,
                        user.getIsBot()
                ))
                .then();
    }

    @Override
    public Mono<Void> incrementTotalDeposit(Long userId, BigDecimal depositAmount, Long agentId) {
        return userProfileService.getUserProfileMinimal(userId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + userId)))
                .flatMap(user -> repository.upsertTotalDeposit(
                        userId,
                        agentId,
                        depositAmount,
                        user.getIsBot()
                ))
                .then();
    }

    // -----------------------------
    // Fetch leaderboard
    // -----------------------------

//    @Override
//    public Mono<PageResponse<TotalLeaderboardDto>> getTotalLeaderboard(int page, int size, String orderBy, Boolean includeBots) {
//        int offset = (page - 1) * size;
//
//        String column = switch (orderBy.toLowerCase()) {
//            case "totalgamesplayed" -> "total_games_played";
//            case "totalwins" -> "total_wins";
//            case "totalprize" -> "total_prize";
//            case "totalbets" -> "total_bets";
//            case "totaldeposit" -> "total_deposit";
//            case "createdat" -> "created_at";
//            case "updatedat" -> "updated_at";
//            default -> "totalwins";
//        };
//
//        // Build dynamic SQL
//        String baseQuery = "SELECT * FROM total_leaderboard";
//
//        // includeBots = false → filter humans only
//        if (Boolean.FALSE.equals(includeBots)) {
//            baseQuery += " WHERE is_bot = FALSE";
//        }
//
//        baseQuery += " ORDER BY " + column + " DESC LIMIT :size OFFSET :offset";
//
//        DatabaseClient.GenericExecuteSpec query = databaseClient.sql(baseQuery)
//                .bind("size", size)
//                .bind("offset", offset);
//
//        Flux<TotalLeaderboard> leaderboardFlux = query
//                .map((row, meta) -> TotalLeaderboardMapper.fromRow(row))
//                .all();
//
//        // Count for pagination
//        Mono<Long> countMono = Boolean.FALSE.equals(includeBots)
//                ? databaseClient.sql("SELECT COUNT(*) FROM total_leaderboard WHERE is_bot = FALSE")
//                .map((row, meta) -> row.get(0, Long.class))
//                .one()
//                : databaseClient.sql("SELECT COUNT(*) FROM total_leaderboard")
//                .map((row, meta) -> row.get(0, Long.class))
//                .one();
//
//        Flux<TotalLeaderboardDto> dtoFlux = leaderboardFlux
//                .flatMap(this::enrichTotalWithUserProfile);
//
//        return dtoFlux.collectList()
//                .zipWith(countMono)
//                .map(tuple -> new PageResponse<>(tuple.getT1(), page, size, tuple.getT2()));
//    }


//    @Override
//    public Mono<PageResponse<TotalLeaderboardDto>> getTotalLeaderboard(
//            Long agentId, int page,
//            int size,
//            String orderBy,
//            Boolean includeBots
//    ) {
//        int offset = (page - 1) * size;
//
//        String column = switch (orderBy.toLowerCase()) {
//            case "totalgamesplayed" -> "total_games_played";
//            case "totalwins" -> "total_wins";
//            case "totalprize" -> "total_prize";
//            case "totalbets" -> "total_bets";
//            case "totaldeposit" -> "total_deposit";
//            case "createdat" -> "created_at";
//            case "updatedat" -> "updated_at";
//            default -> "total_wins";
//        };
//
//        // ===== Build WHERE conditions =====
//        List<String> conditions = new ArrayList<>();
//
//        // Only rows where ORDER BY column > 0
//        conditions.add(column + " > 0");
//
//        // includeBots = false → filter bots
//        if (Boolean.FALSE.equals(includeBots)) {
//            conditions.add("is_bot = FALSE");
//        }
//
//        String whereClause = conditions.isEmpty()
//                ? ""
//                : " WHERE " + String.join(" AND ", conditions);
//
//        whereClause += " AND agent_id = :agentId";
//
//        // ===== Final SQL =====
//        String sql =
//                "SELECT * FROM total_leaderboard" +
//                        whereClause +
//                        " ORDER BY " + column + " DESC" +
//                        " LIMIT :size OFFSET :offset";
//
//        Flux<TotalLeaderboard> leaderboardFlux = databaseClient.sql(sql)
//                .bind("size", size)
//                .bind("offset", offset)
//                .map((row, meta) -> TotalLeaderboardMapper.fromRow(row))
//                .all();
//
//        // ===== Count query must match filters =====
//        String countSql =
//                "SELECT COUNT(*) FROM total_leaderboard" + whereClause;
//
//        Mono<Long> countMono = databaseClient.sql(countSql)
//                .map((row, meta) -> row.get(0, Long.class))
//                .one();
//
//        Flux<TotalLeaderboardDto> dtoFlux =
//                leaderboardFlux.flatMap(this::enrichTotalWithUserProfile);
//
//        return dtoFlux.collectList()
//                .zipWith(countMono)
//                .map(t -> new PageResponse<>(t.getT1(), page, size, t.getT2()));
//    }

    @Override
    public Mono<PageResponse<TotalLeaderboardDto>> getTotalLeaderboard(
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

        // ---- Normalize and whitelist orderBy -> DB column ----
        String order = (orderBy == null ? "totalwins" : orderBy).toLowerCase();

        String column = switch (order) {
            case "totalgamesplayed" -> "total_games_played";
            case "totalwins" -> "total_wins";
            case "totalprize" -> "total_prize";
            case "totalbets" -> "total_bets";
            case "totaldeposit" -> "total_deposit";
            case "createdat" -> "created_at";
            case "updatedat" -> "updated_at";
            default -> "total_wins";
        };

        boolean numericOrderColumn = switch (column) {
            case "total_games_played", "total_wins", "total_prize", "total_bets", "total_deposit" -> true;
            default -> false; // created_at, updated_at
        };

        // ---- Build WHERE conditions ----
        List<String> conditions = new ArrayList<>();

        // always filter by agent
        conditions.add("agent_id = :agentId");

        // Only include rows where ORDER BY numeric column > 0
        if (numericOrderColumn) {
            conditions.add(column + " > 0");
        }

        // includeBots = false → filter bots
        if (Boolean.FALSE.equals(includeBots)) {
            conditions.add("is_bot = FALSE");
        }

        String whereClause = " WHERE " + String.join(" AND ", conditions);

        // ---- SELECT query ----
        String sql =
                "SELECT * FROM total_leaderboard" +
                        whereClause +
                        " ORDER BY " + column + " DESC" +
                        " LIMIT :size OFFSET :offset";

        Flux<TotalLeaderboard> leaderboardFlux = databaseClient.sql(sql)
                .bind("agentId", agentId)
                .bind("size", safeSize)
                .bind("offset", offset)
                .map((row, meta) -> TotalLeaderboardMapper.fromRow(row))
                .all();

        // ---- COUNT query ----
        String countSql =
                "SELECT COUNT(*) FROM total_leaderboard" + whereClause;

        Mono<Long> countMono = databaseClient.sql(countSql)
                .bind("agentId", agentId)
                .map((row, meta) -> {
                    Number n = row.get(0, Number.class);
                    return n == null ? 0L : n.longValue();
                })
                .one();

        // ---- Enrich + build response ----
        Flux<TotalLeaderboardDto> dtoFlux =
                leaderboardFlux.flatMap(this::enrichTotalWithUserProfile);

        return dtoFlux.collectList()
                .zipWith(countMono)
                .map(t -> new PageResponse<>(t.getT1(), safePage, safeSize, t.getT2()));
    }

    @Override
    public Mono<Void> updateForWithdrawal(Long agentId, Long userId, BigDecimal amount, Boolean isBot) {
        return repository.upsertTotalWithdrawal(
                userId,
                agentId,
                amount,
                isBot
        ).then();
    }


    private Mono<TotalLeaderboardDto> enrichTotalWithUserProfile(TotalLeaderboard entity) {
        return userProfileService.getUserProfileMinimal(entity.getUserId())
                .defaultIfEmpty(new UserProfileMinimalDto())
                .map(userProfile -> TotalLeaderboardMapper.toDto(entity, userProfile));
    }
}
