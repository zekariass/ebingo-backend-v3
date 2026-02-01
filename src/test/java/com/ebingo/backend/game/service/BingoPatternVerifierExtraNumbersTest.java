//package com.ebingo.backend.game.service;
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
//class BingoPatternVerifierExtraNumbersTest {
//
//    @Autowired
//    private BingoPatternVerifier verifier;
//
//    private Map<BingoColumn, List<Integer>> sampleCard() {
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
//    void returnsCornersPlusHalfOfOtherMarkedNumbers_whenCornersWin() {
//        var card = sampleCard();
//
//        // Winning: corners [1,61,5,65]
//        // Other marked (not in corners): [17,32,48,49] -> size 4 -> half = 2 -> adds [17,32]
//        Set<Integer> marked = Set.of(1, 61, 5, 65, 17, 32, 48, 49);
//
//        List<Integer> result = verifier.getWinningAndHalfOfOtherMarkedNumbers(card, marked);
//
//        System.out.println("Result: " + result);
//
//        assertThat(result).containsExactly(1, 61, 5, 65, 17, 32);
//    }
//
//    @Test
//    void returnsWinningRowPlusHalfOfOtherMarkedNumbers() {
//        var card = sampleCard();
//
//        // Winning row: [1,16,31,46,61]
//        // Other marked: [2,3,4,5] -> size 4 -> half = 2 -> adds [2,3]
//        Set<Integer> marked = Set.of(1, 16, 31, 46, 61, 2, 3, 4, 5);
//
//        List<Integer> result = verifier.getWinningAndHalfOfOtherMarkedNumbers(card, marked);
//
//        System.out.println("Result: " + result);
//
//
//        assertThat(result).containsExactly(1, 16, 31, 46, 61, 2, 3);
//    }
//
//    @Test
//    void floorsWhenHalfIsDecimal() {
//        var card = sampleCard();
//
//        // Winning row: [1,16,31,46,61]
//        // Other marked: [2,3,4] -> size 3 -> half = 1 -> adds [2]
//        Set<Integer> marked = Set.of(1, 16, 31, 46, 61, 2, 3, 4);
//
//        List<Integer> result = verifier.getWinningAndHalfOfOtherMarkedNumbers(card, marked);
//
//        assertThat(result).containsExactly(1, 16, 31, 46, 61, 2);
//    }
//
//    @Test
//    void returnsBColumnPlusHalfOfOtherMarkedNumbers_whenColumnWins() {
//        var card = sampleCard();
//
//        // Winning column B: [1,2,3,4,5]
//        // Other marked: [16,17] -> size 2 -> half = 1 -> adds [16]
//        Set<Integer> marked = Set.of(1, 2, 3, 4, 5, 16, 17);
//
//        List<Integer> result = verifier.getWinningAndHalfOfOtherMarkedNumbers(card, marked);
//
//        assertThat(result).containsExactly(1, 2, 3, 4, 5, 16);
//    }
//
//    @Test
//    void returnsDiagonalPlusHalfOfOtherMarkedNumbers_whenDiagonalWins_includingFreeSpace() {
//        var card = sampleCard();
//
//        // Winning main diagonal: [1,17,0,49,65]
//        // Choose "other marked" that does NOT accidentally complete a row/column/corners
//        // Other marked: [20,34,50] -> size 3 -> half = 1 -> adds [20]
//        Set<Integer> marked = Set.of(1, 17, 49, 65, 20, 34, 50);
//
//        List<Integer> result = verifier.getWinningAndHalfOfOtherMarkedNumbers(card, marked);
//
//        assertThat(result).containsExactly(1, 17, 0, 49, 65, 20);
//    }
//
//    @Test
//    void returnsEmpty_whenNoWinningPatternExists() {
//        var card = sampleCard();
//
//        Set<Integer> marked = Set.of(1, 17, 33);
//
//        List<Integer> result = verifier.getWinningAndHalfOfOtherMarkedNumbers(card, marked);
//
//        assertThat(result).isEmpty();
//    }
//}
