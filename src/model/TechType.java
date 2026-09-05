package model;

public enum TechType {
    STONE_MINING("Stone Mining Tech", TownHallLevel.LEVEL_1, true, 0, ResourceType.WOOD, 50),
    IRON_MINING("Iron Mining Tech", TownHallLevel.LEVEL_1, true, 0, ResourceType.STONE, 100),
    SETTLEMENT_TECH("Settlement Tech", TownHallLevel.LEVEL_1, true, 0, ResourceType.WOOD, 150),
    PRO_TOOLS("Pro Tools Tech", TownHallLevel.LEVEL_1, true, 0, ResourceType.IRON, 100),
    SAILING("Sailing", TownHallLevel.LEVEL_2, false, 4, ResourceType.WOOD, 80),
    STEEL_TOOLS("Steel Tools", TownHallLevel.LEVEL_2, false, 3, ResourceType.IRON, 40),
    DEFENSIVE_ARCHITECTURE("Defensive Architecture", TownHallLevel.LEVEL_3, false, 4, ResourceType.STONE, 100);

    private final String displayName;
    private final TownHallLevel requiredLevel;
    private final boolean instant;
    private final int researchTurns;
    private final ResourceType costResource;
    private final int costAmount;

    TechType(String displayName, TownHallLevel requiredLevel, boolean instant, int researchTurns,
             ResourceType costResource, int costAmount) {
        this.displayName = displayName;
        this.requiredLevel = requiredLevel;
        this.instant = instant;
        this.researchTurns = researchTurns;
        this.costResource = costResource;
        this.costAmount = costAmount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TownHallLevel getRequiredLevel() {
        return requiredLevel;
    }

    public boolean isInstant() {
        return instant;
    }

    public int getResearchTurns() {
        return researchTurns;
    }

    public ResourceType getCostResource() {
        return costResource;
    }

    public int getCostAmount() {
        return costAmount;
    }

    public String getCostString() {
        if (costAmount <= 0) return "Free";

        String resourceName = switch (costResource) {
            case WOOD -> "Wood";
            case STONE -> "Stone";
            case IRON -> "Iron";
            case WHEAT -> "Wheat";
            case CATTLE -> "Cattle";
            default -> "";
        };
        return costAmount + " " + resourceName;
    }
}
