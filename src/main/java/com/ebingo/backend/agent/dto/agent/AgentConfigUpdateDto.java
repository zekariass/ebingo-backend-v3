package com.ebingo.backend.agent.dto.agent;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Admin request body for PUT /api/v1/admin/agents/{agentId}/config.
 * Full upsert semantics: the provided values become the stored config.
 * Field names mirror AgentConfigDto so the admin UI can round-trip the GET response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigUpdateDto {

    /** Display name used in bot messages. */
    private String name;

    /** Comma-separated Telegram user IDs of bot admins. */
    private String adminIds;

    /** Static logo filename served by the bot app. */
    private String logoName;

    private String supportContact;

    /** Telegram username without @. */
    private String supportUsername;

    /** Telegram channel/group handle without @. */
    private String supportChannel;

    /** Free-form map keyed by payment method. */
    private Map<String, Object> bankDetails;

    /** UI theme palette key; null resets the agent to the client "default" palette. */
    @Size(max = 64, message = "themeKey must be at most 64 characters")
    private String themeKey;
}
