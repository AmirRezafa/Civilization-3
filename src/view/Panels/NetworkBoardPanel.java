package view.Panels;

import model.Building;
import model.BuildingType;
import model.HexUtils;
import model.ResourceType;
import model.TerrainType;
import model.Tile;
import model.Unit;
import model.UnitType;
import network.client.NetworkClient;
import network.protocol.GameStateSnapshotMessage;
import network.protocol.MoveUnitRequest;
import network.protocol.UnitMovedMessage;
import view.components.BuildingView;
import view.components.TileView;
import view.components.UnitView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkBoardPanel extends JPanel {
    private static final int HEX_SIZE = 28;

    private final List<Tile> tiles = new ArrayList<>();
    private final List<ClientUnit> units = new ArrayList<>();
    private final Map<Integer, ClientUnit> unitsById = new HashMap<>();

    private int rows;
    private int cols;
    private String myPlayerId;
    private String currentTurnPlayerId;
    private NetworkClient client;
    private Unit selectedUnit;

    public NetworkBoardPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e);
            }
        });
    }

    public void setClient(NetworkClient client) {
        this.client = client;
    }

    public void setCurrentTurnPlayerId(String currentTurnPlayerId) {
        this.currentTurnPlayerId = currentTurnPlayerId;
        repaint();
    }

    public void loadSnapshot(GameStateSnapshotMessage snapshot, String myPlayerId) {
        this.myPlayerId = myPlayerId;
        this.rows = snapshot.getRows();
        this.cols = snapshot.getCols();

        tiles.clear();
        for (GameStateSnapshotMessage.TileEntry entry : snapshot.getTiles()) {
            Map<ResourceType, Integer> resources = new HashMap<>();
            for (Map.Entry<String, Integer> resourceEntry : entry.resources().entrySet()) {
                resources.put(ResourceType.valueOf(resourceEntry.getKey()), resourceEntry.getValue());
            }
            Tile tile = new Tile(entry.col(), entry.row(), TerrainType.valueOf(entry.terrain()), resources);
            tile.setVisible(true);
            tiles.add(tile);
        }

        for (GameStateSnapshotMessage.BuildingEntry entry : snapshot.getBuildings()) {
            Building building = new Building(BuildingType.valueOf(entry.buildingType()), entry.col(), entry.row());
            for (Tile tile : tiles) {
                if (tile.getCol() == entry.col() && tile.getRow() == entry.row()) {
                    tile.setBuilding(building);
                    break;
                }
            }
        }

        units.clear();
        unitsById.clear();
        for (GameStateSnapshotMessage.UnitEntry entry : snapshot.getUnits()) {
            Unit unit = new Unit(UnitType.valueOf(entry.unitType()), entry.col(), entry.row());
            unit.setCurrentAP(entry.currentAP());
            ClientUnit clientUnit = new ClientUnit(entry.id(), unit, entry.ownerId());
            units.add(clientUnit);
            unitsById.put(entry.id(), clientUnit);
        }

        selectedUnit = null;
        revalidate();
        repaint();
    }

    public void applyUnitMoved(UnitMovedMessage moved) {
        ClientUnit clientUnit = unitsById.get(moved.getUnitId());
        if (clientUnit == null) {
            return;
        }
        clientUnit.unit.placeAt(moved.getNewCol(), moved.getNewRow());
        clientUnit.unit.setCurrentAP(moved.getRemainingAP());
        repaint();
    }

    public void reset() {
        tiles.clear();
        units.clear();
        unitsById.clear();
        selectedUnit = null;
        myPlayerId = null;
        currentTurnPlayerId = null;
        rows = 0;
        cols = 0;
        repaint();
    }

    private void handleClick(MouseEvent e) {
        Tile clickedTile = getTileAtPixel(e.getX(), e.getY());
        if (clickedTile == null) {
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            ClientUnit clicked = getUnitAt(clickedTile.getCol(), clickedTile.getRow());
            selectedUnit = (clicked != null && myPlayerId != null && myPlayerId.equals(clicked.ownerId)) ? clicked.unit : null;
            repaint();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            attemptMove(clickedTile);
        }
    }

    private void attemptMove(Tile targetTile) {
        if (selectedUnit == null || client == null) {
            return;
        }
        if (myPlayerId == null || !myPlayerId.equals(currentTurnPlayerId)) {
            return;
        }
        if (!HexUtils.isNeighbor(selectedUnit.getCol(), selectedUnit.getRow(), targetTile.getCol(), targetTile.getRow())) {
            return;
        }
        int unitId = findId(selectedUnit);
        if (unitId == -1) {
            return;
        }
        try {
            client.send(new MoveUnitRequest(unitId, targetTile.getCol(), targetTile.getRow()));
        } catch (IOException ignored) {
        }
    }

    private int findId(Unit unit) {
        for (ClientUnit clientUnit : units) {
            if (clientUnit.unit == unit) {
                return clientUnit.id;
            }
        }
        return -1;
    }

    private ClientUnit getUnitAt(int col, int row) {
        for (ClientUnit clientUnit : units) {
            if (clientUnit.unit.getCol() == col && clientUnit.unit.getRow() == row) {
                return clientUnit;
            }
        }
        return null;
    }

    private Tile getTileAtPixel(int pixelX, int pixelY) {
        Tile closestTile = null;
        double minDistance = Double.MAX_VALUE;
        for (Tile tile : tiles) {
            double x = HexUtils.centerX(tile.getCol()) * HEX_SIZE;
            double y = HexUtils.centerY(tile.getCol(), tile.getRow()) * HEX_SIZE;
            double distance = Math.pow(pixelX - x, 2) + Math.pow(pixelY - y, 2);
            if (distance < minDistance) {
                minDistance = distance;
                closestTile = tile;
            }
        }
        if (minDistance <= HEX_SIZE * HEX_SIZE * 1.5) {
            return closestTile;
        }
        return null;
    }

    @Override
    public Dimension getPreferredSize() {
        if (rows == 0 || cols == 0) {
            return super.getPreferredSize();
        }
        int width = (int) (HexUtils.centerX(cols - 1) * HEX_SIZE + HEX_SIZE * 2);
        int height = (int) (HexUtils.centerY(cols - 1, rows - 1) * HEX_SIZE + HEX_SIZE * 2);
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Tile tile : tiles) {
            double x = HexUtils.centerX(tile.getCol()) * HEX_SIZE;
            double y = HexUtils.centerY(tile.getCol(), tile.getRow()) * HEX_SIZE;
            TileView.show(x, y, HEX_SIZE, g2, getTerrainColor(tile.getTerrain()), tile);
            if (tile.getBuilding() != null) {
                BuildingView.show(tile.getBuilding(), x, y, HEX_SIZE, g2);
            }
        }

        for (ClientUnit clientUnit : units) {
            UnitView.show(clientUnit.unit, HEX_SIZE, clientUnit.unit == selectedUnit, g2);
        }

        g2.dispose();
    }

    private Color getTerrainColor(TerrainType type) {
        return switch (type) {
            case PLAIN -> new Color(180, 200, 100);
            case FOREST -> new Color(34, 139, 34);
            case MOUNTAIN -> new Color(128, 128, 128);
            case MEADOW -> new Color(144, 238, 144);
            case SEA -> new Color(65, 105, 225);
            case MOUNTAIN_RANGE -> new Color(90, 90, 90);
        };
    }

    private static class ClientUnit {
        final int id;
        final Unit unit;
        final String ownerId;

        ClientUnit(int id, Unit unit, String ownerId) {
            this.id = id;
            this.unit = unit;
            this.ownerId = ownerId;
        }
    }
}
