package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.payment.config.AddisPayProperties;
import com.ebingo.backend.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class AddisPayWebhookController {

    private final PaymentOrderService orderService;
    private final AddisPayProperties addisPayProperties;

    @PostMapping("/addispay/success")
    public Mono<ResponseEntity<String>> addisCallbackSuccess(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String headerSignature,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("AddisPay webhook success received: {}", payload);
        log.info("Header Signature: {}", headerSignature);

        String sessionUuid = toStringSafe(payload.get("session_uuid"));
        if (sessionUuid == null) {
            return Mono.just(ResponseEntity.badRequest().body("missing session_uuid"));
        }

        // TODO: uncomment signature verification later
//        boolean verified = verifyWebhookSignature(headerSignature, sessionUuid);
//        if (!verified) {
//            log.warn("Invalid webhook signature for success: {}", payload);
//            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature"));
//        }

        return orderService.processAddisPayCallbackForSuccess(
                        sessionUuid,
                        toStringSafe(payload.get("payment_status")),
                        toStringSafe(payload.get("total_amount")),
                        toStringSafe(payload.get("order_id")),
                        toStringSafe(payload.get("nonce")),
                        toStringSafe(payload.get("addispay_transaction_id")),
                        toStringSafe(payload.get("third_party_transaction_ref"))
                )
                .thenReturn(ResponseEntity.ok("ok"));
    }

    @PostMapping("/addispay/error")
    public Mono<ResponseEntity<String>> addisCallbackError(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String headerSignature,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("AddisPay webhook for error received: {}", payload);
        log.info("Header Signature for error: {}", headerSignature);

        String id = toStringSafe(payload.get("id")); // use id for error
        if (id == null) {
            return Mono.just(ResponseEntity.badRequest().body("missing id"));
        }

        // TODO: uncomment signature verification later
//        boolean verified = verifyWebhookSignature(headerSignature, id);
//        if (!verified) {
//            log.warn("Invalid webhook signature for error: {}", payload);
//            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature"));
//        }

        return orderService.processAddisPayCallbackForError(
                        id,
                        toStringSafe(payload.get("payment_status")),
                        toStringSafe(payload.get("total_amount")),
                        toStringSafe(payload.get("order_id")),
                        toStringSafe(payload.get("nonce")),
                        toStringSafe(payload.get("addispay_transaction_id")),
                        toStringSafe(payload.get("third_party_transaction_ref"))
                )
                .thenReturn(ResponseEntity.ok("ok"));
    }

    @PostMapping("/addispay/cancel")
    public Mono<ResponseEntity<String>> addisCallbackCancel(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String headerSignature,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("AddisPay webhook for cancel received: {}", payload);
        log.info("Header Signature for cancel: {}", headerSignature);

        String sessionUuid = toStringSafe(payload.get("session_uuid"));
        if (sessionUuid == null) {
            return Mono.just(ResponseEntity.badRequest().body("missing session_uuid"));
        }

        // TODO: uncomment signature verification later
//        boolean verified = verifyWebhookSignature(headerSignature, sessionUuid);
//        if (!verified) {
//            log.warn("Invalid webhook signature for cancel: {}", payload);
//            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature"));
//        }

        return orderService.processAddisPayCallbackCancel(
                        sessionUuid,
                        toStringSafe(payload.get("payment_status")),
                        toStringSafe(payload.get("total_amount")),
                        toStringSafe(payload.get("order_id")),
                        toStringSafe(payload.get("nonce")),
                        toStringSafe(payload.get("addispay_transaction_id")),
                        toStringSafe(payload.get("third_party_transaction_ref"))
                )
                .thenReturn(ResponseEntity.ok("ok"));
    }

    /**
     * Signature verification using static webhookHash + sessionId/id
     */
    private boolean verifyWebhookSignature(String headerSignature, String idOrSession) {
        if (headerSignature == null) return false;

        String expectedSignature = addisPayProperties.getWebhookHash() + idOrSession;
        log.info("Expected signature: {}", expectedSignature);
        log.info("Received signature: {}", headerSignature);

        return expectedSignature.equals(headerSignature);
    }

    private String toStringSafe(Object value) {
        return value == null ? null : value.toString();
    }
}
