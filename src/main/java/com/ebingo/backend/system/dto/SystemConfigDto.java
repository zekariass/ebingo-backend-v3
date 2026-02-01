package com.ebingo.backend.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SystemConfigDto {
    private Long id;
    private Long agentId;
    private String name;
    private String value;
    private String possibleValues; // comma-separated possible values
}
