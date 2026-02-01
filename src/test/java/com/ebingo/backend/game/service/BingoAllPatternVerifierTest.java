package com.ebingo.backend.game.service;

import com.ebingo.backend.game.enums.BingoColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BingoAllPatternVerifierTest {

    private BingoPatternVerifier verifier;
    private Map<BingoColumn, List<Integer>> validCard;

    @BeforeEach
    void setUp() {
        verifier = new BingoPatternVerifier();

        // Valid test card (numbers within correct ranges)
        validCard = new LinkedHashMap<>();
        validCard.put(BingoColumn.B, Arrays.asList(1, 2, 3, 4, 5));
        validCard.put(BingoColumn.I, Arrays.asList(16, 17, 18, 19, 20));
        validCard.put(BingoColumn.N, Arrays.asList(31, 32, 33, 34)); // center free space will be inserted automatically
        validCard.put(BingoColumn.G, Arrays.asList(46, 47, 48, 49, 50));
        validCard.put(BingoColumn.O, Arrays.asList(61, 62, 63, 64, 65));
    }

    // Utility to create a marked set
    private Set<Integer> marked(Integer... nums) {
        return new HashSet<>(Arrays.asList(nums));
    }

    private Map<BingoColumn, List<Integer>> copyCard() {
        Map<BingoColumn, List<Integer>> copy = new LinkedHashMap<>();
        validCard.forEach((k, v) -> copy.put(k, new ArrayList<>(v)));
        return copy;
    }

    // ---------------------------
    // Winning pattern tests
    // ---------------------------

    @Test
    void testRowWin() {
        Set<Integer> marked = marked(5, 20, 34, 50, 65); // first row
        assertTrue(verifier.verifyAnyRow(validCard, marked));
        assertTrue(verifier.verifyLine(validCard, marked));
    }

    @Test
    void testColumnWin() {
        Set<Integer> marked = marked(1, 2, 3, 4, 5); // B column
        assertTrue(verifier.verifyAnyColumn(validCard, marked));
        assertTrue(verifier.verifyLine(validCard, marked));
    }

    @Test
    void testMainDiagonalWin() {
        // Main diagonal: B(0,0)=1, I(1,1)=17, N(2,2)=0, G(3,3)=49, O(4,4)=65
        Set<Integer> marked = marked(1, 17, 49, 65);
        assertTrue(verifier.verifyDiagonal(validCard, marked));
    }

    @Test
    void testAntiDiagonalWin() {
        // Anti-diagonal: O(0,4)=61, G(1,3)=49, N(2,2)=0, I(3,1)=17, B(4,0)=5
        Set<Integer> marked = marked(61, 47, 35, 19, 5);
        assertTrue(verifier.verifyDiagonal(validCard, marked));
    }

    @Test
    void testFourCornersWin() {
        Set<Integer> marked = marked(1, 5, 61, 65); // four corners
        assertTrue(verifier.verifyFourCorners(validCard, marked));
    }

    @Test
    void testFullHouseWin() {
        Set<Integer> allNumbers = new HashSet<>();
        validCard.values().forEach(allNumbers::addAll);
        allNumbers.add(0); // include center free space
        allNumbers.remove(null);

        assertTrue(verifier.verifyFullHouse(validCard, allNumbers));
    }

    @Test
    void testLineOrFourCorners() {
        Set<Integer> corners = marked(1, 5, 61, 65);
        assertTrue(verifier.verifyLineOrFourCorners(validCard, corners));

        Set<Integer> row = marked(1, 16, 31, 46, 61);
        assertTrue(verifier.verifyLineOrFourCorners(validCard, row));
    }

    @Test
    void testNoWin() {
        Set<Integer> marked = marked(1, 17, 46); // incomplete marks
        assertFalse(verifier.verifyLine(validCard, marked));
        assertFalse(verifier.verifyFourCorners(validCard, marked));
        assertFalse(verifier.verifyFullHouse(validCard, marked));
        assertFalse(verifier.verifyDiagonal(validCard, marked));
    }

    @Test
    void testPatternSelector() {
        Set<Integer> diagonal = marked(1, 17, 0, 49, 65);
        assertTrue(verifier.verifyPattern(validCard, diagonal, "DIAGONAL"));

        Set<Integer> row = marked(1, 16, 31, 46, 61);
        assertTrue(verifier.verifyPattern(validCard, row, "ROW"));

        Set<Integer> column = marked(1, 2, 3, 4, 5);
        assertTrue(verifier.verifyPattern(validCard, column, "COLUMN"));

        Set<Integer> corners = marked(1, 5, 61, 65);
        assertTrue(verifier.verifyPattern(validCard, corners, "CORNERS"));
    }

    // ---------------------------
    // Invalid card tests (numbers out of column range)
    // ---------------------------

    @Test
    void testInvalidBColumn() {
        Map<BingoColumn, List<Integer>> card = copyCard();
        card.put(BingoColumn.B, Arrays.asList(1, 2, 16, 4, 5)); // 16 invalid
        Set<Integer> marked = marked(1, 2, 16, 4, 5);
        assertFalse(verifier.verifyAnyRow(card, marked));
        assertFalse(verifier.verifyAnyColumn(card, marked));
        assertFalse(verifier.verifyDiagonal(card, marked));
        assertFalse(verifier.verifyFullHouse(card, marked));
        assertFalse(verifier.verifyFourCorners(card, marked));
    }

    @Test
    void testInvalidIColumn() {
        Map<BingoColumn, List<Integer>> card = copyCard();
        card.put(BingoColumn.I, Arrays.asList(16, 17, 31, 19, 20)); // 31 invalid
        Set<Integer> marked = marked(16, 17, 31, 19, 20);
        assertFalse(verifier.verifyAnyRow(card, marked));
        assertFalse(verifier.verifyAnyColumn(card, marked));
        assertFalse(verifier.verifyDiagonal(card, marked));
    }

    @Test
    void testInvalidPatternSelector() {
        Set<Integer> marked = marked(1, 16, 31, 46, 61);
        assertFalse(verifier.verifyPattern(validCard, marked, "UNKNOWN_PATTERN"));
    }
}
