package com.ebingo.backend.externalgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameModeDto {
    private String gameMode;
    private String title;
    private String description;
    private String category;
    private Map<String, String> iconsUrls;
    private boolean multiplayer;
    private String rtp;
    private List<String> bonusTypes;
}
