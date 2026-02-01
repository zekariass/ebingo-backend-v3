package com.ebingo.backend.payment.service.payment_strategy;

import com.ebingo.backend.payment.dto.PaymentOrderRequestDto;
import com.ebingo.backend.payment.dto.PaymentOrderResponseDto;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.user.dto.UserProfileDto;
import reactor.core.publisher.Mono;

public interface PaymentStrategy {

    Mono<PaymentOrderResponseDto> initiateOrder(PaymentOrder order, PaymentOrderRequestDto dto, PaymentMethod paymentMethod, UserProfileDto user);
}
