package controller.services;

import model.Building;
import model.Tile;
import model.Unit;

import java.util.List;

public class FogOfWarService {
    private final int rows, cols;
    private final Tile[][] tileGrid;
    private final List<Tile> tiles;
    private final List<Unit> units;
    private final List<Building> buildings;

    public FogOfWarService(int rows, int cols, Tile[][] tileGrid, List<Tile> tiles,
                            List<Unit> units, List<Building> buildings) {
        this.rows = rows;
        this.cols = cols;
        this.tileGrid = tileGrid;
        this.tiles = tiles;
        this.units = units;
        this.buildings = buildings;
    }

    private void revealArea(int centerCol, int centerRow, int radius) {
        int startCol = Math.max(0, centerCol - radius);
        int endCol = Math.min(cols - 1, centerCol + radius);
        int startRow = Math.max(0, centerRow - radius);
        int endRow = Math.min(rows - 1, centerRow + radius);

        int radiust2 = radius * radius;

        for (int col = startCol; col <= endCol; col++) {
            for (int row = startRow; row <= endRow; row++) {
                int dx = col - centerCol;
                int dy = row - centerRow;

                if ((dx * dx) + (dy * dy) <= radiust2)
                    tileGrid[col][row].setVisible(true);
            }
        }
    }

    public void updateFog() {
        for (Tile tile : tiles)
            tile.setVisible(false);

        for (Unit unit : units) {
            if (unit.getOwner() != null) continue;
            revealArea(unit.getCol(), unit.getRow(), unit.getType().getVisionRadius());
        }

        for (Building building : buildings)
            revealArea(building.getCol(), building.getRow(), building.getType().getVisionRadius());
    }
}
