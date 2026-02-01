package com.ebingo.backend.game.utils;

import com.ebingo.backend.game.enums.BingoColumn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BingoCardGenerator {

    /**
     * Generates a pool of unique Bingo cards.
     *
     * @param poolSize Number of cards to generate.
     * @return List of Bingo cards, each represented as a Map<Column, List<Integer>>
     */
    public static List<Map<BingoColumn, List<Integer>>> generateCardPool(int poolSize) {
        System.out.println("============BingoCardGenerator========================>>> CAPACITY RECEIVED" + poolSize);
        List<Map<BingoColumn, List<Integer>>> pool = new ArrayList<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            pool.add(generateBingoCard());
        }

        return pool;
    }

    /**
     * Generates a single Bingo card with center free space (0).
     */
    public static Map<BingoColumn, List<Integer>> generateBingoCard() {
        Map<BingoColumn, List<Integer>> card = new LinkedHashMap<>();

        card.put(BingoColumn.B, generateColumnNumbers(1, 15, 5));
        card.put(BingoColumn.I, generateColumnNumbers(16, 30, 5));

        // N column: 4 numbers + center free space
        List<Integer> nColumn = generateColumnNumbers(31, 45, 4);
        nColumn.add(2, 0); // center free space at 3rd row
        card.put(BingoColumn.N, nColumn);

        card.put(BingoColumn.G, generateColumnNumbers(46, 60, 5));
        card.put(BingoColumn.O, generateColumnNumbers(61, 75, 5));

        return card;
    }

    /**
     * Generates a list of unique random numbers for a column.
     *
     * @param min   Minimum value (inclusive)
     * @param max   Maximum value (inclusive)
     * @param count Number of unique numbers to generate
     */
    private static List<Integer> generateColumnNumbers(int min, int max, int count) {
        return ThreadLocalRandom.current()
                .ints(min, max + 1)
                .distinct()
                .limit(count)
                .boxed()
//                .sorted()
                .collect(Collectors.toList());
    }
}
