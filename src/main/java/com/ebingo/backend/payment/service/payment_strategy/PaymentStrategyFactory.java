package com.ebingo.backend.payment.service.payment_strategy;

import com.ebingo.backend.payment.entity.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {
    private final ApplicationContext ctx;

    public PaymentStrategy getStrategy(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return (PaymentStrategy) ctx.getBean("OFFLINE");
        }
        String beanName = paymentMethod.getIsOnline() ? "ADDISPAY" : "OFFLINE";
        if (ctx.containsBean(beanName)) {
            return (PaymentStrategy) ctx.getBean(beanName);
        }
        // fallback to OFFLINE
        return (PaymentStrategy) ctx.getBean("OFFLINE");
    }
}
