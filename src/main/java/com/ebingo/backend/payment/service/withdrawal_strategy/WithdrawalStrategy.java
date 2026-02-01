package com.ebingo.backend.payment.service.withdrawal_strategy;

import com.ebingo.backend.payment.dto.WithdrawRequestDto;
import com.ebingo.backend.payment.dto.WithdrawalResponseDto;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.user.dto.UserProfileDto;
import reactor.core.publisher.Mono;

public interface WithdrawalStrategy {
    Mono<WithdrawalResponseDto> initiateWithdrawal(PaymentOrder paymentOrder,
                                                   PaymentMethod paymentMethod,
                                                   WithdrawRequestDto request,
                                                   UserProfileDto user);
}
