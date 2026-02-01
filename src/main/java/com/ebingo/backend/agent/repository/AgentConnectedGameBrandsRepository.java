package com.ebingo.backend.agent.repository;

import com.ebingo.backend.agent.entity.AgentConnectedGameBrand;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AgentConnectedGameBrandsRepository extends ReactiveCrudRepository<AgentConnectedGameBrand, Long> {
}
