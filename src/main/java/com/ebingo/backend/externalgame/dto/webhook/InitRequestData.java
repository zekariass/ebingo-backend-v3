package com.ebingo.backend.externalgame.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitRequestData {
    private String currency;
    private String operator;
    private String gameMode;
}
