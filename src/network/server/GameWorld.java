package network.server;

import controller.services.WorldGenerator;
import model.Building;
import model.BuildingType;
import model.Tile;
import model.Unit;
import model.UnitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class GameWorld {
    private static final int ROWS = 16;
    private static final int COLS = 16;
    private static final int FIRST_TOWNHALL_X = 6;
    private static final int FIRST_TOWNHALL_Y = 6;
    private static final int MIN_SPAWN_DISTANCE_SQUARED = 50;

    private final Tile[][] tileGrid;
    private final List<Tile> tiles;
    private final List<NetworkUnit> units = new ArrayList<>();
    private final List<NetworkBuilding> buildings = new ArrayList<>();
    private final Map<Integer, NetworkUnit> unitsById = new HashMap<>();
    private final AtomicInteger nextUnitId = new AtomicInteger(1);
    private final AtomicInteger nextBuildingId = new AtomicInteger(1);

    private GameWorld(Tile[][] tileGrid, List<Tile> tiles) {
        this.tileGrid = tileGrid;
        this.tiles = tiles;
    }

    static GameWorld generate(List<String> playerIds) {
        Random random = new Random();
        WorldGenerator.WorldData firstData = new WorldGenerator().generate(ROWS, COLS, FIRST_TOWNHALL_X, FIRST_TOWNHALL_Y);
        GameWorld world = new GameWorld(firstData.tileGrid, firstData.tiles);

        for (Building neutralBuilding : firstData.neutralBuildings) {
            world.registerBuilding(neutralBuilding, null);
        }

        int avoidCol = FIRST_TOWNHALL_X;
        int avoidRow = FIRST_TOWNHALL_Y;
        for (int i = 0; i < playerIds.size(); i++) {
            String playerId = playerIds.get(i);
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

    private int[] findSpawnSpot(int avoidCol, int avoidRow, Random random) {
        for (int attempt = 0; attempt < 500; attempt++) {
            int col = 2 + random.nextInt(COLS - 4);
            int row = 2 + random.nextInt(ROWS - 4);
            Tile tile = tileGrid[col][row];
            if (tile.getBuilding() != null) continue;
            if (!isLandHex(tile)) continue;

            int distSq = (col - avoidCol) * (col - avoidCol) + (row - avoidRow) * (row - avoidRow);
            if (distSq < MIN_SPAWN_DISTANCE_SQUARED) continue;

            return new int[]{col, row};
        }
        return new int[]{avoidCol, avoidRow};
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

    private void registerUnit(Unit unit, String ownerId) {
        NetworkUnit networkUnit = new NetworkUnit(nextUnitId.getAndIncrement(), unit, ownerId);
        units.add(networkUnit);
        unitsById.put(networkUnit.id, networkUnit);
    }

    private void registerBuilding(Building building, String ownerId) {
        NetworkBuilding networkBuilding = new NetworkBuilding(nextBuildingId.getAndIncrement(), building, ownerId);
        buildings.add(networkBuilding);
    }

    int getRows() {
        return ROWS;
    }

    int getCols() {
        return COLS;
    }

    Tile getTile(int col, int row) {
        return tileGrid[col][row];
    }

    List<Tile> getTiles() {
        return tiles;
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

    static class NetworkUnit {
        final int id;
        final Unit unit;
        final String ownerId;

        NetworkUnit(int id, Unit unit, String ownerId) {
            this.id = id;
            this.unit = unit;
            this.ownerId = ownerId;
        }
    }

    static class NetworkBuilding {
        final int id;
        final Building building;
        final String ownerId;

        NetworkBuilding(int id, Building building, String ownerId) {
            this.id = id;
            this.building = building;
            this.ownerId = ownerId;
        }
    }
}
