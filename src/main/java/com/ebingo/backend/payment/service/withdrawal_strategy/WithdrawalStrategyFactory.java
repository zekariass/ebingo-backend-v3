package com.ebingo.backend.payment.service.withdrawal_strategy;

import com.ebingo.backend.payment.dto.WithdrawRequestDto;
import com.ebingo.backend.payment.enums.WithdrawalMode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawalStrategyFactory {

    private final ApplicationContext ctx;

    public WithdrawalStrategy getStrategy(WithdrawRequestDto requestDto) {
        if (requestDto == null) {
            return (WithdrawalStrategy) ctx.getBean("OFFLINE_WITHDRAWAL");
        }
        String beanName = requestDto.getWithdrawalMode() == WithdrawalMode.ONLINE ? "ADDISPAY_WITHDRAWAL" : "OFFLINE_WITHDRAWAL";
        if (ctx.containsBean(beanName)) {
            return (WithdrawalStrategy) ctx.getBean(beanName);
        }
        return (WithdrawalStrategy) ctx.getBean("OFFLINE_WITHDRAWAL");
    }
}

