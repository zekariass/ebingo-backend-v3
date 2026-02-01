package com.ebingo.backend.user.dto;

import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserIdsResponseDto {
    private Long agentId;
    private List<Long> ids;
}
