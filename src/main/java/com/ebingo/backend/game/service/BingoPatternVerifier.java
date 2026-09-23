

package com.ebingo.backend.game.service;

import com.ebingo.backend.game.enums.BingoColumn;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class BingoPatternVerifier {

    /**
     * Checks if each column's numbers are within the valid Bingo range.
     * Returns false if any number is out of range (excluding free space 0).
     */
    private boolean isValidCard(Map<BingoColumn, List<Integer>> card) {
        Map<BingoColumn, int[]> ranges = Map.of(
                BingoColumn.B, new int[]{1, 15},
                BingoColumn.I, new int[]{16, 30},
                BingoColumn.N, new int[]{31, 45},
                BingoColumn.G, new int[]{46, 60},
                BingoColumn.O, new int[]{61, 75}
        );

        for (Map.Entry<BingoColumn, List<Integer>> entry : card.entrySet()) {
            BingoColumn col = entry.getKey();
            int[] range = ranges.get(col);

            for (Integer num : entry.getValue()) {
                if (num == null || num == 0) continue; // free space allowed
                if (num < range[0] || num > range[1]) return false;
            }
        }
        return true;
    }

    /**
     * Converts a bingo card map into a 5x5 grid.
     * Inserts free space (0) at the center of the N column if missing.
     */
    private List<List<Integer>> toGrid(Map<BingoColumn, List<Integer>> card) {
        List<List<Integer>> grid = new ArrayList<>();
        for (int i = 0; i < 5; i++) grid.add(new ArrayList<>());

        List<BingoColumn> orderedColumns = Arrays.asList(BingoColumn.values()); // B, I, N, G, O

        for (BingoColumn col : orderedColumns) {
            List<Integer> numbers = new ArrayList<>(card.get(col));

            if (col == BingoColumn.N && !numbers.contains(0)) {
                numbers.add(2, 0); // insert free space at center
            }

            while (numbers.size() < 5) numbers.add(null); // pad with nulls

            for (int row = 0; row < 5; row++) {
                grid.get(row).add(numbers.get(row));
            }
        }

        grid.get(2).set(2, 0); // ensure center cell is free space

        return grid;
    }

    /**
     * Checks if all numbers in subset are contained in marked set, considering free space.
     */
    private boolean containsAll(Collection<Integer> subset, Set<Integer> marked) {
        Set<Integer> effectiveMarked = new HashSet<>(marked);
        effectiveMarked.add(0); // free space always counted

        for (Integer n : subset) {
            if (n == null) continue;
            if (!effectiveMarked.contains(n)) return false;
        }
        return true;
    }

    // -------------------------
    // Standard patterns
    // -------------------------

    public boolean verifyFullHouse(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        if (!isValidCard(card)) return false;
        List<List<Integer>> grid = toGrid(card);
        List<Integer> allNumbers = grid.stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return containsAll(allNumbers, marked);
    }

    public boolean verifyAnyRow(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        if (!isValidCard(card)) return false;
        List<List<Integer>> grid = toGrid(card);
        return grid.stream().anyMatch(row -> containsAll(row, marked));
    }

    public boolean verifyAnyColumn(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        if (!isValidCard(card)) return false;
        List<List<Integer>> grid = toGrid(card);
        for (int col = 0; col < 5; col++) {
            final int finalCol = col;
            List<Integer> column = grid.stream()
                    .map(row -> row.get(finalCol))
                    .collect(Collectors.toList());
            if (containsAll(column, marked)) return true;
        }
        return false;
    }

    public boolean verifyDiagonal(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        if (!isValidCard(card)) return false;
        List<List<Integer>> grid = toGrid(card);

        List<Integer> mainDiag = new ArrayList<>();
        List<Integer> antiDiag = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            mainDiag.add(grid.get(i).get(i));
            antiDiag.add(grid.get(i).get(4 - i));
        }

        return containsAll(mainDiag, marked) || containsAll(antiDiag, marked);
    }

    public boolean verifyFourCorners(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        if (!isValidCard(card)) return false;
        List<List<Integer>> grid = toGrid(card);
        List<Integer> corners = Arrays.asList(
                grid.get(0).get(0),
                grid.get(0).get(4),
                grid.get(4).get(0),
                grid.get(4).get(4)
        );
        return containsAll(corners, marked);
    }

    // -------------------------
    // Composite patterns
    // -------------------------

    public boolean verifyLine(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        return verifyAnyRow(card, marked) || verifyAnyColumn(card, marked) || verifyDiagonal(card, marked);
    }

    public boolean verifyLineOrFourCorners(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        return verifyLine(card, marked) || verifyFourCorners(card, marked);
    }


    // ====================================================================

    /**
     * Identifies the FIRST winning pattern (deterministic priority) and returns ONLY the numbers
     * that constitute that winning pattern.
     * <p>
     * Priority order:
     * 1) Four corners
     * 2) Rows (top -> bottom)
     * 3) Columns (B -> O)
     * 4) Diagonals (main -> anti)
     * <p>
     * If no winning pattern exists, returns an empty list.
     * Free space (0) is treated as always marked and may be included in the returned list if part of the pattern.
     */
    public List<Integer> getWinningPatternNumbers(Map<BingoColumn, List<Integer>> card, Set<Integer> marked) {
        if (!isValidCard(card)) return Collections.emptyList();

        List<List<Integer>> grid = toGrid(card);

        // effective marked includes free space
        Set<Integer> effectiveMarked = new HashSet<>(marked);
        effectiveMarked.add(0);

        // 1) Four corners
        List<Integer> corners = Arrays.asList(
                grid.get(0).get(0),
                grid.get(0).get(4),
                grid.get(4).get(0),
                grid.get(4).get(4)
        );
        if (containsAll(corners, marked)) {
            return corners.stream().filter(Objects::nonNull).toList();
        }

        // 2) Rows (top -> bottom)
        for (int r = 0; r < 5; r++) {
            List<Integer> row = grid.get(r);
            if (containsAll(row, marked)) {
                return row.stream().filter(Objects::nonNull).toList();
            }
        }

        // 3) Columns (B -> O; i.e. col 0 -> col 4)
        for (int c = 0; c < 5; c++) {
            List<Integer> column = new ArrayList<>(5);
            for (int r = 0; r < 5; r++) {
                column.add(grid.get(r).get(c));
            }
            if (containsAll(column, marked)) {
                return column.stream().filter(Objects::nonNull).toList();
            }
        }

        // 4) Diagonals (main then anti)
        List<Integer> mainDiag = new ArrayList<>(5);
        List<Integer> antiDiag = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            mainDiag.add(grid.get(i).get(i));
            antiDiag.add(grid.get(i).get(4 - i));
        }

        if (containsAll(mainDiag, marked)) {
            return mainDiag.stream().filter(Objects::nonNull).toList();
        }
        if (containsAll(antiDiag, marked)) {
            return antiDiag.stream().filter(Objects::nonNull).toList();
        }

        return Collections.emptyList();
    }


    /**
     * Returns:
     * - all winning pattern numbers
     * - plus half (floor) of the remaining marked numbers NOT in the winning pattern
     * <p>
     * If no winning pattern exists, returns an empty list.
     */
    public List<Integer> getWinningAndHalfOfOtherMarkedNumbers(
            Map<BingoColumn, List<Integer>> card,
            Set<Integer> marked
    ) {

        List<Integer> winningNumbers = getWinningPatternNumbers(card, marked);

        if (winningNumbers.isEmpty()) {
            return Collections.emptyList(); // no winning pattern
        }

        Set<Integer> winningSet = new HashSet<>(winningNumbers);

        // remove free space if present
        Set<Integer> effectiveMarked = new HashSet<>(marked);
        effectiveMarked.remove(0);

        // get other marked numbers (not part of winning pattern)
        List<Integer> otherMarked = effectiveMarked.stream()
                .filter(n -> !winningSet.contains(n))
                .sorted()
                .toList();

        int half = otherMarked.size() / 2; // floor automatically

        List<Integer> result = new ArrayList<>(winningNumbers);

        // add half of the remaining marked numbers
        result.addAll(otherMarked.subList(0, half));

        return result;
    }


    // ====================================================================


    // -------------------------
    // Pattern selector with debug logging
    // -------------------------

    public boolean verifyPattern(Map<BingoColumn, List<Integer>> card, Set<Integer> marked, String pattern) {

        boolean result = switch (pattern.toUpperCase()) {
            case "FULL_HOUSE" -> verifyFullHouse(card, marked);
            case "ROW" -> verifyAnyRow(card, marked);
            case "COLUMN" -> verifyAnyColumn(card, marked);
            case "DIAGONAL" -> verifyDiagonal(card, marked);
            case "CORNERS" -> verifyFourCorners(card, marked);
            case "LINE" -> verifyLine(card, marked);
            case "LINE_AND_CORNERS" -> verifyLineOrFourCorners(card, marked);
            default -> false; // unknown pattern
        };

        //        System.out.println("=======================RESULT===============================>>>>>: Result " + result);
        return result;
    }
}



