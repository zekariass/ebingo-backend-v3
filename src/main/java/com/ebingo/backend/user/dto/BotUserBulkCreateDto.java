package com.ebingo.backend.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request to bulk-create bot user profiles and their wallets.
 *
 * ID pattern: user_profile.id = user_profile.telegram_id = wallet.id = wallet.user_profile_id,
 * starting at {@link #startId} and incremented by 1 for each record.
 * Phone numbers start at {@link #startPhone} (Ethiopian format, e.g. 251900017013)
 * and are incremented by 1 for each record.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BotUserBulkCreateDto {

    @NotNull(message = "startId is required")
    @Min(value = 1, message = "startId must be positive")
    private Long startId;

    @NotNull(message = "startPhone is required")
    @Min(value = 1, message = "startPhone must be positive")
    private Long startPhone;

    @NotNull(message = "botRoomId is required")
    private Long botRoomId;

    @NotNull(message = "agentId is required")
    private Long agentId;

    @NotNull(message = "count is required")
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 500, message = "count must not exceed 500")
    private Integer count;

    /**
     * Starting total_available_balance for each created wallet.
     * Defaults to 1,000,000,000 when omitted.
     */
    private BigDecimal initialBalance;

    /**
     * Optional pool of names cycled across the created bots.
     * When omitted, a built-in default pool is used.
     */
    @Valid
    private List<BotName> names;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class BotName {
        @NotNull(message = "firstName is required")
        private String firstName;
        private String lastName;
        private String nickname;
    }
}
