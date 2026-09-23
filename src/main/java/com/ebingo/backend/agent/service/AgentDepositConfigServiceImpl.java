package com.ebingo.backend.agent.service;

import com.ebingo.backend.agent.dto.agent.AgentDepositConfigDto;
import com.ebingo.backend.agent.entity.AgentDepositConfig;
import com.ebingo.backend.agent.repository.AgentDepositConfigRepository;
import com.ebingo.backend.agent.repository.AgentRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentDepositConfigServiceImpl implements AgentDepositConfigService {

    private final AgentDepositConfigRepository agentDepositConfigRepository;
    private final AgentRepository agentRepository;

    // Global defaults from application.yml (deposit.*) — used when an agent has no row.
    @Value("${deposit.bonusAmount.rate:0.0}")
    private BigDecimal defaultBonusRate;

    @Value("${deposit.bonusAmount.fixed:0.0}")
    private BigDecimal defaultBonusFixed;

    @Value("${deposit.bonusAmount.max.isCaped:false}")
    private Boolean defaultBonusCapEnabled;

    @Value("${deposit.bonusAmount.max.amount:0.0}")
    private BigDecimal defaultBonusCapAmount;

    @Value("${deposit.lockAmount.rate:0.0}")
    private BigDecimal defaultLockRate;

    @Value("${deposit.lockAmount.fixed:0.0}")
    private BigDecimal defaultLockFixed;

    @Value("${deposit.lockAmount.max.isCaped:false}")
    private Boolean defaultLockCapEnabled;

    @Value("${deposit.lockAmount.max.amount:0.0}")
    private BigDecimal defaultLockCapAmount;

    @Override
    public Mono<AgentDepositConfigDto> getConfig(Long agentId) {
        return agentRepository.existsById(agentId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Agent not found with ID: " + agentId));
                    }
                    return getEffectiveConfig(agentId).map(this::toDto);
                })
                .doOnSubscribe(s -> log.debug("Fetching deposit config for agent ID: {}", agentId))
                .doOnError(e -> log.warn("Failed to fetch deposit config for agent ID {}: {}", agentId, e.getMessage()));
    }

    @Override
    public Mono<AgentDepositConfigDto> upsertConfig(Long agentId, AgentDepositConfigDto dto) {
        return agentRepository.existsById(agentId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Agent not found with ID: " + agentId));
                    }
                    AgentDepositConfig entity = toEntity(agentId, dto);
                    return agentDepositConfigRepository.upsert(
                                    entity.getAgentId(),
                                    entity.getBonusRate(),
                                    entity.getBonusFixed(),
                                    entity.getBonusCapEnabled(),
                                    entity.getBonusCapAmount(),
                                    entity.getLockRate(),
                                    entity.getLockFixed(),
                                    entity.getLockCapEnabled(),
                                    entity.getLockCapAmount())
                            .then(agentDepositConfigRepository.findById(agentId))
                            .map(this::toDto);
                })
                .doOnSubscribe(s -> log.info("Upserting deposit config for agent ID: {}", agentId))
                .doOnSuccess(cfg -> log.info("Upserted deposit config for agent ID: {}", agentId))
                .doOnError(e -> log.error("Failed to upsert deposit config for agent ID: {}", agentId, e));
    }

    @Override
    public Mono<Void> deleteConfig(Long agentId) {
        return agentRepository.existsById(agentId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Agent not found with ID: " + agentId));
                    }
                    return agentDepositConfigRepository.deleteById(agentId);
                })
                .doOnSubscribe(s -> log.info("Deleting deposit config for agent ID: {} (reverts to global defaults)", agentId))
                .doOnError(e -> log.error("Failed to delete deposit config for agent ID: {}", agentId, e));
    }

    @Override
    public Mono<AgentDepositConfig> getEffectiveConfig(Long agentId) {
        if (agentId == null) {
            return Mono.just(defaults());
        }
        return agentDepositConfigRepository.findById(agentId)
                .defaultIfEmpty(defaults());
    }

    /** Builds an AgentDepositConfig holding the global application.yml defaults. */
    private AgentDepositConfig defaults() {
        return AgentDepositConfig.builder()
                .agentId(null)
                .bonusRate(defaultZero(defaultBonusRate))
                .bonusFixed(defaultZero(defaultBonusFixed))
                .bonusCapEnabled(Boolean.TRUE.equals(defaultBonusCapEnabled))
                .bonusCapAmount(defaultZero(defaultBonusCapAmount))
                .lockRate(defaultZero(defaultLockRate))
                .lockFixed(defaultZero(defaultLockFixed))
                .lockCapEnabled(Boolean.TRUE.equals(defaultLockCapEnabled))
                .lockCapAmount(defaultZero(defaultLockCapAmount))
                .build();
    }

    private AgentDepositConfig toEntity(Long agentId, AgentDepositConfigDto dto) {
        AgentDepositConfigDto.AmountRule bonus = dto.getBonusAmount();
        AgentDepositConfigDto.AmountRule lock = dto.getLockAmount();
        return AgentDepositConfig.builder()
                .agentId(agentId)
                .bonusRate(bonus.getRate())
                .bonusFixed(bonus.getFixed())
                .bonusCapEnabled(Boolean.TRUE.equals(bonus.getMax().getIsCapped()))
                .bonusCapAmount(bonus.getMax().getAmount())
                .lockRate(lock.getRate())
                .lockFixed(lock.getFixed())
                .lockCapEnabled(Boolean.TRUE.equals(lock.getMax().getIsCapped()))
                .lockCapAmount(lock.getMax().getAmount())
                .build();
    }

    private AgentDepositConfigDto toDto(AgentDepositConfig entity) {
        return AgentDepositConfigDto.builder()
                .agentId(entity.getAgentId())
                .bonusAmount(AgentDepositConfigDto.AmountRule.builder()
                        .rate(entity.getBonusRate())
                        .fixed(entity.getBonusFixed())
                        .max(AgentDepositConfigDto.Cap.builder()
                                .isCapped(Boolean.TRUE.equals(entity.getBonusCapEnabled()))
                                .amount(entity.getBonusCapAmount())
                                .build())
                        .build())
                .lockAmount(AgentDepositConfigDto.AmountRule.builder()
                        .rate(entity.getLockRate())
                        .fixed(entity.getLockFixed())
                        .max(AgentDepositConfigDto.Cap.builder()
                                .isCapped(Boolean.TRUE.equals(entity.getLockCapEnabled()))
                                .amount(entity.getLockCapAmount())
                                .build())
                        .build())
                .build();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
