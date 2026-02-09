package com.ebingo.backend.externalgame.controller;

import com.ebingo.backend.externalgame.dto.bonus.BonusDTOs.*;
import com.ebingo.backend.externalgame.service.GoldenEggsBonusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/external-games/golden-eggs/bonuses")
@RequiredArgsConstructor
@Slf4j
public class GoldenEggsBonusController {

    private final GoldenEggsBonusService bonusService;

    /**
     * Create a bonus for a user
     * POST /external-games/golden-eggs/bonuses/{subId}
     */
    @PostMapping(value = "/{subId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CreateBonusResponse> createBonus(
            @PathVariable String subId,
            @Valid @RequestBody CreateBonusRequest request) {
        log.info("Create bonus request: subId={}, bonusId={}, userId={}", 
                subId, request.getBonusId(), request.getUserId());
        return bonusService.createBonus(subId, request);
    }

    /**
     * Fetch bonuses with filters
     * GET /external-games/golden-eggs/bonuses/{subId}
     */
    @GetMapping(value = "/{subId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<FetchBonusesResponse> fetchBonuses(
            @PathVariable String subId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("Fetch bonuses request: subId={}, userId={}, status={}", subId, userId, status);
        return bonusService.fetchBonuses(subId, userId, status, page, limit);
    }

    /**
     * View a single bonus
     * GET /external-games/golden-eggs/bonuses/{subId}/{bonusId}
     */
    @GetMapping(value = "/{subId}/{bonusId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BonusDTO> viewBonus(
            @PathVariable String subId,
            @PathVariable UUID bonusId) {
        log.info("View bonus request: subId={}, bonusId={}", subId, bonusId);
        return bonusService.viewBonus(subId, bonusId);
    }

    /**
     * Cancel a bonus
     * DELETE /external-games/golden-eggs/bonuses/{subId}/{bonusId}
     */
    @DeleteMapping(value = "/{subId}/{bonusId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CancelBonusResponse> cancelBonus(
            @PathVariable String subId,
            @PathVariable UUID bonusId) {
        log.info("Cancel bonus request: subId={}, bonusId={}", subId, bonusId);
        return bonusService.cancelBonus(subId, bonusId);
    }
}
