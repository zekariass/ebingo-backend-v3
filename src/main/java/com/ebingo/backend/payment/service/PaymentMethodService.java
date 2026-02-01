package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.PaymentMethodDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentMethodService {
    Flux<PaymentMethodDto> getAllPaymentMethods();

    Mono<PaymentMethodDto> getPaymentMethodById(Long id);

    Mono<PaymentMethodDto> getPaymentMethodByCode(String internal);
}
