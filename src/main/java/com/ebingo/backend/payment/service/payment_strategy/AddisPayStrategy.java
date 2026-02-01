//package com.ebingo.backend.payment.service.payment_strategy;
//
//import com.ebingo.backend.payment.config.AddisPayProperties;
//import com.ebingo.backend.payment.dto.PaymentOrderRequestDto;
//import com.ebingo.backend.payment.dto.PaymentOrderResponseDto;
//import com.ebingo.backend.payment.entity.PaymentMethod;
//import com.ebingo.backend.payment.entity.PaymentOrder;
//import com.ebingo.backend.payment.enums.PaymentOrderStatus;
//import com.ebingo.backend.payment.service.AddisPayClientService;
//import com.ebingo.backend.user.dto.UserProfileDto;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//
//@Component("ADDISPAY")
//@RequiredArgsConstructor
//@Slf4j
//public class AddisPayStrategy implements PaymentStrategy {
//    private final AddisPayClientService addisPay;
//    private final ObjectMapper mapper;
//    private final AddisPayProperties addisPayProperties;
//
//    @Override
//    public Mono<PaymentOrderResponseDto> initiateOrder(PaymentOrder order, PaymentOrderRequestDto dto, PaymentMethod paymentMethod, UserProfileDto user) {
//        // Build payload
//        log.info("Initiating payment order: {}", order);
//        ObjectNode payload = mapper.createObjectNode();
//        ObjectNode data = mapper.createObjectNode();
//        ObjectNode orderDetail = mapper.createObjectNode();
//
//        // order_detail object
//        orderDetail.put("amount", order.getAmount().toPlainString());
//        orderDetail.put("description", order.getReason() == null ? "Test payment" : order.getReason());
//
//        String phoneNumber = order.getPhoneNumber();
//        if (phoneNumber.contains("+")) {
//            phoneNumber = phoneNumber.replace("+", "");
//        }
//
//        // data object
//        data.put("redirect_url", addisPayProperties.getRedirectUrl());
//        data.put("cancel_url", addisPayProperties.getCancelUrl());
//        data.put("success_url", addisPayProperties.getSuccessUrl());
//        data.put("error_url", addisPayProperties.getErrorUrl());
//        data.put("order_reason", order.getReason());
//        data.put("currency", order.getCurrency());
//        data.put("email", "");
//        data.put("first_name", user.getFirstName());
//        data.put("last_name", user.getLastName() != null ? user.getLastName() : "");
//        data.put("nonce", order.getNonce());
//        data.put("phone_number", phoneNumber);
//        data.put("session_expired", ""); // ISO 8601 date string
//        data.put("total_amount", order.getAmount().toPlainString());
//        data.put("tx_ref", order.getTxnRef());
//        data.set("order_detail", orderDetail);
//
//        // Wrap into payload
//        payload.set("data", data);
//        payload.put("message", "Create AddisPay order");
//
//        // Call AddisPay API
//        log.info("AddisPay OrderPayload: {}", payload.toPrettyString());
//        return addisPay.createOrder(payload)
//                .flatMap(json -> {
//                    String uuid = json.path("uuid").asText(null);
//                    String checkoutUrl = json.path("checkout_url").asText(null);
//
//                    if (uuid == null) {
//                        return Mono.error(new IllegalStateException("AddisPay did not return a UUID"));
//                    }
//
//                    return addisPay.checkOrder(uuid)
//                            .map(result -> PaymentOrderResponseDto.builder()
//                                    .orderId(order.getId())
//                                    .txnRef(order.getTxnRef())
//                                    .status(PaymentOrderStatus.INITIATED)
//                                    .amount(order.getAmount())
//                                    .providerUuid(uuid)
//                                    .checkoutUrl(checkoutUrl)
//                                    .checkData(result)
//                                    .paymentMethodId(paymentMethod.getId())
//                                    .build());
//                })
//                .doOnError(err -> log.error("AddisPay create-order failed: {}", err.getMessage(), err));
//    }
//
//}

package com.ebingo.backend.payment.service.payment_strategy;

import com.ebingo.backend.payment.config.AddisPayProperties;
import com.ebingo.backend.payment.dto.PaymentOrderRequestDto;
import com.ebingo.backend.payment.dto.PaymentOrderResponseDto;
import com.ebingo.backend.payment.entity.PaymentMethod;
import com.ebingo.backend.payment.entity.PaymentOrder;
import com.ebingo.backend.payment.enums.PaymentOrderStatus;
import com.ebingo.backend.payment.service.AddisPayClientService;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component("ADDISPAY")
@RequiredArgsConstructor
@Slf4j
public class AddisPayStrategy implements PaymentStrategy {

    private final AddisPayClientService addisPay;
    private final ObjectMapper mapper;
    private final AddisPayProperties addisPayProperties;

    @Override
    public Mono<PaymentOrderResponseDto> initiateOrder(
            PaymentOrder order,
            PaymentOrderRequestDto dto,
            PaymentMethod paymentMethod,
            UserProfileDto user
    ) {
        log.info("Initiating AddisPay order for txnRef={}, amount={}", order.getTxnRef(), order.getAmount());

        try {
            validateInputs(order, user);

            ObjectNode payload = buildPayload(order, user);

            log.info("AddisPay Payload (sanitized): {}", payload);

            return addisPay.createOrder(payload)
                    .flatMap(json -> {
                        String uuid = json.path("uuid").asText(null);
                        String checkoutUrl = json.path("checkout_url").asText(null);

                        if (uuid == null) {
                            log.error("AddisPay response missing UUID: {}", json);
                            return Mono.error(new IllegalStateException("AddisPay did not return a UUID"));
                        }

                        if (checkoutUrl == null) {
                            log.error("AddisPay response missing checkout URL: {}", json);
                            return Mono.error(new IllegalStateException("AddisPay did not return a checkout URL"));
                        }

                        return addisPay.checkOrder(uuid)
                                .map(result -> PaymentOrderResponseDto.builder()
                                        .orderId(order.getId())
                                        .txnRef(order.getTxnRef())
                                        .status(PaymentOrderStatus.INITIATED)
                                        .amount(order.getAmount())
                                        .providerUuid(uuid)
                                        .checkoutUrl(checkoutUrl)
                                        .checkData(result)
                                        .paymentMethodId(paymentMethod.getId())
                                        .build());
                    })
                    .doOnError(err -> log.error("AddisPay order initiation failed for txnRef={}: {}", order.getTxnRef(), err.getMessage(), err));

        } catch (Exception e) {
            log.error("Error building AddisPay order payload: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }

    private void validateInputs(PaymentOrder order, UserProfileDto user) {
        if (order.getPhoneNumber() == null || user.getFirstName() == null) {
            throw new IllegalArgumentException("Missing required fields: phone number or first name");
        }
    }

    private ObjectNode buildPayload(PaymentOrder order, UserProfileDto user) {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode data = mapper.createObjectNode();
        ObjectNode orderDetail = mapper.createObjectNode();

        orderDetail.put("amount", order.getAmount().toPlainString());
        orderDetail.put("description", order.getReason() != null ? order.getReason() : "Ebingo payment");

        String phoneNumber = order.getPhoneNumber().replace("+", "");

        data.put("redirect_url", addisPayProperties.getRedirectUrl());
        data.put("cancel_url", addisPayProperties.getCancelUrl());
        data.put("success_url", addisPayProperties.getSuccessUrl());
        data.put("error_url", addisPayProperties.getErrorUrl());
        data.put("order_reason", order.getReason());
        data.put("currency", order.getCurrency());
        data.put("email", "");
        data.put("first_name", user.getFirstName());
        data.put("last_name", user.getLastName() != null ? user.getLastName() : "");
        data.put("nonce", order.getNonce());
        data.put("phone_number", phoneNumber);
        data.put("session_expired", Instant.now().plus(15, ChronoUnit.MINUTES).toString());
        data.put("total_amount", order.getAmount().toPlainString());
        data.put("tx_ref", order.getTxnRef());
        data.set("order_detail", orderDetail);

        payload.set("data", data);
        payload.put("message", "Create AddisPay order");

        return payload;
    }
}
