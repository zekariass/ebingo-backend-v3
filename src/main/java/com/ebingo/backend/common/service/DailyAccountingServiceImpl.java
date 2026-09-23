package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.DailyAccountingDto;
import com.ebingo.backend.common.dto.DailyAccountingUpdateDto;
import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.mapper.DailyAccountingMapper;
import com.ebingo.backend.common.repository.DailyAgentAccountingRepository;
import com.ebingo.backend.common.repository.TotalAgentAccountingRepository;
import com.ebingo.backend.system.exceptions.BadRequestException;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyAccountingServiceImpl implements DailyAccountingService {

    private final DailyAgentAccountingRepository repository;
    private final TotalAgentAccountingRepository totalAccountingRepository;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<DailyAccountingDto> getById(Long id) {
        return repository.findById(id)
                .map(DailyAccountingMapper::toDto)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Daily accounting not found with ID: " + id)))
                .doOnSubscribe(s -> log.info("Fetching daily accounting with ID: {}", id))
                .doOnSuccess(dto -> log.info("Fetched daily accounting with ID: {}", id))
                .doOnError(e -> log.error("Failed to fetch daily accounting with ID: {}", id, e));
    }

    @Override
    public Mono<PageResponse<DailyAccountingDto>> getByAgentId(Long agentId, int page, int size) {
        long offset = (long) page * size;

        return repository.countByAgentId(agentId)
                .flatMap(totalElements ->
                        repository.findByAgentIdPaged(agentId, size, offset)
                                .map(DailyAccountingMapper::toDto)
                                .collectList()
                                .map(content -> new PageResponse<>(content, page, size, totalElements))
                )
                .doOnSubscribe(s -> log.info("Fetching daily accounting for agent: {}, page: {}, size: {}", agentId, page, size))
                .doOnSuccess(response -> log.info("Fetched {} daily accounting records for agent: {}", response.getContent().size(), agentId))
                .doOnError(e -> log.error("Failed to fetch daily accounting for agent: {}", agentId, e));
    }

    @Override
    public Mono<DailyAccountingDto> updateById(Long id, DailyAccountingUpdateDto updateDto) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Daily accounting not found with ID: " + id)))
                .flatMap(existing -> {
                    DailyAccountingMapper.toEntity(updateDto, existing);
                    return repository.save(existing);
                })
                .map(DailyAccountingMapper::toDto)
                .doOnSubscribe(s -> log.info("Updating daily accounting with ID: {}", id))
                .doOnSuccess(dto -> log.info("Updated daily accounting with ID: {}", id))
                .doOnError(e -> log.error("Failed to update daily accounting with ID: {}", id, e));
    }

    @Override
    public Mono<PageResponse<DailyAccountingDto>> getByAgentIdAndDateRange(
            Long agentId, LocalDate startDate, LocalDate endDate, int page, int size) {

        if (startDate.isAfter(endDate)) {
            return Mono.error(new BadRequestException("Start date cannot be after end date"));
        }

        long offset = (long) page * size;

        // Use the DB's current date so validation matches the timezone used
        // for accounting_date in the upserts
        return repository.getCurrentDate()
                .flatMap(today -> {
                    if (startDate.isAfter(today)) {
                        return Mono.error(new BadRequestException("Start date cannot be in the future"));
                    }
                    if (endDate.isAfter(today)) {
                        return Mono.error(new BadRequestException("End date cannot be in the future"));
                    }
                    return Mono.just(today);
                })
                .flatMap(today -> repository.countByAgentIdAndDateRange(agentId, startDate, endDate))
                .flatMap(totalElements ->
                        repository.findByAgentIdAndDateRangePaged(agentId, startDate, endDate, size, offset)
                                .map(DailyAccountingMapper::toDto)
                                .collectList()
                                .map(content -> new PageResponse<>(content, page, size, totalElements))
                )
                .doOnSubscribe(s -> log.info(
                        "Fetching daily accounting for agent: {} from {} to {}, page: {}, size: {}",
                        agentId, startDate, endDate, page, size))
                .doOnSuccess(response -> log.info(
                        "Fetched {} daily accounting records for agent: {} in date range",
                        response.getContent().size(), agentId))
                .doOnError(e -> log.error(
                        "Failed to fetch daily accounting for agent: {} in date range", agentId, e));
    }

    @Override
    public Mono<PageResponse<DailyAccountingDto>> getTodayRecordsForAllAgents(int page, int size) {
        long offset = (long) page * size;

        return repository.countToday()
                .flatMap(totalElements ->
                        repository.findTodayPaged(size, offset)
                                .map(DailyAccountingMapper::toDto)
                                .collectList()
                                .map(content -> new PageResponse<>(content, page, size, totalElements))
                )
                .doOnSubscribe(s -> log.info(
                        "Fetching today's daily accounting records for all agents, page: {}, size: {}", page, size))
                .doOnSuccess(response -> log.info(
                        "Fetched {} today's daily accounting records", response.getContent().size()))
                .doOnError(e -> log.error("Failed to fetch today's daily accounting records", e));
    }

    @Override
    public Mono<DailyAccountingDto> settleById(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Daily accounting not found with ID: " + id)))
                .flatMap(dailyAccounting -> {
                    // Check if already settled (atomic UPDATE below also guards against races)
                    if (dailyAccounting.getSettledAt() != null) {
                        return Mono.error(new BadRequestException(
                                "This accounting record has already been settled at: " + dailyAccounting.getSettledAt()));
                    }

                    // Atomically mark settled — returns empty if another request settled it
                    // concurrently or if the record is from today (DB-side CURRENT_DATE)
                    return repository.applySettlement(id)
                            .switchIfEmpty(Mono.error(new BadRequestException(
                                    "Cannot settle this record: it is already settled or belongs to today.")))
                            .flatMap(settledDaily -> {
                                BigDecimal settledAmount = settledDaily.getNetIncome() != null
                                        ? settledDaily.getNetIncome()
                                        : BigDecimal.ZERO;

                                // Atomically accumulate into the agent's total accounting
                                return totalAccountingRepository.applySettlement(settledDaily.getAgentId(), settledAmount)
                                        .switchIfEmpty(Mono.error(
                                                new ResourceNotFoundException(
                                                        "Total accounting not found for agent ID: " + settledDaily.getAgentId())))
                                        .thenReturn(settledDaily);
                            });
                })
                // Daily settle + total accumulation commit or roll back together
                .as(transactionalOperator::transactional)
                .map(DailyAccountingMapper::toDto)
                .doOnSubscribe(s -> log.info("Settling daily accounting with ID: {}", id))
                .doOnSuccess(dto -> log.info("Settled daily accounting with ID: {} at {} with amount: {}", 
                        id, dto.getSettledAt(), dto.getNetIncome()))
                .doOnError(e -> log.error("Failed to settle daily accounting with ID: {}", id, e));
    }

    @Override
    public Mono<DailyAccountingDto> getTodayRecordForAgent(Long agentId) {
        return repository.findTodayByAgentId(agentId)
                .map(DailyAccountingMapper::toDto)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException(
                                "No daily accounting record found for agent ID: " + agentId + " for today")))
                .doOnSubscribe(s -> log.info("Fetching today's daily accounting record for agent: {}", agentId))
                .doOnSuccess(dto -> log.info("Fetched today's daily accounting record for agent: {}", agentId))
                .doOnError(e -> log.error("Failed to fetch today's daily accounting record for agent: {}", agentId, e));
    }
}
