package com.ebingo.backend.externalgame.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RollbackRequest {
    private String action;
    private String token;
    private String gameMode;
    private RollbackRequestData data;
}
