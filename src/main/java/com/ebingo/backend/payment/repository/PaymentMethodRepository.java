package com.ebingo.backend.payment.repository;


import com.ebingo.backend.payment.entity.PaymentMethod;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentMethodRepository extends ReactiveCrudRepository<PaymentMethod, Long> {
    Mono<PaymentMethod> findByCode(String internal);
//    Mono<PaymentMethod> findByCode(String code);
}

