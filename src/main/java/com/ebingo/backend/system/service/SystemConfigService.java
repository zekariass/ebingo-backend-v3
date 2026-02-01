package com.ebingo.backend.system.service;

import com.ebingo.backend.system.dto.SystemConfigDto;
import com.ebingo.backend.system.dto.SystemConfigUpdateDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SystemConfigService {
    Mono<SystemConfigDto> getSystemConfigByNameAndAgentId(String name, Long agentId);

    Flux<SystemConfigDto> getAllSystemConfigs(Long agentId);

    Mono<SystemConfigDto> updateSystemConfigById(Long id, SystemConfigUpdateDto systemConfigUpdateDto);
}
