package com.ebingo.backend.externalgame.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetResponse {
    private String code;
    private String balance;
    private Boolean hideFromStat;
}
