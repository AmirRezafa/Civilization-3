package model;

public enum UnitType {
    EXPLORER("Explorer", 6, 3, 20, 1, 3, 3, 0, 0),
    BUILDER("Builder", 4, 2, 20, 3, 1, 3, 0, 0),
    WORKER("Worker", 4, 2, 10, 1, 0, 3, 0, 0),
    BORDER_EXPANDER("Border Expander", 4, 4, 15, 1, 1, 3, 0, 0),
    SWORDSMAN("Swordsman", 2, 2, 15, 1, 1, 1, 10, 1),
    ARCHER("Archer", 2, 2, 15, 1, 2, 1, 6, 2),
    CAVALRY("Cavalry", 4, 3, 25, 1, 2, 2, 8, 1);

    private final String displayName;
    private final int maxAP;
    private final int buildTurns;
    private final int foodCost;
    private final int chargesCount;
    private final int visionRadius;

    private final int maxHP;
    private final int attackPower;
    private final int attackRange;

    UnitType(String displayName, int maxAP, int buildTurns, int foodCost, int chargesCount, int visionRadius,
             int maxHP, int attackPower, int attackRange) {
        this.displayName = displayName;
        this.maxAP = maxAP;
        this.buildTurns = buildTurns;
        this.foodCost = foodCost;
        this.chargesCount = chargesCount;
        this.visionRadius = visionRadius;
        this.maxHP = maxHP;
        this.attackPower = attackPower;
        this.attackRange = attackRange;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxAP() {
        return maxAP;
    }

    public int getChargesCount() {
        return chargesCount;
    }

    public int getBuildTurns() {
        return buildTurns;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public int getVisionRadius() {
        return visionRadius;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public int getAttackRange() {
        return attackRange;
    }
}