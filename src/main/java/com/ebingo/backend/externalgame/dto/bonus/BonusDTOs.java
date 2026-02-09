package com.ebingo.backend.externalgame.dto.bonus;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * All DTOs for Golden Eggs Bonus API
 */
public class BonusDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateBonusRequest {
        @NotBlank(message = "User ID is required")
        private String userId;

        @NotNull(message = "Bonus ID is required")
        private UUID bonusId;

        private List<String> gameModes;

        @NotBlank(message = "Currency is required")
        private String currency;

        @NotBlank(message = "Type is required")
        private String type; // FREEBET

        @NotNull(message = "Expires at is required")
        private Instant expiresAt;

        @NotNull(message = "Freebet config is required")
        private BonusConfigs.FreebetConfig freebetConfig;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateBonusResponse {
        private Boolean status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BonusDTO {
        private UUID bonusId;
        private String userId;
        private UUID operatorId;
        private String gameMode;
        private List<String> gameModeOptions;
        private String type;
        private String status;
        private String currency;
        private Integer bonusQuantity;
        private Integer bonusAvailable;
        private BigDecimal winSum;
        private Object gameConfig; // Dynamic based on game mode
        private Instant expiresAt;
        private Instant expiresWhenActiveAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FetchBonusesResponse {
        private List<BonusDTO> bonuses;
        private PageInfo pageInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageInfo {
        private Integer pages;
        private Integer total;
        private Integer page;
        private Integer limit;
        private Boolean hasNext;
        private Boolean hasPrev;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CancelBonusResponse {
        private Boolean status;
    }

    // Webhook DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BonusWebhookRequest {
        private String action; // bonus-complete, bonus-expired-when-active
        private String token;
        private String gameMode;
        private BonusWebhookData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BonusWebhookData {
        private UUID bonusId;
        private String currency;
        private String winSum;
        private String operator;
        private String userId;
        private UUID transactionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BonusWebhookResponse {
        private String code;
        private String balance;
        private Boolean hideFromStat;
    }
}
