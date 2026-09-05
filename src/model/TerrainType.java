package model;

public enum TerrainType {
    PLAIN("Plain", 1, true),
    FOREST("Forest", 2, true),
    MOUNTAIN("Mountain", 4, true),
    MEADOW("Meadow", 1, true),
    SEA("Sea", 2, true),
    MOUNTAIN_RANGE("Mountain Range", 4, false);

    private final String displayName;
    private final int movementCost;
    private final boolean isPassable;

    TerrainType(String displayName, int movementCost, boolean isPassable) {
        this.displayName = displayName;
        this.movementCost = movementCost;
        this.isPassable = isPassable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMovementCost() {
        return movementCost;
    }

    public boolean isPassable() {
        return isPassable;
    }
}