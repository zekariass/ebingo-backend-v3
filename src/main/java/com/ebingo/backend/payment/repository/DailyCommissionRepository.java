package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.DailyCommission;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DailyCommissionRepository extends ReactiveCrudRepository<DailyCommission, Long> {

    Mono<DailyCommission> findByCommissionDate(LocalDate now);

}
