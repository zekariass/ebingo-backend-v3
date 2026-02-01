package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.TotalCommission;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TotalCommissionRepository extends ReactiveCrudRepository<TotalCommission, Long> {
}
