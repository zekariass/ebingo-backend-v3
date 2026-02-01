package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.PageResponse;
import com.ebingo.backend.common.dto.TotalAccountingDto;
import com.ebingo.backend.common.dto.TotalAccountingUpdateDto;
import reactor.core.publisher.Mono;

public interface TotalAccountingService {
    
    Mono<TotalAccountingDto> getById(Long id);
    
    Mono<PageResponse<TotalAccountingDto>> getAll(int page, int size, String sortBy);
    
    Mono<TotalAccountingDto> getByAgentId(Long agentId);
    
    Mono<TotalAccountingDto> updateById(Long id, TotalAccountingUpdateDto updateDto);
}
