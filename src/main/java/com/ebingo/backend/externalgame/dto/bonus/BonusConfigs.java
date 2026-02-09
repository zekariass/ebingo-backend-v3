package com.ebingo.backend.externalgame.dto.bonus;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All bonus configuration classes for Golden Eggs
 */
public class BonusConfigs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FreebetConfig {
        private String count;
        private ChickenRoadConfig chickenRoadConfig;
        private LuckyMinesConfig luckyMinesConfig;
        private ChickenRoadTwoConfig chickenRoadTwoConfig;
        private Plinko1000Config plinko1000Config;
        private ForestFortuneConfig forestFortuneConfig;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChickenRoadConfig {
        private String betAmount;
        private String difficulty; // EASY, MEDIUM, HARD, DAREDEVIL
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LuckyMinesConfig {
        private String betAmount;
        private String minesCount; // 2-24
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChickenRoadTwoConfig {
        private String betAmount;
        private String difficulty; // EASY, MEDIUM, HARD, DAREDEVIL
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Plinko1000Config {
        private String betAmount;
        private String risk; // LOW, MEDIUM, HIGH
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ForestFortuneConfig {
        private String betAmount;
        private String risk; // LOW, MEDIUM, HIGH
    }
}
