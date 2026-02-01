//package com.ebingo.backend.game.service;
//
//
//import com.ebingo.backend.game.enums.BingoColumn;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@ExtendWith(SpringExtension.class)
//@Import(BingoPatternVerifier.class)
//class BingoPatternVerifierTest {
//
//    @Autowired
//    private BingoPatternVerifier verifier;
//
//    private Map<BingoColumn, List<Integer>> sampleCard() {
//        // A valid card within Bingo ranges
//        // Grid:
//        // [ 1, 16, 31, 46, 61 ]
//        // [ 2, 17, 32, 47, 62 ]
//        // [ 3, 18,  0, 48, 63 ]  (free space)
//        // [ 4, 19, 34, 49, 64 ]
//        // [ 5, 20, 35, 50, 65 ]
//        Map<BingoColumn, List<Integer>> card = new EnumMap<>(BingoColumn.class);
//        card.put(BingoColumn.B, Arrays.asList(1, 2, 3, 4, 5));
//        card.put(BingoColumn.I, Arrays.asList(16, 17, 18, 19, 20));
//        card.put(BingoColumn.N, Arrays.asList(31, 32, 0, 34, 35)); // include free space
//        card.put(BingoColumn.G, Arrays.asList(46, 47, 48, 49, 50));
//        card.put(BingoColumn.O, Arrays.asList(61, 62, 63, 64, 65));
//        return card;
//    }
//
//    @Test
//    void returnsFourCorners_whenCornersWin() {
//        var card = sampleCard();
//        Set<Integer> marked = Set.of(1, 61, 5, 65, 17, 32); // extra marks don't matter
//
//        List<Integer> win = verifier.getWinningPatternNumbers(card, marked);
//
//        assertThat(win).containsExactly(1, 61, 5, 65);
//    }
//
//    @Test
//    void returnsFirstRow_whenFirstRowWins() {
//        var card = sampleCard();
//        // Mark the entire first row: [1,16,31,46,61]
//        // Avoid bottom corners (5,65) so corners pattern does NOT trigger.
//        Set<Integer> marked = Set.of(1, 16, 31, 46, 61, 17, 32);
//
//        List<Integer> win = verifier.getWinningPatternNumbers(card, marked);
//
//        assertThat(win).containsExactly(1, 16, 31, 46, 61);
//    }
//
//    @Test
//    void returnsBColumn_whenBColumnWins() {
//        var card = sampleCard();
//        Set<Integer> marked = Set.of(1, 2, 3, 4, 5, 17, 32); // extra marks don't matter
//
//        List<Integer> win = verifier.getWinningPatternNumbers(card, marked);
//
//        assertThat(win).containsExactly(1, 2, 3, 4, 5);
//    }
//
//    @Test
//    void returnsMainDiagonal_whenMainDiagonalWins_includingFreeSpace() {
//        var card = sampleCard();
//        // Main diagonal is [1,17,0,49,65]. We don't need to mark 0.
//        Set<Integer> marked = Set.of(1, 17, 49, 65, 32, 48); // extra marks don't matter
//
//        List<Integer> win = verifier.getWinningPatternNumbers(card, marked);
//
//        assertThat(win).containsExactly(1, 17, 0, 49, 65);
//    }
//
//    @Test
//    void prefersCorners_overRow_whenBothAreWinning() {
//        var card = sampleCard();
//        // This marks corners AND the first row (because corners include 1 and 61).
//        Set<Integer> marked = Set.of(1, 16, 31, 46, 61, 5, 65);
//
//        List<Integer> win = verifier.getWinningPatternNumbers(card, marked);
//
//        // Priority in the method: Corners first
//        assertThat(win).containsExactly(1, 61, 5, 65);
//    }
//
//    @Test
//    void returnsEmpty_whenNoPatternWins() {
//        var card = sampleCard();
//        Set<Integer> marked = Set.of(1, 17); // not enough to complete any pattern
//
//        List<Integer> win = verifier.getWinningPatternNumbers(card, marked);
//
//        assertThat(win).isEmpty();
//    }
//}
