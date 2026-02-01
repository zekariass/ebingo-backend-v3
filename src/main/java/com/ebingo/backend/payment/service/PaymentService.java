package com.ebingo.backend.payment.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PaymentService {

    Mono<Boolean> processPayment(Long telegramId, BigDecimal amount, Long gameId, Long agentId);

    Mono<Boolean> processRefund(Long telegramId, Long gameId, Long agentId);
}
