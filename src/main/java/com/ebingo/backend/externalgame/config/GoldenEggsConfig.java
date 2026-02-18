package com.ebingo.backend.externalgame.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "golden-eggs")
@Data
public class GoldenEggsConfig {

    private String operatorId;
    private String aggregatorId;
    private String apiBaseUrl = "https://api.golden-eggs.games";
    private String signatureSecret;
    private Integer authTokenTtlMinutes = 60;
    private Integer sessionTokenTtlMinutes = 120;
    private Integer launchTokenTtlMinutes = 60;
    private Integer initDataMaxAgeSeconds = 600; // 10 minutes

    @Bean
    public WebClient goldenEggsWebClient() {
        return WebClient.builder()
                .baseUrl(apiBaseUrl)
                .build();
    }
}
