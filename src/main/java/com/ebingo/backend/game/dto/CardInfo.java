package com.ebingo.backend.game.dto;

import com.ebingo.backend.game.enums.BingoColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardInfo {
    private String cardId;
    private Map<BingoColumn, List<Integer>> numbers = new ConcurrentHashMap<>(); // 25 numbers for 5x5
    private Set<Integer> marked = new LinkedHashSet<>();
}
