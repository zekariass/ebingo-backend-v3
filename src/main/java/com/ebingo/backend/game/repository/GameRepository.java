package com.ebingo.backend.game.repository;

import com.ebingo.backend.game.entity.Game;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface GameRepository extends ReactiveCrudRepository<Game, Long> {
}
