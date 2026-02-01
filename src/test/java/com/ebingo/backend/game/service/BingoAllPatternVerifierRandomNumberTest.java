package com.ebingo.backend.game.service;

import com.ebingo.backend.game.enums.BingoColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BingoAllPatternVerifierRandomNumberTest {

    private BingoPatternVerifier verifier;
    private Map<BingoColumn, List<Integer>> validCard;
    private Random random = new Random(12345); // fixed seed for reproducibility

    @BeforeEach
    void setUp() {
        verifier = new BingoPatternVerifier();
        validCard = generateRandomCard();
    }

    // Generate a valid randomized card
    private Map<BingoColumn, List<Integer>> generateRandomCard() {
        Map<BingoColumn, List<Integer>> card = new LinkedHashMap<>();
        card.put(BingoColumn.B, randomNumbers(1, 15, 5));
        card.put(BingoColumn.I, randomNumbers(16, 30, 5));
        card.put(BingoColumn.N, randomNumbers(31, 45, 4)); // center free space will be inserted
        card.put(BingoColumn.G, randomNumbers(46, 60, 5));
        card.put(BingoColumn.O, randomNumbers(61, 75, 5));
        return card;
    }

    // Generate a shuffled set of n numbers in range [min,max]
    private List<Integer> randomNumbers(int min, int max, int count) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = min; i <= max; i++) numbers.add(i);
        Collections.shuffle(numbers, random);
        return new ArrayList<>(numbers.subList(0, count));
    }

    // Utility to mark numbers
    private Set<Integer> marked(Integer... nums) {
        return new HashSet<>(Arrays.asList(nums));
    }

    // ---------------------------
    // Winning pattern tests
    // ---------------------------

    @Test
    void testRowWin() {
        List<List<Integer>> grid = toGrid(validCard);
        List<Integer> firstRow = grid.get(0);
        assertTrue(verifier.verifyAnyRow(validCard, new HashSet<>(firstRow)));
    }

    @Test
    void testColumnWin() {
        List<List<Integer>> grid = toGrid(validCard);
        List<Integer> firstCol = new ArrayList<>();
        for (List<Integer> row : grid) firstCol.add(row.get(0));
        assertTrue(verifier.verifyAnyColumn(validCard, new HashSet<>(firstCol)));
    }

    @Test
    void testMainDiagonalWin() {
        List<List<Integer>> grid = toGrid(validCard);
        List<Integer> diag = new ArrayList<>();
        for (int i = 0; i < 5; i++) diag.add(grid.get(i).get(i));
        assertTrue(verifier.verifyDiagonal(validCard, new HashSet<>(diag)));
    }

    @Test
    void testAntiDiagonalWin() {
        List<List<Integer>> grid = toGrid(validCard);
        List<Integer> antiDiag = new ArrayList<>();
        for (int i = 0; i < 5; i++) antiDiag.add(grid.get(i).get(4 - i));
        assertTrue(verifier.verifyDiagonal(validCard, new HashSet<>(antiDiag)));
    }

    @Test
    void testFourCornersWin() {
        List<List<Integer>> grid = toGrid(validCard);
        Set<Integer> corners = Set.of(
                grid.get(0).get(0),
                grid.get(0).get(4),
                grid.get(4).get(0),
                grid.get(4).get(4)
        );
        assertTrue(verifier.verifyFourCorners(validCard, corners));
    }

    @Test
    void testFullHouseWin() {
        List<List<Integer>> grid = toGrid(validCard);
        Set<Integer> allNumbers = new HashSet<>();
        for (List<Integer> row : grid) allNumbers.addAll(row);
        assertTrue(verifier.verifyFullHouse(validCard, allNumbers));
    }

    @Test
    void testLineOrFourCorners() {
        List<List<Integer>> grid = toGrid(validCard);
        // Row
        Set<Integer> firstRow = new HashSet<>(grid.get(0));
        assertTrue(verifier.verifyLineOrFourCorners(validCard, firstRow));
        // Corners
        Set<Integer> corners = Set.of(
                grid.get(0).get(0),
                grid.get(0).get(4),
                grid.get(4).get(0),
                grid.get(4).get(4)
        );
        assertTrue(verifier.verifyLineOrFourCorners(validCard, corners));
    }

    // ---------------------------
    // Helper: convert card to grid (updated to match verifier)
    // ---------------------------
    private List<List<Integer>> toGrid(Map<BingoColumn, List<Integer>> card) {
        List<List<Integer>> grid = new ArrayList<>();
        for (int i = 0; i < 5; i++) grid.add(new ArrayList<>());

        List<BingoColumn> orderedColumns = Arrays.asList(BingoColumn.values());
        for (BingoColumn col : orderedColumns) {
            List<Integer> numbers = new ArrayList<>(card.get(col));
            // Insert free space at center if missing
            if (col == BingoColumn.N && !numbers.contains(0)) numbers.add(2, 0);
            while (numbers.size() < 5) numbers.add(null);
            for (int row = 0; row < 5; row++) grid.get(row).add(numbers.get(row));
        }
        grid.get(2).set(2, 0); // ensure center free space
        return grid;
    }
}
