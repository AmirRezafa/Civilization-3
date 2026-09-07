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
    private final Map<String, Integer> yourResources;
    private final List<String> yourTechs;
    private final Map<String, Integer> yourItems;

    public GameStateSnapshotMessage(int rows, int cols, List<TileEntry> tiles, List<UnitEntry> units,
                                     List<BuildingEntry> buildings, Map<String, Integer> yourResources,
                                     List<String> yourTechs, Map<String, Integer> yourItems) {
        super(MessageType.GAME_STATE_SNAPSHOT);
        this.rows = rows;
        this.cols = cols;
        this.tiles = tiles;
        this.units = units;
        this.buildings = buildings;
        this.yourResources = yourResources;
        this.yourTechs = yourTechs;
        this.yourItems = yourItems;
    }

    private GameStateSnapshotMessage(int rows, int cols, List<TileEntry> tiles, List<UnitEntry> units,
                                      List<BuildingEntry> buildings, Map<String, Integer> yourResources,
                                      List<String> yourTechs, Map<String, Integer> yourItems, long timestamp) {
        super(MessageType.GAME_STATE_SNAPSHOT, timestamp);
        this.rows = rows;
        this.cols = cols;
        this.tiles = tiles;
        this.units = units;
        this.buildings = buildings;
        this.yourResources = yourResources;
        this.yourTechs = yourTechs;
        this.yourItems = yourItems;
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

    public Map<String, Integer> getYourResources() {
        return yourResources;
    }

    public List<String> getYourTechs() {
        return yourTechs;
    }

    public Map<String, Integer> getYourItems() {
        return yourItems;
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
            tileMap.put("visible", tile.visible());
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
            unitMap.put("hp", unit.hp());
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
            buildingMap.put("hp", building.hp());
            buildingMap.put("maxHp", building.maxHp());
            buildingMap.put("townHallLevel", building.townHallLevel());
            buildingMap.put("producingKind", building.producingKind());
            buildingMap.put("producingTarget", building.producingTarget());
            buildingMap.put("turnsRemaining", building.turnsRemaining());
            encodedBuildings.add(buildingMap);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rows", rows);
        payload.put("cols", cols);
        payload.put("tiles", encodedTiles);
        payload.put("units", encodedUnits);
        payload.put("buildings", encodedBuildings);
        payload.put("yourResources", yourResources);
        payload.put("yourTechs", yourTechs);
        payload.put("yourItems", yourItems);
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
                    boolean visible = !(map.get("visible") instanceof Boolean b) || b;
                    tiles.add(new TileEntry(col, row, terrain, resources, visible));
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
                    int hp = map.get("hp") instanceof Number number ? number.intValue() : 0;
                    units.add(new UnitEntry(id, ownerId, unitType, col, row, currentAP, hp));
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
                    int hp = map.get("hp") instanceof Number number ? number.intValue() : 0;
                    int maxHp = map.get("maxHp") instanceof Number number ? number.intValue() : 0;
                    String townHallLevel = (String) map.get("townHallLevel");
                    String producingKind = (String) map.get("producingKind");
                    String producingTarget = (String) map.get("producingTarget");
                    int turnsRemaining = map.get("turnsRemaining") instanceof Number number ? number.intValue() : 0;
                    buildings.add(new BuildingEntry(id, ownerId, buildingType, col, row, hp, maxHp,
                            townHallLevel, producingKind, producingTarget, turnsRemaining));
                }
            }
        }

        Map<String, Integer> yourResources = new LinkedHashMap<>();
        if (payload.get("yourResources") instanceof Map<?, ?> resourceMap) {
            for (Map.Entry<?, ?> entry : resourceMap.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    yourResources.put((String) entry.getKey(), number.intValue());
                }
            }
        }

        List<String> yourTechs = new ArrayList<>();
        if (payload.get("yourTechs") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) {
                    yourTechs.add(s);
                }
            }
        }

        Map<String, Integer> yourItems = new LinkedHashMap<>();
        if (payload.get("yourItems") instanceof Map<?, ?> itemMap) {
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    yourItems.put((String) entry.getKey(), number.intValue());
                }
            }
        }

        return new GameStateSnapshotMessage(rows, cols, tiles, units, buildings, yourResources, yourTechs, yourItems, timestamp);
    }

    public record TileEntry(int col, int row, String terrain, Map<String, Integer> resources, boolean visible) {
    }

    public record UnitEntry(int id, String ownerId, String unitType, int col, int row, int currentAP, int hp) {
    }

    public record BuildingEntry(int id, String ownerId, String buildingType, int col, int row, int hp, int maxHp,
                                 String townHallLevel, String producingKind, String producingTarget, int turnsRemaining) {
    }
}
