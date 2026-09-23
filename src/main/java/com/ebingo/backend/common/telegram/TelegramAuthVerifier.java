package com.ebingo.backend.common.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramAuthVerifier {

    @Value("${telegram.bot.token:}")
    private String botToken;


    public Optional<Map<String, String>> verifyInitData(String initData) {
        if (botToken == null || botToken.isBlank()) {
            // Dev mode: no bot token configured -> skip signature verification, just parse
            log.warn("telegram.bot.token not configured - skipping initData signature verification");
            Map<String, String> params = parseInitData(initData);
            return params.isEmpty() ? Optional.empty() : Optional.of(params);
        }
        return verifyInitData(initData, botToken, 600); // Default 10 minutes
    }

    /**
     * Verify Telegram WebApp initData with custom bot token
     * @param initData Raw initData string from Telegram WebApp
     * @param customBotToken Bot token to use for verification
     * @param maxAgeSeconds Maximum age of auth_date in seconds (e.g., 600 for 10 minutes)
     * @return Optional containing parsed params if valid, empty otherwise
     */
    public Optional<Map<String, String>> verifyInitData(String initData, String customBotToken, int maxAgeSeconds) {
        try {
            Map<String, String> params = parseInitData(initData);
            String receivedHash = params.remove("hash");
            if (receivedHash == null) {
                log.warn("No hash field in Telegram initData");
                return Optional.empty();
            }

            // Check auth_date freshness
            String authDateStr = params.get("auth_date");
            if (authDateStr != null) {
                try {
                    long authDate = Long.parseLong(authDateStr);
                    long currentTime = System.currentTimeMillis() / 1000;
                    if (currentTime - authDate > maxAgeSeconds) {
                        log.warn("Telegram initData expired: auth_date={}, current={}, maxAge={}", 
                                authDate, currentTime, maxAgeSeconds);
                        return Optional.empty();
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid auth_date format: {}", authDateStr);
                    return Optional.empty();
                }
            }

            // Build data check string
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            // Step 1: secretKey = HMAC_SHA256("WebAppData", botToken)
            Mac keyMac = Mac.getInstance("HmacSHA256");
            keyMac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = keyMac.doFinal(customBotToken.getBytes(StandardCharsets.UTF_8));

            // Step 2: computedHash = HMAC_SHA256(secretKey, dataCheckString)
            Mac dataMac = Mac.getInstance("HmacSHA256");
            dataMac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] computed = dataMac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String computedHash = bytesToHex(computed);

            if (receivedHash.equalsIgnoreCase(computedHash)) {
                return Optional.of(params);
            } else {
                log.warn("Telegram initData hash mismatch");
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Telegram verification failed", e);
            return Optional.empty();
        }
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> map = new HashMap<>();
        for (String pair : initData.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
