package model;

public enum QuestType {
    FARMER_SUPPLIES("Help with food storage", ResourceType.WOOD, 20, ResourceType.STONE, 10,
            5, ResourceType.WHEAT, 30, 15),
    TRADER_ROUTE("Connect a trade route (build road to camp)", null, 0, null, 0,
            10, null, 0, 20),
    WARRIOR_DEFEAT("Defeat 2 hostile units near the camp", null, 0, null, 0,
            8, null, 0, 20),
    MOUNTAIN_TOOLS("Deliver mining tools", ResourceType.WOOD, 15, ResourceType.IRON, 10,
            6, ResourceType.STONE, 20, 15),
    COASTAL_DOCK("Build a Dock within 4 hexes of the camp", null, 0, null, 0,
            10, ResourceType.WHEAT, 30, 20);

    private final String displayName;
    private final ResourceType costResource1;
    private final int costAmount1;
    private final ResourceType costResource2;
    private final int costAmount2;
    private final int deadlineTurns;
    private final ResourceType rewardResource;
    private final int rewardAmount;
    private final int rewardRelationship;

    QuestType(String displayName, ResourceType costResource1, int costAmount1,
              ResourceType costResource2, int costAmount2, int deadlineTurns,
              ResourceType rewardResource, int rewardAmount, int rewardRelationship) {
        this.displayName = displayName;
        this.costResource1 = costResource1;
        this.costAmount1 = costAmount1;
        this.costResource2 = costResource2;
        this.costAmount2 = costAmount2;
        this.deadlineTurns = deadlineTurns;
        this.rewardResource = rewardResource;
        this.rewardAmount = rewardAmount;
        this.rewardRelationship = rewardRelationship;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceType getCostResource1() {
        return costResource1;
    }

    public int getCostAmount1() {
        return costAmount1;
    }

    public ResourceType getCostResource2() {
        return costResource2;
    }

    public int getCostAmount2() {
        return costAmount2;
    }

    public int getDeadlineTurns() {
        return deadlineTurns;
    }

    public ResourceType getRewardResource() {
        return rewardResource;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    public int getRewardRelationship() {
        return rewardRelationship;
    }

    public static QuestType forTribeType(TribeType tribeType) {
        return switch (tribeType) {
            case FARMER -> FARMER_SUPPLIES;
            case TRADER -> TRADER_ROUTE;
            case WARRIOR -> WARRIOR_DEFEAT;
            case MOUNTAIN -> MOUNTAIN_TOOLS;
            case COASTAL -> COASTAL_DOCK;
        };
    }
}
