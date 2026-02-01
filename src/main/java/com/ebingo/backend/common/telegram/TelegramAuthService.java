package com.ebingo.backend.common.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAuthService {

    private final TelegramAuthVerifier telegramAuthVerifier;
    private final ObjectMapper objectMapper;

    public Mono<TelegramUser> verifyAndExtractUser(String telegramInitData) {
        Optional<Map<String, String>> initData = telegramAuthVerifier.verifyInitData(telegramInitData);

        if (initData.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Telegram init data"));
        }

        String userJson = initData.get().get("user");
        if (userJson == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'user' field in initData"));
        }

        try {
            Map<String, Object> userMap = objectMapper.readValue(userJson, Map.class);
            Object idObj = userMap.get("id");
            if (idObj == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing user ID in initData"));
            }

            long telegramId;
            if (idObj instanceof Number n) {
                telegramId = n.longValue();
            } else {
                telegramId = Long.parseLong(idObj.toString());
            }

            return Mono.just(new TelegramUser(telegramId, userMap));

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Telegram user JSON: {}", e.getMessage());
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user JSON"));
        } catch (Exception e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Telegram data format"));
        }
    }
}
