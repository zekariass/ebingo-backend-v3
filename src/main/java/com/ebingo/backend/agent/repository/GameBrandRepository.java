package com.ebingo.backend.agent.repository;

import com.ebingo.backend.agent.entity.GameBrand;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface GameBrandRepository extends ReactiveCrudRepository<GameBrand, Long> {
}
