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
public class BetRequestData {
    private String amount;
    private String currency;
    private String operator;
    
    @JsonProperty("user_id")
    private String userId;
    
    private UUID transactionId;
    private UUID gameId;
}
