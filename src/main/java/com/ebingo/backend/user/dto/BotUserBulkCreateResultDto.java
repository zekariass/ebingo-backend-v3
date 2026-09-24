package com.ebingo.backend.user.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Summary of a bulk bot-user creation run.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BotUserBulkCreateResultDto {

    private int createdCount;
    private Long firstId;
    private Long lastId;
    private String firstPhone;
    private String lastPhone;
    private Long botRoomId;
    private Long agentId;
    private BigDecimal initialBalance;
}
