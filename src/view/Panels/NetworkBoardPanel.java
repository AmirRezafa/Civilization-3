package view.Panels;

import model.Building;
import model.BuildingType;
import model.HexUtils;
import model.ResourceType;
import model.TerrainType;
import model.Tile;
import model.TechType;
import model.TownHallLevel;
import model.Unit;
import model.UnitType;
import network.client.NetworkClient;
import network.protocol.AttackRequest;
import network.protocol.BuildingConstructedMessage;
import network.protocol.CombatResultMessage;
import network.protocol.DisasterOccurredMessage;
import network.protocol.GameStateSnapshotMessage;
import network.protocol.MoveUnitRequest;
import network.protocol.ProductionCompletedMessage;
import network.protocol.ProductionStartedMessage;
import network.protocol.UnitMovedMessage;
import network.protocol.UnitRemovedMessage;
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
import java.util.function.Consumer;

public class NetworkBoardPanel extends JPanel {
    private static final int HEX_SIZE = 28;

    private final List<Tile> tiles = new ArrayList<>();
    private final List<ClientUnit> units = new ArrayList<>();
    private final Map<Integer, ClientUnit> unitsById = new HashMap<>();
    private final Map<Building, String> buildingOwners = new HashMap<>();
    private final Map<Building, Integer> buildingIds = new HashMap<>();
    private final Map<Integer, Building> buildingsById = new HashMap<>();

    private int rows;
    private int cols;
    private String myPlayerId;
    private String currentTurnPlayerId;
    private NetworkClient client;
    private Unit selectedUnit;
    private Building selectedBuilding;
    private Consumer<String> combatLogListener;
    private Runnable selectionListener;

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

    public void setCombatLogListener(Consumer<String> combatLogListener) {
        this.combatLogListener = combatLogListener;
    }

    public void setSelectionListener(Runnable selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setCurrentTurnPlayerId(String currentTurnPlayerId) {
        this.currentTurnPlayerId = currentTurnPlayerId;
        repaint();
    }

    public String getMyPlayerId() {
        return myPlayerId;
    }

    public boolean isMyTurn() {
        return myPlayerId != null && myPlayerId.equals(currentTurnPlayerId);
    }

    public Unit getSelectedUnit() {
        return selectedUnit;
    }

    public int getSelectedUnitId() {
        return selectedUnit == null ? -1 : findId(selectedUnit);
    }

    public Building getSelectedBuilding() {
        return selectedBuilding;
    }

    public int getSelectedBuildingId() {
        Integer id = selectedBuilding == null ? null : buildingIds.get(selectedBuilding);
        return id == null ? -1 : id;
    }

    public String getSelectedBuildingOwnerId() {
        return selectedBuilding == null ? null : buildingOwners.get(selectedBuilding);
    }

    public void sendQuiet(Object message) {
        if (client == null || !(message instanceof network.protocol.Message typed)) {
            return;
        }
        try {
            client.send(typed);
        } catch (IOException ignored) {
        }
    }

    public void loadSnapshot(GameStateSnapshotMessage snapshot, String myPlayerId) {
        int previousSelectedUnitId = getSelectedUnitId();
        int previousSelectedBuildingId = getSelectedBuildingId();

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
            if (!entry.visible()) {
                tile.setVisible(false);
            }
            tiles.add(tile);
        }

        buildingOwners.clear();
        buildingIds.clear();
        buildingsById.clear();
        for (GameStateSnapshotMessage.BuildingEntry entry : snapshot.getBuildings()) {
            Building building = new Building(BuildingType.valueOf(entry.buildingType()), entry.col(), entry.row());
            if (entry.producingKind() != null) {
                applyProducingState(building, entry.producingKind(), entry.producingTarget());
            }
            buildingOwners.put(building, entry.ownerId());
            buildingIds.put(building, entry.id());
            buildingsById.put(entry.id(), building);
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

        ClientUnit reselectedUnit = previousSelectedUnitId == -1 ? null : unitsById.get(previousSelectedUnitId);
        selectedUnit = reselectedUnit != null ? reselectedUnit.unit : null;
        selectedBuilding = previousSelectedBuildingId == -1 ? null : buildingsById.get(previousSelectedBuildingId);

        fireSelectionChanged();
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
        fireSelectionChanged();
        repaint();
    }

    public void applyCombatResult(CombatResultMessage result) {
        for (int id : result.getAttackingUnitIds()) {
            ClientUnit clientUnit = unitsById.get(id);
            if (clientUnit != null) {
                clientUnit.unit.setCurrentAP(clientUnit.unit.getCurrentAP() - 1);
            }
        }
        for (int id : result.getKilledUnitIds()) {
            ClientUnit killed = unitsById.remove(id);
            if (killed != null) {
                units.remove(killed);
                if (killed.unit == selectedUnit) {
                    selectedUnit = null;
                }
            }
        }
        if (result.getDestroyedBuildingId() != null) {
            Building destroyed = null;
            for (Map.Entry<Building, Integer> entry : buildingIds.entrySet()) {
                if (entry.getValue().equals(result.getDestroyedBuildingId())) {
                    destroyed = entry.getKey();
                    break;
                }
            }
            if (destroyed != null) {
                buildingOwners.remove(destroyed);
                buildingIds.remove(destroyed);
                for (Tile tile : tiles) {
                    if (tile.getBuilding() == destroyed) {
                        tile.setBuilding(null);
                        break;
                    }
                }
            }
        }
        if (combatLogListener != null) {
            combatLogListener.accept(describeCombat(result));
        }
        fireSelectionChanged();
        repaint();
    }

    private String describeCombat(CombatResultMessage result) {
        if (result.getStructureDamage() > 0 || result.getDestroyedBuildingId() != null) {
            String outcome = result.getDestroyedBuildingId() != null ? " Building destroyed!" : "";
            return "Attack dealt " + result.getStructureDamage() + " damage to the structure." + outcome;
        }
        String outcome = result.getKilledUnitIds().isEmpty() ? "" : " " + result.getKilledUnitIds().size() + " unit(s) died.";
        return "Combat: " + result.getAttackerHits() + " hit(s) dealt, " + result.getDefenderHits() + " hit(s) taken." + outcome;
    }

    public void reset() {
        tiles.clear();
        units.clear();
        unitsById.clear();
        buildingOwners.clear();
        buildingIds.clear();
        buildingsById.clear();
        selectedUnit = null;
        selectedBuilding = null;
        myPlayerId = null;
        currentTurnPlayerId = null;
        rows = 0;
        cols = 0;
        fireSelectionChanged();
        repaint();
    }

    public void applyProductionStarted(ProductionStartedMessage message) {
        Building building = buildingsById.get(message.getBuildingId());
        if (building != null) {
            applyProducingState(building, message.getKind(), message.getTarget());
        }
        fireSelectionChanged();
        repaint();
    }

    public void applyProductionCompleted(ProductionCompletedMessage message) {
        Building building = buildingsById.get(message.getBuildingId());
        if (building != null) {
            building.clearProduction();
        }
        if ("UNIT".equals(message.getKind()) && message.getCreatedUnitId() >= 0) {
            UnitType unitType = safeUnitType(message.getTarget());
            if (unitType != null) {
                Unit unit = new Unit(unitType, message.getCreatedUnitCol(), message.getCreatedUnitRow());
                ClientUnit clientUnit = new ClientUnit(message.getCreatedUnitId(), unit, message.getOwnerId());
                units.add(clientUnit);
                unitsById.put(message.getCreatedUnitId(), clientUnit);
            }
        }
        fireSelectionChanged();
        repaint();
    }

    public void applyBuildingConstructed(BuildingConstructedMessage message) {
        BuildingType type = safeBuildingType(message.getBuildingType());
        if (type == null) {
            return;
        }
        Building building = new Building(type, message.getCol(), message.getRow());
        buildingOwners.put(building, message.getOwnerId());
        buildingIds.put(building, message.getBuildingId());
        buildingsById.put(message.getBuildingId(), building);
        for (Tile tile : tiles) {
            if (tile.getCol() == message.getCol() && tile.getRow() == message.getRow()) {
                tile.setBuilding(building);
                break;
            }
        }
        fireSelectionChanged();
        repaint();
    }

    public void applyUnitRemoved(UnitRemovedMessage message) {
        ClientUnit removed = unitsById.remove(message.getUnitId());
        if (removed != null) {
            units.remove(removed);
            if (removed.unit == selectedUnit) {
                selectedUnit = null;
            }
        }
        fireSelectionChanged();
        repaint();
    }

    public void applyDisaster(DisasterOccurredMessage message) {
        for (int id : message.getKilledUnitIds()) {
            ClientUnit removed = unitsById.remove(id);
            if (removed != null) {
                units.remove(removed);
                if (removed.unit == selectedUnit) {
                    selectedUnit = null;
                }
            }
        }
        if (combatLogListener != null && !message.getKilledUnitIds().isEmpty()) {
            combatLogListener.accept("A disaster struck near (" + message.getCenterCol() + ", " + message.getCenterRow()
                    + "), " + message.getKilledUnitIds().size() + " unit(s) lost.");
        }
        fireSelectionChanged();
        repaint();
    }

    private void applyProducingState(Building building, String kind, String target) {
        switch (kind) {
            case "UNIT" -> {
                UnitType unitType = safeUnitType(target);
                if (unitType != null) {
                    building.startProducing(unitType);
                }
            }
            case "UPGRADE" -> {
                try {
                    building.startUpgrading(TownHallLevel.valueOf(target));
                } catch (IllegalArgumentException ignored) {
                }
            }
            case "TECH" -> {
                try {
                    building.startResearching(TechType.valueOf(target));
                } catch (IllegalArgumentException ignored) {
                }
            }
            default -> {
            }
        }
    }

    private UnitType safeUnitType(String name) {
        try {
            return UnitType.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private BuildingType safeBuildingType(String name) {
        try {
            return BuildingType.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private void fireSelectionChanged() {
        if (selectionListener != null) {
            selectionListener.run();
        }
    }

    private void handleClick(MouseEvent e) {
        Tile clickedTile = getTileAtPixel(e.getX(), e.getY());
        if (clickedTile == null) {
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            ClientUnit clicked = getUnitAt(clickedTile.getCol(), clickedTile.getRow());
            selectedUnit = (clicked != null && myPlayerId != null && myPlayerId.equals(clicked.ownerId)) ? clicked.unit : null;
            selectedBuilding = clickedTile.getBuilding();
            fireSelectionChanged();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            attemptAction(clickedTile);
        }
    }

    private void attemptAction(Tile targetTile) {
        if (selectedUnit == null || client == null) {
            return;
        }
        if (myPlayerId == null || !myPlayerId.equals(currentTurnPlayerId)) {
            return;
        }
        int unitId = findId(selectedUnit);
        if (unitId == -1) {
            return;
        }

        boolean targetHasForeignPresence = hasForeignUnit(targetTile.getCol(), targetTile.getRow())
                || isForeignBuilding(targetTile.getBuilding());
        try {
            if (targetHasForeignPresence) {
                client.send(new AttackRequest(unitId, targetTile.getCol(), targetTile.getRow()));
            } else if (HexUtils.isNeighbor(selectedUnit.getCol(), selectedUnit.getRow(), targetTile.getCol(), targetTile.getRow())) {
                client.send(new MoveUnitRequest(unitId, targetTile.getCol(), targetTile.getRow()));
            }
        } catch (IOException ignored) {
        }
    }

    private boolean hasForeignUnit(int col, int row) {
        for (ClientUnit clientUnit : units) {
            if (clientUnit.unit.getCol() == col && clientUnit.unit.getRow() == row
                    && clientUnit.ownerId != null && !clientUnit.ownerId.equals(myPlayerId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isForeignBuilding(Building building) {
        if (building == null) {
            return false;
        }
        String ownerId = buildingOwners.get(building);
        return ownerId != null && !ownerId.equals(myPlayerId);
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
