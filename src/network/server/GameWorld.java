package network.server;

import controller.services.WorldGenerator;
import model.Building;
import model.BuildingType;
import model.GlobalResourceManager;
import model.HexUtils;
import model.ResourceType;
import model.TechType;
import model.Tile;
import model.Unit;
import model.UnitType;
import network.protocol.ItemType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class GameWorld implements java.io.Serializable {
    private static final int FAIRNESS_RADIUS = 5;
    private static final int MAX_MAP_REGENERATIONS = 15;
    private static final int MAX_SPAWN_CANDIDATE_ATTEMPTS = 60;
    private static final int MIN_SPAWN_DISTANCE_SQUARED = 40;

    private static final Map<String, MapPreset> MAP_PRESETS = Map.of(
            "Grasslands Valley", new MapPreset(16, 16, 6, 6),
            "Highland Frontier", new MapPreset(20, 20, 7, 7),
            "Coastal Reach", new MapPreset(18, 22, 6, 8)
    );
    private static final MapPreset DEFAULT_PRESET = MAP_PRESETS.get("Grasslands Valley");

    private final int rows;
    private final int cols;
    private final Tile[][] tileGrid;
    private final List<Tile> tiles;
    private final List<NetworkUnit> units = new ArrayList<>();
    private final List<NetworkBuilding> buildings = new ArrayList<>();
    private final Map<Integer, NetworkUnit> unitsById = new HashMap<>();
    private final Map<String, PlayerEconomy> economies = new HashMap<>();
    private final AtomicInteger nextUnitId = new AtomicInteger(1);
    private final AtomicInteger nextBuildingId = new AtomicInteger(1);
    private final Map<String, boolean[][]> exploredByPlayer = new HashMap<>();

    private GameWorld(int rows, int cols, Tile[][] tileGrid, List<Tile> tiles) {
        this.rows = rows;
        this.cols = cols;
        this.tileGrid = tileGrid;
        this.tiles = tiles;
    }

    static GameWorld generate(List<String> playerIds, String mapName) {
        MapPreset preset = MAP_PRESETS.getOrDefault(mapName, DEFAULT_PRESET);
        Random random = new Random();

        WorldGenerator.WorldData firstData = null;
        for (int attempt = 0; attempt < MAX_MAP_REGENERATIONS; attempt++) {
            WorldGenerator.WorldData candidate = new WorldGenerator()
                    .generate(preset.rows(), preset.cols(), preset.firstTownhallX(), preset.firstTownhallY());
            firstData = candidate;
            if (hasFairResourceAccess(candidate.tiles, preset.firstTownhallX(), preset.firstTownhallY())) {
                break;
            }
        }

        GameWorld world = new GameWorld(preset.rows(), preset.cols(), firstData.tileGrid, firstData.tiles);

        for (Building neutralBuilding : firstData.neutralBuildings) {
            world.registerBuilding(neutralBuilding, null);
        }

        int avoidCol = preset.firstTownhallX();
        int avoidRow = preset.firstTownhallY();
        for (int i = 0; i < playerIds.size(); i++) {
            String playerId = playerIds.get(i);
            world.economies.put(playerId, new PlayerEconomy());
            if (i == 0) {
                world.registerBuilding(firstData.townhallBuilding, playerId);
                for (Unit unit : firstData.initialUnits) {
                    world.registerUnit(unit, playerId);
                }
            } else {
                int[] spot = world.findSpawnSpot(avoidCol, avoidRow, random);
                world.placeSpawn(spot[0], spot[1], playerId);
                avoidCol = spot[0];
                avoidRow = spot[1];
            }
        }
        return world;
    }

    private static boolean hasFairResourceAccess(List<Tile> tiles, int col, int row) {
        List<Tile> nearby = HexUtils.hexesWithinRadius(col, row, FAIRNESS_RADIUS, tiles);
        boolean hasWood = false, hasStone = false, hasIron = false, hasFood = false;
        for (Tile t : nearby) {
            if (t.hasResource(ResourceType.WOOD)) hasWood = true;
            if (t.hasResource(ResourceType.STONE)) hasStone = true;
            if (t.hasResource(ResourceType.IRON)) hasIron = true;
            if (t.hasResource(ResourceType.WHEAT) || t.hasResource(ResourceType.CATTLE) || t.hasResource(ResourceType.FISH)) {
                hasFood = true;
            }
        }
        return hasWood && hasStone && hasIron && hasFood;
    }

    private int[] findSpawnSpot(int avoidCol, int avoidRow, Random random) {
        int[] fallback = null;
        for (int attempt = 0; attempt < MAX_SPAWN_CANDIDATE_ATTEMPTS; attempt++) {
            int col = 2 + random.nextInt(Math.max(1, cols - 4));
            int row = 2 + random.nextInt(Math.max(1, rows - 4));
            Tile tile = tileGrid[col][row];
            if (tile.getBuilding() != null) continue;
            if (!isLandHex(tile)) continue;

            int distSq = (col - avoidCol) * (col - avoidCol) + (row - avoidRow) * (row - avoidRow);
            if (distSq < MIN_SPAWN_DISTANCE_SQUARED) continue;

            if (fallback == null) {
                fallback = new int[]{col, row};
            }
            if (hasFairResourceAccess(tiles, col, row)) {
                return new int[]{col, row};
            }
        }
        return fallback != null ? fallback : new int[]{avoidCol, avoidRow};
    }

    private boolean isLandHex(Tile tile) {
        return tile.getTerrain() != model.TerrainType.SEA && tile.getTerrain() != model.TerrainType.MOUNTAIN_RANGE
                && tile.getTerrain().isPassable();
    }

    private void placeSpawn(int col, int row, String playerId) {
        Building townhall = new Building(BuildingType.TOWN_HALL, col, row);
        tileGrid[col][row].setBuilding(townhall);
        registerBuilding(townhall, playerId);

        registerUnit(new Unit(UnitType.BUILDER, col, row + 1), playerId);
        registerUnit(new Unit(UnitType.BUILDER, col + 1, row), playerId);
        registerUnit(new Unit(UnitType.WORKER, col - 1, row + 1), playerId);
        registerUnit(new Unit(UnitType.WORKER, col, row - 1), playerId);
        registerUnit(new Unit(UnitType.EXPLORER, col + 1, row + 1), playerId);
    }

    NetworkUnit registerUnit(Unit unit, String ownerId) {
        NetworkUnit networkUnit = new NetworkUnit(nextUnitId.getAndIncrement(), unit, ownerId);
        units.add(networkUnit);
        unitsById.put(networkUnit.id, networkUnit);
        return networkUnit;
    }

    NetworkBuilding registerBuilding(Building building, String ownerId) {
        NetworkBuilding networkBuilding = new NetworkBuilding(nextBuildingId.getAndIncrement(), building, ownerId);
        buildings.add(networkBuilding);
        return networkBuilding;
    }

    int getRows() {
        return rows;
    }

    int getCols() {
        return cols;
    }

    Tile getTile(int col, int row) {
        return tileGrid[col][row];
    }

    List<Tile> getTiles() {
        return tiles;
    }

    void markExplored(String playerId, int col, int row) {
        boolean[][] explored = exploredByPlayer.computeIfAbsent(playerId, id -> new boolean[rows][cols]);
        explored[col][row] = true;
    }

    boolean isExplored(String playerId, int col, int row) {
        boolean[][] explored = exploredByPlayer.get(playerId);
        return explored != null && explored[col][row];
    }

    List<NetworkUnit> getUnits() {
        return units;
    }

    List<NetworkBuilding> getBuildings() {
        return buildings;
    }

    NetworkUnit getUnitById(int id) {
        return unitsById.get(id);
    }

    NetworkBuilding getBuildingById(int id) {
        for (NetworkBuilding networkBuilding : buildings) {
            if (networkBuilding.id == id) {
                return networkBuilding;
            }
        }
        return null;
    }

    List<NetworkUnit> getUnitsAt(int col, int row) {
        List<NetworkUnit> result = new ArrayList<>();
        for (NetworkUnit networkUnit : units) {
            if (networkUnit.unit.getCol() == col && networkUnit.unit.getRow() == row) {
                result.add(networkUnit);
            }
        }
        return result;
    }

    List<NetworkUnit> getUnitsOwnedBy(String ownerId) {
        List<NetworkUnit> result = new ArrayList<>();
        for (NetworkUnit networkUnit : units) {
            if (ownerId.equals(networkUnit.ownerId)) {
                result.add(networkUnit);
            }
        }
        return result;
    }

    List<NetworkBuilding> getBuildingsOwnedBy(String ownerId) {
        List<NetworkBuilding> result = new ArrayList<>();
        for (NetworkBuilding networkBuilding : buildings) {
            if (ownerId.equals(networkBuilding.ownerId)) {
                result.add(networkBuilding);
            }
        }
        return result;
    }

    NetworkBuilding getBuildingAt(int col, int row) {
        for (NetworkBuilding networkBuilding : buildings) {
            if (networkBuilding.building.getCol() == col && networkBuilding.building.getRow() == row) {
                return networkBuilding;
            }
        }
        return null;
    }

    void removeUnit(int id) {
        NetworkUnit removed = unitsById.remove(id);
        if (removed != null) {
            units.remove(removed);
        }
    }

    void removeBuilding(int id) {
        NetworkBuilding toRemove = getBuildingById(id);
        if (toRemove != null) {
            buildings.remove(toRemove);
            tileGrid[toRemove.building.getCol()][toRemove.building.getRow()].setBuilding(null);
        }
    }

    PlayerEconomy getEconomy(String playerId) {
        return economies.get(playerId);
    }

    void removePlayer(String playerId) {
        economies.remove(playerId);
    }

    boolean hasBuildingType(String playerId, BuildingType type) {
        for (NetworkBuilding networkBuilding : buildings) {
            if (playerId.equals(networkBuilding.ownerId) && networkBuilding.building.getType() == type
                    && !networkBuilding.building.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    int countActiveTownHalls(String playerId) {
        int count = 0;
        for (NetworkBuilding networkBuilding : buildings) {
            if (playerId.equals(networkBuilding.ownerId) && networkBuilding.building.getType() == BuildingType.TOWN_HALL
                    && !networkBuilding.building.isDestroyed()) {
                count++;
            }
        }
        return count;
    }

    static class NetworkUnit implements java.io.Serializable {
        final int id;
        final long createdAt;
        final Unit unit;
        final String ownerId;

        NetworkUnit(int id, Unit unit, String ownerId) {
            this.id = id;
            this.createdAt = System.currentTimeMillis();
            this.unit = unit;
            this.ownerId = ownerId;
        }
    }

    static class NetworkBuilding implements java.io.Serializable {
        final int id;
        final long createdAt;
        final Building building;
        final String ownerId;

        NetworkBuilding(int id, Building building, String ownerId) {
            this.id = id;
            this.createdAt = System.currentTimeMillis();
            this.building = building;
            this.ownerId = ownerId;
        }
    }

    static class PlayerEconomy implements java.io.Serializable {
        final long createdAt = System.currentTimeMillis();
        final GlobalResourceManager resources = new GlobalResourceManager();
        final Map<TechType, Boolean> researchedTechs = new HashMap<>();
        final Map<ItemType, Integer> items = new HashMap<>();

        boolean hasTech(TechType tech) {
            return researchedTechs.getOrDefault(tech, false);
        }

        int getItemCount(ItemType type) {
            return items.getOrDefault(type, 0);
        }

        void addItem(ItemType type, int amount) {
            items.merge(type, amount, Integer::sum);
        }

        boolean consumeItem(ItemType type) {
            int current = items.getOrDefault(type, 0);
            if (current <= 0) {
                return false;
            }
            items.put(type, current - 1);
            return true;
        }
    }

    private record MapPreset(int rows, int cols, int firstTownhallX, int firstTownhallY) {
    }
}
