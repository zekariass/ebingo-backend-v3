package com.ebingo.backend.system.service;

import com.ebingo.backend.system.Repository.SystemConfigRepository;
import com.ebingo.backend.system.dto.SystemConfigDto;
import com.ebingo.backend.system.dto.SystemConfigUpdateDto;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.system.mapper.SystemConfigMapper;
import com.ebingo.backend.system.redis.CacheKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {
    private final SystemConfigRepository systemConfigRepository;
    private final CacheService cacheService;

//    @Override
//    public Mono<SystemConfigDto> getSystemConfig(String name) {
//        return systemConfigRepository.findByName(name)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("SYSTEM_CONFIG_NOT_FOUND")))
//                .map(SystemConfigMapper::toDto)
//                .doOnSubscribe(s -> log.info("Fetching system config {}", name))
//                .doOnSuccess(s -> log.info("Fetched system config {}", name))
//                .doOnError(e -> log.error("Failed to fetch system config {}", name, e));
//    }


    @Override
    public Mono<SystemConfigDto> getSystemConfigByNameAndAgentId(String name, Long agentId) {

        String systemConfigKey = CacheKeyUtil.getSystemConfigByNameKey(name, agentId);

        Mono<SystemConfigDto> systemConfig = systemConfigRepository.findByNameAndAgentId(name, agentId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("SYSTEM_CONFIG_NOT_FOUND")))
                .map(SystemConfigMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching system config {}", name))
                .doOnSuccess(s -> log.info("Fetched system config {}", name))
                .doOnError(e -> log.error("Failed to fetch system config {}", name, e));

        return cacheService.cacheMono(
                systemConfigKey,
                systemConfig,
                SystemConfigDto.class
        );
    }

//    @Override
//    public Flux<SystemConfigDto> getAllSystemConfigs() {
//        return systemConfigRepository.findAll()
//                .map(SystemConfigMapper::toDto)
//                .doOnSubscribe(s -> log.info("Fetching all system configs"))
//                .doOnComplete(() -> log.info("Fetched all system configs"))
//                .doOnError(e -> log.error("Failed to fetch all system configs", e));
//    }


    @Override
    public Flux<SystemConfigDto> getAllSystemConfigs(Long agentId) {

        String systemConfigKey = CacheKeyUtil.getSystemConfigsByAgentKey(agentId);

        Flux<SystemConfigDto> systemConfigs = systemConfigRepository.findByAgentId(agentId)
                .map(SystemConfigMapper::toDto)
                .doOnSubscribe(s -> log.info("Fetching all system configs"))
                .doOnComplete(() -> log.info("Fetched all system configs"))
                .doOnError(e -> log.error("Failed to fetch all system configs", e));

        return cacheService.cacheFlux(
                systemConfigKey,
                systemConfigs,
                SystemConfigDto.class
        );
    }


    //  Update system config by id. Only 'value' field is updatable.
    @Override
    public Mono<SystemConfigDto> updateSystemConfigById(
            Long id,
            SystemConfigUpdateDto systemConfigUpdateDto
    ) {
        return systemConfigRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("SYSTEM_CONFIG_NOT_FOUND")))
                .flatMap(existing -> {

                    if (systemConfigUpdateDto.getValue() != null) {
                        existing.setValue(systemConfigUpdateDto.getValue());
                    }

                    return systemConfigRepository.save(existing);
                })
                .flatMap(updated -> {
                    // Evict caches AFTER successful save
                    String allConfigsKey = CacheKeyUtil.getSystemConfigsByAgentKey(updated.getAgentId());
                    String configByNameKey =
                            CacheKeyUtil.getSystemConfigByNameKey(updated.getName(), updated.getAgentId());

                    return cacheService.evict(allConfigsKey)
                            .then(cacheService.evict(configByNameKey))
                            .thenReturn(SystemConfigMapper.toDto(updated));
                })
                .doOnSubscribe(s ->
                        log.info("Updating system config id={}", id))
                .doOnSuccess(s ->
                        log.info("Updated system config id={}", id))
                .doOnError(e ->
                        log.error("Failed to update system config id={}", id, e));
    }
}
