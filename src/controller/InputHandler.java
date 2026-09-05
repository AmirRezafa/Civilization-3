package controller;

import controller.events.EventBus;
import controller.events.UnitActionsChangedEvent;
import model.EdgeFeature;
import model.HexUtils;
import model.Season;
import model.TechType;
import model.TerrainType;
import model.Tile;
import model.Unit;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class InputHandler extends MouseAdapter {
    private final GameController gc;

    public InputHandler(GameController gc) {
        this.gc = gc;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        handleMouseClick(e);
    }

    private Unit getUnitAt(int col, int row) {
        for (Unit u : gc.getUnits()) {
            if (u.isAssigned()) continue;
            if (u.getCol() == col && u.getRow() == row) return u;
        }
        return null;
    }

    private Tile getTileAtPixel(int pixelX, int pixelY) {
        int worldX = pixelX + gc.getXOffset();
        int worldY = pixelY + gc.getYOffset();
        int a = gc.getA();

        Tile closestTile = null;
        double minDistance = Double.MAX_VALUE;

        for (Tile tile : gc.getTiles()) {
            double x = HexUtils.centerX(tile.getCol()) * a;
            double y = HexUtils.centerY(tile.getCol(), tile.getRow()) * a;

            double distance = Math.pow(worldX - x, 2) + Math.pow(worldY - y, 2);
            if (distance < minDistance) {
                minDistance = distance;
                closestTile = tile;
            }
        }

        if (minDistance <= a * a * 1.5) return closestTile;
        return null;
    }

    private void handleMouseClick(MouseEvent e) {
        Tile clickedTile = getTileAtPixel(e.getX(), e.getY());
        if (clickedTile == null) return;

        if (SwingUtilities.isLeftMouseButton(e)) {
            Unit unitOnTile = getUnitAt(clickedTile.getCol(), clickedTile.getRow());
            if (unitOnTile != null) {
                gc.setSelectedUnit(unitOnTile);
                gc.setTileUnderUnit(clickedTile);
            } else {
                gc.setSelectedUnit(null);
                gc.setTileUnderUnit(clickedTile);
            }
            EventBus.publish(new UnitActionsChangedEvent());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            Unit selectedUnit = gc.getSelectedUnit();
            if (selectedUnit != null) {
                boolean isAdjacent = HexUtils.isNeighbor(selectedUnit.getCol(), selectedUnit.getRow(),
                        clickedTile.getCol(), clickedTile.getRow());

                if (gc.isPendingAttack()) {
                    boolean isRangedEligible = !isAdjacent && gc.hasArcherAvailable() &&
                            HexUtils.isDistanceTwo(selectedUnit.getCol(), selectedUnit.getRow(),
                                    clickedTile.getCol(), clickedTile.getRow(), gc.getTiles());
                    if (isAdjacent || isRangedEligible) {
                        if (isAdjacent && gc.hasWallToward(clickedTile.getCol(), clickedTile.getRow()) &&
                                gc.hasDefendersAt(clickedTile.getCol(), clickedTile.getRow())) {
                            Object[] options = {"Attack the wall", "Attack through (defender +2 per die)", "Cancel"};
                            int choice = JOptionPane.showOptionDialog(null,
                                    "A wall stands between you and the defenders.",
                                    "Wall in the way", JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                            if (choice != 0 && choice != 1) return;
                            if (gc.attackHex(clickedTile.getCol(), clickedTile.getRow(), choice == 0)) {
                                EventBus.publish(new UnitActionsChangedEvent());
                            }
                            return;
                        }
                        if (gc.attackHex(clickedTile.getCol(), clickedTile.getRow())) {
                            EventBus.publish(new UnitActionsChangedEvent());
                        }
                    }
                    return;
                }

                if (gc.isPendingBuildingDeconstruct()) {
                    boolean sameHex = selectedUnit.getCol() == clickedTile.getCol() && selectedUnit.getRow() == clickedTile.getRow();
                    if ((sameHex || isAdjacent) && gc.canDeconstructBuildingAt(clickedTile.getCol(), clickedTile.getRow())) {
                        String buildingName = clickedTile.getBuilding().getType().getDisplayName();
                        int confirm = JOptionPane.showConfirmDialog(null,
                                "Deconstruct " + buildingName + "? This cannot be undone.",
                                "Confirm Deconstruction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION &&
                                gc.deconstructBuildingAt(clickedTile.getCol(), clickedTile.getRow())) {
                            EventBus.publish(new UnitActionsChangedEvent());
                        }
                    } else {
                        gc.cancelPendingBuildingDeconstruct();
                    }
                    return;
                }

                if (isAdjacent) {
                    EdgeFeature pending = gc.getPendingEdgeBuild();
                    if (pending != null) {
                        if (gc.buildEdgeFeature(selectedUnit.getCol(), selectedUnit.getRow(),
                                clickedTile.getCol(), clickedTile.getRow(), pending)) {
                            EventBus.publish(new UnitActionsChangedEvent());
                        }
                        return;
                    }

                    if (gc.isPendingEdgeDeconstruct()) {
                        EdgeFeature existing = gc.getEdgeFeature(selectedUnit.getCol(), selectedUnit.getRow(),
                                clickedTile.getCol(), clickedTile.getRow());
                        if (existing == EdgeFeature.ROAD || existing == EdgeFeature.WALL) {
                            int confirm = JOptionPane.showConfirmDialog(null,
                                    "Destroy this " + existing + "? This cannot be undone.",
                                    "Confirm Deconstruction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                            if (confirm == JOptionPane.YES_OPTION &&
                                    gc.deconstructEdge(selectedUnit.getCol(), selectedUnit.getRow(),
                                            clickedTile.getCol(), clickedTile.getRow())) {
                                EventBus.publish(new UnitActionsChangedEvent());
                            }
                        }
                        return;
                    }

                    if (!gc.canStackAt(clickedTile.getCol(), clickedTile.getRow(), selectedUnit.getType())) {
                        return;
                    }

                    TerrainType targetTerrain = clickedTile.getTerrain();
                    if (!targetTerrain.isPassable()) {
                        return;
                    }
                    if (targetTerrain == TerrainType.SEA && !gc.hasTech(TechType.SAILING)) {
                        return;
                    }

                    EdgeFeature edge = gc.getEdgeFeature(selectedUnit.getCol(), selectedUnit.getRow(),
                            clickedTile.getCol(), clickedTile.getRow());
                    boolean hasRiver = gc.hasRiverEdge(selectedUnit.getCol(), selectedUnit.getRow(),
                            clickedTile.getCol(), clickedTile.getRow());

                    Season season = gc.getCurrentSeason();
                    int movementCost;
                    if (targetTerrain == TerrainType.SEA) {
                        movementCost = targetTerrain.getMovementCost() + season.getWaterMovementPenalty();
                    } else if (edge == EdgeFeature.ROAD) {
                        movementCost = 1;
                    } else {
                        movementCost = targetTerrain.getMovementCost();
                        if (hasRiver) movementCost += 2;
                        movementCost += season.getLandMovementPenalty();
                    }

                    if(selectedUnit.move(clickedTile.getCol(), clickedTile.getRow(), movementCost)){
                        if (targetTerrain == TerrainType.SEA) {
                            selectedUnit.setCurrentAP(0);
                        }
                        gc.setTileUnderUnit(clickedTile);
                        gc.updateFog();
                        EventBus.publish(new UnitActionsChangedEvent());
                    }
                }
            }
        }
    }
}
