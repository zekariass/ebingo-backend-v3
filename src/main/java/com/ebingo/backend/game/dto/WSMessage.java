package com.ebingo.backend.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WSMessage {
    private String type;
    private Map<String, Object> payload;
}