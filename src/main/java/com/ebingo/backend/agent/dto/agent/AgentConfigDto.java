package com.ebingo.backend.agent.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Bot-facing agent configuration.
 * Field names are contractual — the bot server consumes them as-is.
 * NOTE: "recieverName" inside bankDetails values is intentionally misspelled
 * to match the existing client contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigDto {

    private Long agentId;

    /** Display name used in bot messages (maps to agent_config.brand_name). */
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

    /** Free-form map keyed by payment method (e.g. telebirr, cbeonline). */
    private Map<String, Object> bankDetails;
}
