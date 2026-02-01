package com.ebingo.backend.payment.service.payment_strategy;

import com.ebingo.backend.payment.dto.PaymentOrderRequestDto;
import com.ebingo.backend.payment.dto.PaymentOrderResponseDto;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.user.dto.UserProfileDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("OFFLINE")
public class OfflinePaymentStrategy implements PaymentStrategy {
    @Override
    public Mono<PaymentOrderResponseDto> initiateOrder(PaymentOrder order, PaymentOrderRequestDto dto, PaymentMethod paymentMethod, UserProfileDto user) {
        return Mono.just(PaymentOrderResponseDto.builder()
                .orderId(order.getId())
                .txnRef(order.getTxnRef())
                .status(PaymentOrderStatus.AWAITING_APPROVAL)
                .amount(order.getAmount())
                .instructionsUrl(paymentMethod.getInstructionUrl())
                .paymentMethodId(paymentMethod.getId())
                .build());
    }
}
