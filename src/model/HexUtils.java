package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class HexUtils {
    public static double centerX(int col) {
        return (col + 1) * 1.5;
    }

    public static double centerY(int col, int row) {
        return (row + 1) * Math.sqrt(3) +
                (col % 2 == 0 ? Math.sqrt(3) / 2 : 0);
    }

    public static boolean isNeighbor(int col1, int row1, int col2, int row2) {
        if (col1 == col2 && Math.abs(row1 - row2) == 1) return true;
        if (Math.abs(col1 - col2) == 1) {
            if (col1 % 2 == 0) {
                return (row2 == row1 || row2 == row1 + 1);
            } else {
                return (row2 == row1 || row2 == row1 - 1);
            }
        }
        return false;
    }

    public static boolean isDistanceTwo(int col1, int row1, int col2, int row2, List<Tile> allTiles) {
        if (isNeighbor(col1, row1, col2, row2)) return false;

        for (Tile mid : allTiles) {
            if (isNeighbor(col1, row1, mid.getCol(), mid.getRow()) &&
                    isNeighbor(mid.getCol(), mid.getRow(), col2, row2)) {
                return true;
            }
        }
        return false;
    }

    public static List<Tile> hexesWithinRadius(int centerCol, int centerRow, int radius, List<Tile> allTiles) {
        List<Tile> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<int[]> frontier = new ArrayDeque<>();
        visited.add(edgeKey(centerCol, centerRow));

        for (Tile t : allTiles) {
            if (t.getCol() == centerCol && t.getRow() == centerRow) {
                result.add(t);
                break;
            }
        }
        frontier.add(new int[]{centerCol, centerRow, 0});

        while (!frontier.isEmpty()) {
            int[] cur = frontier.poll();
            if (cur[2] >= radius) continue;

            for (Tile t : allTiles) {
                long k = edgeKey(t.getCol(), t.getRow());
                if (visited.contains(k)) continue;
                if (!isNeighbor(cur[0], cur[1], t.getCol(), t.getRow())) continue;

                visited.add(k);
                result.add(t);
                frontier.add(new int[]{t.getCol(), t.getRow(), cur[2] + 1});
            }
        }
        return result;
    }

    private static long edgeKey(int col, int row) {
        return ((long) col << 32) | (row & 0xffffffffL);
    }
}
