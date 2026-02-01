package com.ebingo.backend.common.service;

import com.ebingo.backend.common.dto.DailyAccountingDto;
import com.ebingo.backend.common.dto.DailyAccountingUpdateDto;
import com.ebingo.backend.common.dto.PageResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DailyAccountingService {
    
    Mono<DailyAccountingDto> getById(Long id);
    
    Mono<PageResponse<DailyAccountingDto>> getByAgentId(Long agentId, int page, int size);
    
    Mono<DailyAccountingDto> updateById(Long id, DailyAccountingUpdateDto updateDto);
    
    Mono<PageResponse<DailyAccountingDto>> getByAgentIdAndDateRange(
            Long agentId, LocalDate startDate, LocalDate endDate, int page, int size);
    
    Mono<PageResponse<DailyAccountingDto>> getTodayRecordsForAllAgents(int page, int size);
    
    Mono<DailyAccountingDto> settleById(Long id);
    
    Mono<DailyAccountingDto> getTodayRecordForAgent(Long agentId);
}
