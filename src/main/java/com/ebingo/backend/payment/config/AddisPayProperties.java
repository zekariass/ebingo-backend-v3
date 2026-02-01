package com.ebingo.backend.payment.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "addis-pay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddisPayProperties {
    private String apiKey;
    private String baseUrl; // uat or prod
    private String webhookHash;
    private String redirectUrl;
    private String successUrl;
    private String errorUrl;
    private String cancelUrl;
}

