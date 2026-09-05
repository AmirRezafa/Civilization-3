package controller.services;

import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class WorldGenerator {

    public static class WorldData {
        public final ArrayList<Tile> tiles;
        public final Tile[][] tileGrid;
        public final Tile townhall;
        public final Building townhallBuilding;
        public final List<Unit> initialUnits;
        public final Map<HexEdge, EdgeFeature> edgeFeatures;
        public final Set<HexEdge> riverEdges;
        public final List<Building> neutralBuildings;
        public final List<Tribe> tribes;

        public WorldData(ArrayList<Tile> tiles, Tile[][] tileGrid, Tile townhall,
                          Building townhallBuilding, List<Unit> initialUnits,
                          Map<HexEdge, EdgeFeature> edgeFeatures, Set<HexEdge> riverEdges,
                          List<Building> neutralBuildings, List<Tribe> tribes) {
            this.tiles = tiles;
            this.tileGrid = tileGrid;
            this.townhall = townhall;
            this.townhallBuilding = townhallBuilding;
            this.initialUnits = initialUnits;
            this.edgeFeatures = edgeFeatures;
            this.riverEdges = riverEdges;
            this.neutralBuildings = neutralBuildings;
            this.tribes = tribes;
        }
    }

    public WorldData generate(int rows, int cols, int townhallX, int townhallY) {
        Random random = new Random();
        TerrainType[] coreTypes = {TerrainType.PLAIN, TerrainType.FOREST, TerrainType.MOUNTAIN, TerrainType.MEADOW};
        TerrainType[] specialTypes = {TerrainType.SEA, TerrainType.MOUNTAIN_RANGE};

        // Seed density is tuned relative to the original 100x100 (10,000-tile) map, where
        // 120 core + 30 special seeds produced reasonably large, contiguous terrain blobs.
        // Scaling by tile count keeps that same blob size on smaller/larger maps instead of
        // fragmenting terrain into a checkerboard when the map shrinks.
        int totalTiles = rows * cols;
        int coreSeedsCount = Math.max(4, (int) (totalTiles * 0.012));
        int specialSeedsCount = Math.max(1, (int) (totalTiles * 0.003));
        int seedsCount = coreSeedsCount + specialSeedsCount;
        int[][] seeds = new int[seedsCount + coreTypes.length + 1][2];
        TerrainType[] seedTypes = new TerrainType[seedsCount + coreTypes.length + 1];

        for (int i = 0; i < seedsCount; i++) {
            int c = random.nextInt(cols);
            int r = random.nextInt(rows);
            while(Math.pow(c - townhallX, 2) + Math.pow(r - townhallY, 2) < 16){
                c = random.nextInt(cols);
                r = random.nextInt(rows);
            }
            seeds[i][0] = c;
            seeds[i][1] = r;
            if (i < coreSeedsCount) {
                seedTypes[i] = coreTypes[random.nextInt(coreTypes.length)];
            } else {
                seedTypes[i] = specialTypes[random.nextInt(specialTypes.length)];
            }
        }

        ArrayList<int[]> positions = new ArrayList<>(List.of(
                new int[]{townhallX + 5, townhallY},
                new int[]{townhallX - 5, townhallY},
                new int[]{townhallX, townhallY + 5},
                new int[]{townhallX, townhallY - 5},
                new int[]{townhallX + 4, townhallY + 4}
        ));

        Collections.shuffle(positions, random);

        for(int i = 0; i < coreTypes.length + 1; i++){
            seeds[i + seedsCount] = positions.get(i);
            seedTypes[i + seedsCount] = coreTypes[((i + 1) % coreTypes.length)];
        }

        ArrayList<Tile> tempTiles = new ArrayList<>();
        Tile[][] tileGrid = new Tile[rows][cols];
        Tile townhallTile = null;

        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                double minD = Double.MAX_VALUE;
                TerrainType finalType = coreTypes[0];

                for (int i = 0; i < seedsCount + coreTypes.length + 1; i++) {
                    double dist = Math.pow(seeds[i][0] - col, 2) + Math.pow(seeds[i][1] - row, 2);
                    dist += random.nextDouble() * 8.0;

                    if (dist < minD) {
                        minD = dist;
                        finalType = seedTypes[i];
                    }
                }

                Map<ResourceType, Integer> tileResources = new HashMap<>();

                switch (finalType) {
                    case FOREST:
                        tileResources.put(ResourceType.WOOD, 500);
                        break;

                    case MOUNTAIN:
                        tileResources.put(ResourceType.STONE, 500);

                        if (random.nextDouble() < 0.20)
                            tileResources.put(ResourceType.IRON, 150);
                        break;

                    case PLAIN:
                        if (random.nextDouble() < 0.20)
                            tileResources.put(ResourceType.CATTLE, 300);
                        break;

                    case MEADOW:
                        if (random.nextDouble() < 0.30)
                            tileResources.put(ResourceType.WHEAT, 300);
                        break;

                    case SEA:
                        if (random.nextDouble() < 0.30)
                            tileResources.put(ResourceType.FISH, 300);
                        break;
                }
                Tile tile = new Tile(col, row, finalType, tileResources);
                tileGrid[col][row] = tile;
                tempTiles.add(tile);
                if(col == townhallX && row == townhallY) townhallTile = tile;
            }
        }

        Building townhallBuilding = new Building(BuildingType.TOWN_HALL, townhallX, townhallY);
        townhallTile.setBuilding(townhallBuilding);

        List<Unit> initialUnits = new ArrayList<>();
        initialUnits.add(new Unit(UnitType.BUILDER, townhallX, townhallY + 1));
        initialUnits.add(new Unit(UnitType.BUILDER, townhallX + 1, townhallY));
        initialUnits.add(new Unit(UnitType.WORKER, townhallX - 1, townhallY + 1));
        initialUnits.add(new Unit(UnitType.WORKER, townhallX, townhallY - 1));
        initialUnits.add(new Unit(UnitType.EXPLORER, townhallX + 1, townhallY + 1));

        Set<HexEdge> riverEdges = generateRivers(tempTiles, rows, cols, random);
        Map<HexEdge, EdgeFeature> edgeFeatures = new HashMap<>();
        List<Building> neutralBuildings = placeTradingPosts(tempTiles, townhallX, townhallY, random);

        List<Tribe> tribes = placeTribes(tempTiles, townhallX, townhallY, random);
        for (Tribe tribe : tribes) {
            neutralBuildings.add(tileGrid[tribe.getCol()][tribe.getRow()].getBuilding());
        }

        return new WorldData(tempTiles, tileGrid, townhallTile, townhallBuilding, initialUnits, edgeFeatures,
                riverEdges, neutralBuildings, tribes);
    }

    private boolean isLandHex(Tile tile) {
        return tile.getTerrain() != TerrainType.SEA && tile.getTerrain() != TerrainType.MOUNTAIN_RANGE
                && tile.getTerrain().isPassable();
    }

    private static final String[] TRIBE_NAME_PREFIXES = {
            "Ashgrove", "Stonemere", "Fallowmere", "Highcairn", "Duskholt",
            "Brightwater", "Ironmoor", "Willowfen", "Cragmont", "Saltmarsh"
    };

    private List<Tribe> placeTribes(List<Tile> tiles, int townhallX, int townhallY, Random random) {
        List<Tribe> tribes = new ArrayList<>();
        int placed = 0;
        int attempts = 0;

        while (placed < 3 && attempts < 200) {
            attempts++;
            Tile candidate = tiles.get(random.nextInt(tiles.size()));
            if (!isLandHex(candidate)) continue;
            if (candidate.getBuilding() != null) continue;

            double distSq = Math.pow(candidate.getCol() - townhallX, 2) + Math.pow(candidate.getRow() - townhallY, 2);
            if (distSq < 100) continue;

            Building camp = new Building(BuildingType.TRIBE_CAMP, candidate.getCol(), candidate.getRow());
            TribeType[] types = TribeType.values();
            TribeType type = types[random.nextInt(types.length)];
            camp.setMaxHP(type.getCampHP());
            camp.heal(type.getCampHP());
            candidate.setBuilding(camp);
            String name = TRIBE_NAME_PREFIXES[random.nextInt(TRIBE_NAME_PREFIXES.length)] + " " + type.getDisplayName();
            Tribe tribe = new Tribe(candidate.getCol(), candidate.getRow(), type, name);
            camp.setOwner(tribe);
            tribes.add(tribe);
            placed++;
        }

        return tribes;
    }

    private List<Building> placeTradingPosts(List<Tile> tiles, int townhallX, int townhallY, Random random) {
        List<Building> posts = new ArrayList<>();
        int placed = 0;
        int attempts = 0;

        while (placed < 3 && attempts < 200) {
            attempts++;
            Tile candidate = tiles.get(random.nextInt(tiles.size()));
            if (!isLandHex(candidate)) continue;
            if (candidate.getBuilding() != null) continue;

            double distSq = Math.pow(candidate.getCol() - townhallX, 2) + Math.pow(candidate.getRow() - townhallY, 2);
            if (distSq < 100) continue;

            Building post = new Building(BuildingType.TRADING_POST, candidate.getCol(), candidate.getRow());
            candidate.setBuilding(post);
            posts.add(post);
            placed++;
        }

        return posts;
    }

    private Set<HexEdge> generateRivers(List<Tile> tiles, int rows, int cols, Random random) {
        double riverChance = 0.04;
        Set<HexEdge> riverEdges = new HashSet<>();

        for (Tile tile : tiles) {
            int col = tile.getCol();
            int row = tile.getRow();

            int[][] neighborOffsets = (col % 2 == 0)
                    ? new int[][]{{col, row - 1}, {col, row + 1}, {col - 1, row}, {col - 1, row + 1}, {col + 1, row}, {col + 1, row + 1}}
                    : new int[][]{{col, row - 1}, {col, row + 1}, {col - 1, row - 1}, {col - 1, row}, {col + 1, row - 1}, {col + 1, row}};

            for (int[] neighbor : neighborOffsets) {
                int nCol = neighbor[0];
                int nRow = neighbor[1];
                if (nCol < 0 || nCol >= cols || nRow < 0 || nRow >= rows) continue;

                if (random.nextDouble() < riverChance) {
                    riverEdges.add(new HexEdge(col, row, nCol, nRow));
                }
            }
        }

        return riverEdges;
    }
}
