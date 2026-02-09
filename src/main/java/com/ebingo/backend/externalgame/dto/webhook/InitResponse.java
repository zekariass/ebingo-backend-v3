package com.ebingo.backend.externalgame.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitResponse {
    private String code;
    private String userId;
    private String nickname;
    private String balance;
    private String currency;
    private String operator;
    private String userAvatar;
    private String token; // Optional session token
}
