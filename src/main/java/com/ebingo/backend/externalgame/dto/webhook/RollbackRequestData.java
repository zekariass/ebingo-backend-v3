package com.ebingo.backend.externalgame.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RollbackRequestData {
    @JsonProperty("user_id")
    private String userId;
    
    private String currency;
    private String operator;
    private String amount;
    private UUID transactionId;
    private UUID debitId;
    private UUID gameId;
    private Boolean isFinished;
}
