package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.dto.TotalAccountingDto;
import com.ebingo.backend.common.dto.TotalAccountingUpdateDto;
import com.ebingo.backend.common.mapper.TotalAccountingMapper;
import com.ebingo.backend.common.repository.TotalAgentAccountingRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TotalAccountingServiceImpl implements TotalAccountingService {

    private final TotalAgentAccountingRepository repository;

    @Override
    public Mono<TotalAccountingDto> getById(Long id) {
        return repository.findById(id)
                .map(TotalAccountingMapper::toDto)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Total accounting not found with ID: " + id)))
                .doOnSubscribe(s -> log.info("Fetching total accounting with ID: {}", id))
                .doOnSuccess(dto -> log.info("Fetched total accounting with ID: {}", id))
                .doOnError(e -> log.error("Failed to fetch total accounting with ID: {}", id, e));
    }

    @Override
    public Mono<PageResponse<TotalAccountingDto>> getAll(int page, int size, String sortBy) {
        long offset = (long) page * size;
        
        return repository.countAll()
                .flatMap(totalElements -> {
                    var dataFlux = switch (sortBy != null ? sortBy.toLowerCase() : "id") {
                        case "netincome" -> repository.findAllPagedSortedByNetIncome(size, offset);
                        case "lastsettledat" -> repository.findAllPagedSortedByLastSettledAt(size, offset);
                        case "createdat" -> repository.findAllPagedSortedByCreatedAt(size, offset);
                        case "updatedat" -> repository.findAllPagedSortedByUpdatedAt(size, offset);
                        default -> repository.findAllPaged(size, offset);
                    };
                    
                    return dataFlux
                            .map(TotalAccountingMapper::toDto)
                            .collectList()
                            .map(content -> new PageResponse<>(content, page, size, totalElements));
                })
                .doOnSubscribe(s -> log.info("Fetching all total accounting records, page: {}, size: {}, sortBy: {}", page, size, sortBy))
                .doOnSuccess(response -> log.info("Fetched {} total accounting records", response.getContent().size()))
                .doOnError(e -> log.error("Failed to fetch total accounting records", e));
    }

    @Override
    public Mono<TotalAccountingDto> getByAgentId(Long agentId) {
        return repository.findByAgentId(agentId)
                .map(TotalAccountingMapper::toDto)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Total accounting not found for agent ID: " + agentId)))
                .doOnSubscribe(s -> log.info("Fetching total accounting for agent: {}", agentId))
                .doOnSuccess(dto -> log.info("Fetched total accounting for agent: {}", agentId))
                .doOnError(e -> log.error("Failed to fetch total accounting for agent: {}", agentId, e));
    }

    @Override
    public Mono<TotalAccountingDto> updateById(Long id, TotalAccountingUpdateDto updateDto) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Total accounting not found with ID: " + id)))
                .flatMap(existing -> {
                    TotalAccountingMapper.toEntity(updateDto, existing);
                    return repository.save(existing);
                })
                .map(TotalAccountingMapper::toDto)
                .doOnSubscribe(s -> log.info("Updating total accounting with ID: {}", id))
                .doOnSuccess(dto -> log.info("Updated total accounting with ID: {}", id))
                .doOnError(e -> log.error("Failed to update total accounting with ID: {}", id, e));
    }
}
