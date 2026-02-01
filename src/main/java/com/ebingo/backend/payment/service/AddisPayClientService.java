package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.config.AddisPayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddisPayClientService {
    private final AddisPayProperties props;
    private final WebClient webClient = WebClient.builder().build();

    public Mono<JsonNode> createOrder(JsonNode payload) {
        String url = props.getBaseUrl() + "/create-order";
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Auth", props.getApiKey())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("AddisPay create-order failed: {}", e.getMessage()));
    }

    public Mono<JsonNode> checkOrder(String uuid) {
        String url = props.getBaseUrl() + "/get-order?uuid=" + uuid;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("AddisPay get-order failed: {}", e.getMessage()));
    }

    public Mono<JsonNode> initiatePayment(JsonNode payload) {
        String url = props.getBaseUrl() + "/payment/initiate-payment";
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("AddisPay initiate-payment failed: {}", e.getMessage()));
    }

    public Mono<JsonNode> getStatus(String uuid) {
        String url = props.getBaseUrl() + "/get-status?uuid=" + uuid;
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> directPayout(JsonNode payload) {
        String url = props.getBaseUrl() + "/payment/direct-b2c";
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Auth", props.getApiKey())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("==========================>>>>>>>>: AddisPay direct-b2c failed: {}", e.getMessage()));
    }
}
