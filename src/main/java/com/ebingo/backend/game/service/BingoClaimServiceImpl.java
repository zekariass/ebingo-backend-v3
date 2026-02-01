package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.BingoClaimDto;
import com.ebingo.backend.game.mappers.BingoClaimMapper;
import com.ebingo.backend.game.repository.BingoClaimRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class BingoClaimServiceImpl implements BingoClaimService {
    private final BingoClaimRepository bingoClaimRepository;
    private final UserProfileService userProfileService;

    @Override
    public Mono<BingoClaimDto> createBingoClaim(BingoClaimDto bingoClaim) {
        return bingoClaimRepository.save(BingoClaimMapper.toEntity(bingoClaim))
                .map(BingoClaimMapper::toDto)
                .doOnSubscribe(s -> log.info("Creating bingo claim: {}", bingoClaim))
                .doOnSuccess(s -> log.info("Bingo claim created: {}", bingoClaim))
                .doOnError(e -> log.error("Error creating bingo claim: {}", bingoClaim, e));
    }

    @Override
    public Flux<BingoClaimDto> getPaginatedBingoClaim(Long telegramId, Long agentId, Integer page, Integer size, String sortBy) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0 && size <= 100) ? size : 10;
        long offset = (long) pageNumber * pageSize;

        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                .flatMapMany(up -> {
                    String sortKey = (sortBy != null) ? sortBy.toLowerCase() : "id";
                    return switch (sortKey) {
                        case "createdat" -> bingoClaimRepository
                                .findByPlayerIdOrderByCreatedAtDesc(up.getId(), pageSize, offset);
                        default -> bingoClaimRepository
                                .findByPlayerIdOrderByIdDesc(up.getId(), pageSize, offset);
                    };
                })
                .map(BingoClaimMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching past claims - Page: {}, Size: {}, SortBy: {}", page, size, sortBy))
                .doOnComplete(() -> log.info("Completed fetching past claims for a user"))
                .doOnError(e -> log.error("Failed to fetch claims: {}", e.getMessage(), e));
    }

    @Override
    public Mono<BingoClaimDto> getBingoClaimById(Long telegramId, Long id, Long agentId) {
        return userProfileService.getUserProfileByTelegramIdAndAgentId(telegramId, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User profile not found")))
                .flatMap(up -> bingoClaimRepository.findByIdAndPlayerId(id, up.getId()))
                .map(BingoClaimMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching claim by ID: {}", id))
                .doOnSuccess(s -> log.info("Claim fetched by ID: {}", id))
                .doOnError(e -> log.error("Failed to fetch claim by ID: {}", id, e));
    }
}
