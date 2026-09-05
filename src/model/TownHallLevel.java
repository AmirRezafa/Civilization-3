package model;

public enum TownHallLevel {
    LEVEL_1("Town Hall", 1, 200,
            100, 100, 100, 100, 100, 100,
            0, 0, 0, 0, 0, 5),
    LEVEL_2("Settlement", 2, 200,
            150, 150, 250, 200, 180, 150,
            50, 50, 0, 3, 50, 8),
    LEVEL_3("Capital", 3, 200,
            400, 400, 600, 500, 400, 400,
            0, 100, 50, 5, 0, 12);

    private final String displayName;
    private final int levelNumber;
    private final int maxHP;

    private final int cattleCapacity;
    private final int wheatCapacity;
    private final int woodCapacity;
    private final int stoneCapacity;
    private final int ironCapacity;
    private final int fishCapacity;

    private final int upgradeWoodCost;
    private final int upgradeStoneCost;
    private final int upgradeIronCost;
    private final int upgradeTurns;

    private final int healOnUpgrade;

    private final int militaryUnitCap;

    TownHallLevel(String displayName, int levelNumber, int maxHP,
                  int cattleCapacity, int wheatCapacity, int woodCapacity, int stoneCapacity, int ironCapacity,
                  int fishCapacity, int upgradeWoodCost, int upgradeStoneCost, int upgradeIronCost, int upgradeTurns,
                  int healOnUpgrade, int militaryUnitCap) {
        this.displayName = displayName;
        this.levelNumber = levelNumber;
        this.maxHP = maxHP;
        this.cattleCapacity = cattleCapacity;
        this.wheatCapacity = wheatCapacity;
        this.woodCapacity = woodCapacity;
        this.stoneCapacity = stoneCapacity;
        this.ironCapacity = ironCapacity;
        this.fishCapacity = fishCapacity;
        this.upgradeWoodCost = upgradeWoodCost;
        this.upgradeStoneCost = upgradeStoneCost;
        this.upgradeIronCost = upgradeIronCost;
        this.upgradeTurns = upgradeTurns;
        this.healOnUpgrade = healOnUpgrade;
        this.militaryUnitCap = militaryUnitCap;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public int getCattleCapacity() {
        return cattleCapacity;
    }

    public int getWheatCapacity() {
        return wheatCapacity;
    }

    public int getWoodCapacity() {
        return woodCapacity;
    }

    public int getStoneCapacity() {
        return stoneCapacity;
    }

    public int getIronCapacity() {
        return ironCapacity;
    }

    public int getFishCapacity() {
        return fishCapacity;
    }

    public int getUpgradeWoodCost() {
        return upgradeWoodCost;
    }

    public int getUpgradeStoneCost() {
        return upgradeStoneCost;
    }

    public int getUpgradeIronCost() {
        return upgradeIronCost;
    }

    public int getUpgradeTurns() {
        return upgradeTurns;
    }

    public int getHealOnUpgrade() {
        return healOnUpgrade;
    }

    public int getMilitaryUnitCap() {
        return militaryUnitCap;
    }

    public TownHallLevel getNextLevel() {
        if (this == LEVEL_1) return LEVEL_2;
        if (this == LEVEL_2) return LEVEL_3;
        return null;
    }
}
