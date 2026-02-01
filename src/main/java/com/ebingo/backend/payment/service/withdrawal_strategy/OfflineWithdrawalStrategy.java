package com.ebingo.backend.payment.service.withdrawal_strategy;

import com.ebingo.backend.payment.dto.WithdrawRequestDto;
import com.ebingo.backend.payment.dto.WithdrawalResponseDto;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.WithdrawalMode;
import com.ebingo.backend.user.dto.UserProfileDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("OFFLINE_WITHDRAWAL")
public class OfflineWithdrawalStrategy implements WithdrawalStrategy {

    @Override
    public Mono<WithdrawalResponseDto> initiateWithdrawal(PaymentOrder paymentOrder,
                                                          PaymentMethod paymentMethod,
                                                          WithdrawRequestDto request,
                                                          UserProfileDto user) {

        // Offline withdrawals can be manually processed
        return Mono.just(WithdrawalResponseDto.builder()
                .message("Offline withdrawal initiated. Follow instructions to complete.")
                .status(PaymentOrderStatus.PENDING)
                .withdrawalMode(WithdrawalMode.OFFLINE)
                .build());
    }
}
