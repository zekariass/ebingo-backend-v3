package com.ebingo.backend.common.telegram;

import java.util.Map;

/**
 * Simple DTO for extracted Telegram user info
 */
public record TelegramUser(long id, Map<String, Object> rawData) {
}