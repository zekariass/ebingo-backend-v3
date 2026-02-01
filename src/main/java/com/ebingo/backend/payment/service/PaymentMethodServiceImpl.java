package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.PaymentMethodDto;
import com.ebingo.backend.payment.mappers.PaymentMethodMapper;
import com.ebingo.backend.payment.repository.PaymentMethodRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public Flux<PaymentMethodDto> getAllPaymentMethods() {
        log.info("Fetching all payment methods");
        return paymentMethodRepository.findAll()
                .onErrorMap(e -> new RuntimeException("Failed to fetch payment methods", e))
                .map(PaymentMethodMapper::toDto);
    }

    @Override
    public Mono<PaymentMethodDto> getPaymentMethodById(Long id) {
        log.info("Fetching payment method by id: {}", id);
        return paymentMethodRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment method not found with id: " + id)))
                .map(PaymentMethodMapper::toDto);
//                .onErrorMap(e -> new RuntimeException("Failed to fetch payment method", e));
    }

    @Override
    public Mono<PaymentMethodDto> getPaymentMethodByCode(String internal) {
        return paymentMethodRepository.findByCode(internal)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment method not found with code: " + internal)))
                .map(PaymentMethodMapper::toDto);
    }

}
