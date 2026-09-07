package model;

import java.util.ArrayList;
import java.util.List;

public enum BuildingType {
    LUMBER_MILL("Lumber Mill", TerrainType.FOREST, ResourceType.WOOD,
            0, 0, 0, 2, 1, 0, true, 1),
    STONE_MINE("Stone Mine", TerrainType.MOUNTAIN, ResourceType.STONE,
            15, 0, 0, 3, 2, 0, true, 1) {
        @Override
        public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
            return stoneTech;
        }

        @Override
        public boolean isBuildableAt(Tile tile, List<Tile> allTiles) {
            return super.isBuildableAt(tile, allTiles) && tile.hasResource(ResourceType.STONE);
        }
    },
    IRON_MINE("Iron Mine", TerrainType.MOUNTAIN, ResourceType.IRON,
            25, 0, 0, 3, 2, 0, true, 1) {
        @Override
        public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
            return ironTech;
        }

        @Override
        public boolean isBuildableAt(Tile tile, List<Tile> allTiles) {
            return super.isBuildableAt(tile, allTiles) && tile.hasResource(ResourceType.IRON);
        }
    },
    FARM("Farm", TerrainType.MEADOW, ResourceType.WHEAT,
            0, 0, 0, 2, 2, 0, true, 1),
    STABLE("Stable", TerrainType.PLAIN, ResourceType.CATTLE,
            20, 0, 0, 2, 3, 0, true, 1),
    TOWN_HALL("Town Hall", null, ResourceType.NONE,
            0, 0, 0, 0, 0, 3, false, 1) {
        @Override
        public int produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker,
                                     List<Tile> allTiles, boolean commit) {
            if (commit) {
                economy.addResource(ResourceType.WHEAT, 1);
                economy.addResource(ResourceType.WOOD, 1);
            } else {
                economy.addNetChanges(ResourceType.WHEAT, 1);
                economy.addNetChanges(ResourceType.WOOD, 1);
            }
            return 0;
        }
    },
    SETTLEMENT("Settlement", null, ResourceType.NONE,
            25, 15, 10, 0, 2, 2, true, 1) {
        @Override
        public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
            return settlementTech;
        }

        @Override
        public int getUnitCapacityBonus() {
            return 3;
        }

        @Override
        public int produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker,
                                     List<Tile> allTiles, boolean commit) {
            return 0;
        }
    },
    DOCK("Dock", null, ResourceType.FISH,
            30, 0, 0, 2, 2, 0, true, 2) {
        @Override
        public boolean isBuildableAt(Tile tile, List<Tile> allTiles) {
            if (!tile.getTerrain().isPassable() || tile.getTerrain() == TerrainType.SEA) return false;

            for (Tile other : allTiles) {
                if (other.getTerrain() == TerrainType.SEA &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker,
                                     List<Tile> allTiles, boolean commit) {
            if (!building.isOccupied()) return 0;

            for (Tile other : allTiles) {
                if (other.getTerrain() == TerrainType.SEA && other.hasResource(ResourceType.FISH) &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    int desired = ratePerWorker * building.getStationedWorkers().size();
                    int available = other.getResources().getOrDefault(ResourceType.FISH, 0);
                    int amount = Math.min(desired, Math.min(available, roomFor(economy, ResourceType.FISH)));
                    if (amount <= 0) return 0;

                    if (commit) {
                        other.extractResource(ResourceType.FISH, amount);
                        economy.addResource(ResourceType.FISH, amount);
                    } else {
                        economy.addNetChanges(ResourceType.FISH, amount);
                    }
                    return amount;
                }
            }
            return 0;
        }
    },
    BAZAAR("Bazaar", null, ResourceType.NONE,
            40, 20, 0, 0, 2, 0, true, 2),
    TRADING_POST("Trading Post", null, ResourceType.NONE,
            0, 0, 0, 0, 0, 0, false, 1),
    TRIBE_CAMP("Tribe Camp", null, ResourceType.NONE,
            0, 0, 0, 0, 0, 0, false, 1),
    OUTPOST("Outpost", null, ResourceType.NONE,
            0, 0, 0, 0, 0, 2, false, 1),
    MONUMENT("Monument", TerrainType.PLAIN, ResourceType.NONE,
            30, 30, 0, 0, 2, 0, true, 1),
    APOTHECARY("Apothecary", TerrainType.PLAIN, ResourceType.NONE,
            25, 15, 0, 0, 2, 0, true, 2);

    private final String displayName;
    private final TerrainType requiredTerrain;
    private final ResourceType outputResource;

    private final int woodCost;
    private final int stoneCost;
    private final int ironCost;
    private final int apCost;

    private final int maxWorkerCapacity;

    private final int visionRadius;

    private final boolean isPlayerBuildable;
    private final int requiredTownHallLevel;

    BuildingType(String displayName, TerrainType requiredTerrain, ResourceType outputResource,
                 int woodCost, int stoneCost, int ironCost, int maxWorkerCapacity, int apCost, int visionRadius,
                 boolean isPlayerBuildable, int requiredTownHallLevel) {
        this.displayName = displayName;
        this.requiredTerrain = requiredTerrain;
        this.outputResource = outputResource;
        this.woodCost = woodCost;
        this.stoneCost = stoneCost;
        this.ironCost = ironCost;
        this.maxWorkerCapacity = maxWorkerCapacity;
        this.apCost = apCost;
        this.visionRadius = visionRadius;
        this.isPlayerBuildable = isPlayerBuildable;
        this.requiredTownHallLevel = requiredTownHallLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TerrainType getRequiredTerrain() {
        return requiredTerrain;
    }

    public ResourceType getOutputResource() {
        return outputResource;
    }

    public int getWoodCost() {
        return woodCost;
    }

    public int getStoneCost() {
        return stoneCost;
    }

    public int getIronCost() {
        return ironCost;
    }

    public int getMaxWorkerCapacity() {
        return maxWorkerCapacity;
    }

    public int getApCost() {
        return apCost;
    }

    public String getCostString() {
        ArrayList<String> costs = new ArrayList<>();

        if (woodCost > 0) costs.add(woodCost + " Wood");
        if (stoneCost > 0) costs.add(stoneCost + " Stone");
        if (ironCost > 0) costs.add(ironCost + " Iron");

        if (costs.isEmpty()) return "Free";

        return String.join(", ", costs);
    }

    public int getVisionRadius() {
        return visionRadius;
    }

    public boolean isPlayerBuildable() {
        return isPlayerBuildable;
    }

    public int getRequiredTownHallLevel() {
        return requiredTownHallLevel;
    }

    public boolean isBuildableOnTerrain(TerrainType terrain) {
        if (terrain == TerrainType.SEA || terrain == TerrainType.MOUNTAIN_RANGE) return false;
        return requiredTerrain == null || requiredTerrain == terrain;
    }

    public boolean isBuildableAt(Tile tile, List<Tile> allTiles) {
        return isBuildableOnTerrain(tile.getTerrain());
    }

    public boolean isUnlocked(boolean stoneTech, boolean ironTech, boolean settlementTech) {
        return true;
    }

    public int getUnitCapacityBonus() {
        return 0;
    }

    /** How much more of {@code type} the warehouse can still hold - production is clamped to this
     *  so a full storage doesn't waste resources still sitting in the ground/sea. */
    int roomFor(GlobalResourceManager economy, ResourceType type) {
        return Math.max(0, economy.getResourceCapacityAmount(type) - economy.getResourceAmount(type));
    }

    /**
     * Produces this building's output for one turn. When {@code commit} is true, this actually
     * deducts from the tile's resource pool and credits the economy (real turn-end effect).
     * When false, nothing is mutated except {@link GlobalResourceManager}'s net-change tracker -
     * this is what powers the HUD's "next turn" preview. Both paths share this single method so
     * the preview can never drift from what actually happens at turn end.
     */
    public int produceResources(Building building, Tile tile, GlobalResourceManager economy, int ratePerWorker,
                                 List<Tile> allTiles, boolean commit) {
        if (!building.isOccupied()) return 0;

        ResourceType targetResource = getOutputResource();
        if (targetResource == null || targetResource == ResourceType.NONE) return 0;
        if (!tile.hasResource(targetResource)) return 0;

        int desired = ratePerWorker * building.getStationedWorkers().size();
        int available = tile.getResources().getOrDefault(targetResource, 0);
        int amount = Math.min(desired, Math.min(available, roomFor(economy, targetResource)));
        if (amount <= 0) return 0;

        if (commit) {
            tile.extractResource(targetResource, amount);
            economy.addResource(targetResource, amount);
        } else {
            economy.addNetChanges(targetResource, amount);
        }
        return amount;
    }
}
