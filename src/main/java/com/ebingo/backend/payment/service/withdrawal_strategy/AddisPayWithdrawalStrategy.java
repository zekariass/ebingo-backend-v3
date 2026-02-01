package com.ebingo.backend.payment.service.withdrawal_strategy;

import com.ebingo.backend.common.Util;
import com.ebingo.backend.payment.config.AddisPayProperties;
import com.ebingo.backend.payment.dto.WithdrawRequestDto;
import com.ebingo.backend.payment.dto.WithdrawalResponseDto;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.enums.WithdrawalMode;
import com.ebingo.backend.payment.service.AddisPayClientService;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.RoundingMode;

@Component("ADDISPAY_WITHDRAWAL")
@RequiredArgsConstructor
@Slf4j
public class AddisPayWithdrawalStrategy implements WithdrawalStrategy {

    private final AddisPayClientService addisPay;
    private final ObjectMapper mapper;
    private final AddisPayProperties addisPayProperties;

    @Override
    public Mono<WithdrawalResponseDto> initiateWithdrawal(PaymentOrder paymentOrder,
                                                          PaymentMethod paymentMethod,
                                                          WithdrawRequestDto request,
                                                          UserProfileDto user) {

        log.info("Initiating withdrawal request: {}", request);

        ObjectNode payload = mapper.createObjectNode();
        ObjectNode data = mapper.createObjectNode();

        // Build data payload
        data.put("cancel_url", addisPayProperties.getCancelUrl());
        data.put("success_url", addisPayProperties.getSuccessUrl());
        data.put("error_url", addisPayProperties.getErrorUrl());
        data.put("order_reason", "Customer Payout");
        data.put("currency", request.getCurrency());
        data.put("customer_name", (user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).strip());
        data.put("phone_number", Util.normalizePhoneNumber(request.getPhoneNumber()));
        data.put("nonce", "payout" + System.currentTimeMillis());
        data.put("payment_method", request.getProviderPaymentMethodName());
        data.put("total_amount", request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        data.put("tx_ref", "payout-" + System.currentTimeMillis());//paymentOrder.getTxnRef());

        payload.set("data", data);
        payload.put("message", "Direct B2C payout request");

        log.info("AddisPay Withdrawal Payload: {}", payload.toPrettyString());

        return addisPay.directPayout(payload)
                .flatMap(json -> {
                    log.info("AddisPay Payout Response: {}", json.toPrettyString());
                    int statusCode = json.path("status_code").asInt();
                    String message = json.path("message").asText();

                    if (statusCode == 913) { // Success
                        return Mono.just(WithdrawalResponseDto.builder()
                                .status(PaymentOrderStatus.COMPLETED)
                                .message(message)
                                .detail(json.path("details").toPrettyString())
                                .data(json.path("data").toPrettyString())
                                .withdrawalMode(WithdrawalMode.ONLINE)
                                .build());
                    } else { // Failure
                        return Mono.just(WithdrawalResponseDto.builder()
                                .status(PaymentOrderStatus.FAILED)
                                .message(json.path("details").asText(message))
                                .data(json.path("data").toPrettyString())
                                .detail(json.path("details").toPrettyString())
                                .withdrawalMode(WithdrawalMode.ONLINE)
                                .build());
                    }
                })
                .doOnError(err -> log.error("AddisPay payout failed: {}", err.getMessage(), err));
    }
}
