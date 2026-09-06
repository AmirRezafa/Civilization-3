package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameStateSnapshotMessage extends Message {
    private final int rows;
    private final int cols;
    private final List<TileEntry> tiles;
    private final List<UnitEntry> units;
    private final List<BuildingEntry> buildings;

    public GameStateSnapshotMessage(int rows, int cols, List<TileEntry> tiles, List<UnitEntry> units, List<BuildingEntry> buildings) {
        super(MessageType.GAME_STATE_SNAPSHOT);
        this.rows = rows;
        this.cols = cols;
        this.tiles = tiles;
        this.units = units;
        this.buildings = buildings;
    }

    private GameStateSnapshotMessage(int rows, int cols, List<TileEntry> tiles, List<UnitEntry> units, List<BuildingEntry> buildings, long timestamp) {
        super(MessageType.GAME_STATE_SNAPSHOT, timestamp);
        this.rows = rows;
        this.cols = cols;
        this.tiles = tiles;
        this.units = units;
        this.buildings = buildings;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public List<TileEntry> getTiles() {
        return tiles;
    }

    public List<UnitEntry> getUnits() {
        return units;
    }

    public List<BuildingEntry> getBuildings() {
        return buildings;
    }

    @Override
    public Map<String, Object> toPayload() {
        List<Object> encodedTiles = new ArrayList<>();
        for (TileEntry tile : tiles) {
            Map<String, Object> tileMap = new LinkedHashMap<>();
            tileMap.put("col", tile.col());
            tileMap.put("row", tile.row());
            tileMap.put("terrain", tile.terrain());
            Map<String, Object> resourceMap = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : tile.resources().entrySet()) {
                resourceMap.put(entry.getKey(), entry.getValue());
            }
            tileMap.put("resources", resourceMap);
            encodedTiles.add(tileMap);
        }

        List<Object> encodedUnits = new ArrayList<>();
        for (UnitEntry unit : units) {
            Map<String, Object> unitMap = new LinkedHashMap<>();
            unitMap.put("id", unit.id());
            unitMap.put("ownerId", unit.ownerId());
            unitMap.put("unitType", unit.unitType());
            unitMap.put("col", unit.col());
            unitMap.put("row", unit.row());
            unitMap.put("currentAP", unit.currentAP());
            encodedUnits.add(unitMap);
        }

        List<Object> encodedBuildings = new ArrayList<>();
        for (BuildingEntry building : buildings) {
            Map<String, Object> buildingMap = new LinkedHashMap<>();
            buildingMap.put("id", building.id());
            buildingMap.put("ownerId", building.ownerId());
            buildingMap.put("buildingType", building.buildingType());
            buildingMap.put("col", building.col());
            buildingMap.put("row", building.row());
            encodedBuildings.add(buildingMap);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rows", rows);
        payload.put("cols", cols);
        payload.put("tiles", encodedTiles);
        payload.put("units", encodedUnits);
        payload.put("buildings", encodedBuildings);
        return payload;
    }

    @SuppressWarnings("unchecked")
    static GameStateSnapshotMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int rows = ((Number) payload.get("rows")).intValue();
        int cols = ((Number) payload.get("cols")).intValue();

        List<TileEntry> tiles = new ArrayList<>();
        Object rawTiles = payload.get("tiles");
        if (rawTiles instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    int col = ((Number) map.get("col")).intValue();
                    int row = ((Number) map.get("row")).intValue();
                    String terrain = (String) map.get("terrain");
                    Map<String, Integer> resources = new LinkedHashMap<>();
                    Object rawResources = map.get("resources");
                    if (rawResources instanceof Map<?, ?> resourceMap) {
                        for (Map.Entry<?, ?> entry : resourceMap.entrySet()) {
                            resources.put((String) entry.getKey(), ((Number) entry.getValue()).intValue());
                        }
                    }
                    tiles.add(new TileEntry(col, row, terrain, resources));
                }
            }
        }

        List<UnitEntry> units = new ArrayList<>();
        Object rawUnits = payload.get("units");
        if (rawUnits instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    int id = ((Number) map.get("id")).intValue();
                    String ownerId = (String) map.get("ownerId");
                    String unitType = (String) map.get("unitType");
                    int col = ((Number) map.get("col")).intValue();
                    int row = ((Number) map.get("row")).intValue();
                    int currentAP = ((Number) map.get("currentAP")).intValue();
                    units.add(new UnitEntry(id, ownerId, unitType, col, row, currentAP));
                }
            }
        }

        List<BuildingEntry> buildings = new ArrayList<>();
        Object rawBuildings = payload.get("buildings");
        if (rawBuildings instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    int id = ((Number) map.get("id")).intValue();
                    String ownerId = (String) map.get("ownerId");
                    String buildingType = (String) map.get("buildingType");
                    int col = ((Number) map.get("col")).intValue();
                    int row = ((Number) map.get("row")).intValue();
                    buildings.add(new BuildingEntry(id, ownerId, buildingType, col, row));
                }
            }
        }

        return new GameStateSnapshotMessage(rows, cols, tiles, units, buildings, timestamp);
    }

    public record TileEntry(int col, int row, String terrain, Map<String, Integer> resources) {
    }

    public record UnitEntry(int id, String ownerId, String unitType, int col, int row, int currentAP) {
    }

    public record BuildingEntry(int id, String ownerId, String buildingType, int col, int row) {
    }
}
