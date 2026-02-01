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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyAccountingServiceImpl implements DailyAccountingService {

    private final DailyAgentAccountingRepository repository;
    private final TotalAgentAccountingRepository totalAccountingRepository;

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

        // Validate dates are not in the future
        LocalDate today = LocalDate.now();
        if (startDate.isAfter(today)) {
            return Mono.error(new BadRequestException("Start date cannot be in the future"));
        }
        if (endDate.isAfter(today)) {
            return Mono.error(new BadRequestException("End date cannot be in the future"));
        }
        if (startDate.isAfter(endDate)) {
            return Mono.error(new BadRequestException("Start date cannot be after end date"));
        }

        long offset = (long) page * size;

        return repository.countByAgentIdAndDateRange(agentId, startDate, endDate)
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
        LocalDate today = LocalDate.now();
        long offset = (long) page * size;

        return repository.countByAccountingDate(today)
                .flatMap(totalElements ->
                        repository.findByAccountingDatePaged(today, size, offset)
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
        LocalDate today = LocalDate.now();

        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Daily accounting not found with ID: " + id)))
                .flatMap(dailyAccounting -> {
                    // Validate that the record is not from today
                    if (dailyAccounting.getAccountingDate().equals(today)) {
                        return Mono.error(new BadRequestException(
                                "Cannot settle today's accounting record. Settlement is only allowed for past records."));
                    }

                    // Check if already settled
                    if (dailyAccounting.getSettledAt() != null) {
                        return Mono.error(new BadRequestException(
                                "This accounting record has already been settled at: " + dailyAccounting.getSettledAt()));
                    }

                    // Get the net income to settle
                    BigDecimal settledAmount = dailyAccounting.getNetIncome() != null 
                            ? dailyAccounting.getNetIncome() 
                            : BigDecimal.ZERO;

                    // Set settlement timestamp
                    dailyAccounting.setSettledAt(LocalDateTime.now());

                    // Save daily accounting and update total accounting
                    return repository.save(dailyAccounting)
                            .flatMap(savedDaily -> 
                                    // Update TotalAgentAccounting
                                    totalAccountingRepository.findByAgentId(savedDaily.getAgentId())
                                            .switchIfEmpty(Mono.error(
                                                    new ResourceNotFoundException(
                                                            "Total accounting not found for agent ID: " + savedDaily.getAgentId())))
                                            .flatMap(totalAccounting -> {
                                                // Update lastSettledAmount
                                                totalAccounting.setLastSettledAmount(settledAmount);
                                                
                                                // Add to totalSettledAmount
                                                BigDecimal currentTotal = totalAccounting.getTotalSettledAmount() != null 
                                                        ? totalAccounting.getTotalSettledAmount() 
                                                        : BigDecimal.ZERO;
                                                totalAccounting.setTotalSettledAmount(currentTotal.add(settledAmount));
                                                
                                                // Update lastSettledAt
                                                totalAccounting.setLastSettledAt(LocalDateTime.now());
                                                
                                                return totalAccountingRepository.save(totalAccounting);
                                            })
                                            .thenReturn(savedDaily)
                            );
                })
                .map(DailyAccountingMapper::toDto)
                .doOnSubscribe(s -> log.info("Settling daily accounting with ID: {}", id))
                .doOnSuccess(dto -> log.info("Settled daily accounting with ID: {} at {} with amount: {}", 
                        id, dto.getSettledAt(), dto.getNetIncome()))
                .doOnError(e -> log.error("Failed to settle daily accounting with ID: {}", id, e));
    }

    @Override
    public Mono<DailyAccountingDto> getTodayRecordForAgent(Long agentId) {
        LocalDate today = LocalDate.now();

        return repository.findByAgentIdAndAccountingDate(agentId, today)
                .map(DailyAccountingMapper::toDto)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException(
                                "No daily accounting record found for agent ID: " + agentId + " on date: " + today)))
                .doOnSubscribe(s -> log.info("Fetching today's daily accounting record for agent: {}", agentId))
                .doOnSuccess(dto -> log.info("Fetched today's daily accounting record for agent: {}", agentId))
                .doOnError(e -> log.error("Failed to fetch today's daily accounting record for agent: {}", agentId, e));
    }
}
