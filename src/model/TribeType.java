package model;

import java.util.List;

public enum TribeType {
    FARMER("Farmer", List.of(ResourceType.WHEAT), 0.75, true, 40),
    MOUNTAIN("Mountain", List.of(ResourceType.STONE, ResourceType.IRON), 0.75, true, 50),
    TRADER("Trader", List.of(ResourceType.WOOD, ResourceType.STONE, ResourceType.IRON, ResourceType.WHEAT), 0.80, true, 50),
    COASTAL("Coastal", List.of(ResourceType.WHEAT), 0.75, true, 50),
    WARRIOR("Warrior", List.of(), 0.0, false, 70);

    private final String displayName;
    private final List<ResourceType> tradeRewardOptions;
    private final double tradeRate;
    private final boolean canTrade;
    private final int campHP;

    TribeType(String displayName, List<ResourceType> tradeRewardOptions, double tradeRate, boolean canTrade, int campHP) {
        this.displayName = displayName;
        this.tradeRewardOptions = tradeRewardOptions;
        this.tradeRate = tradeRate;
        this.canTrade = canTrade;
        this.campHP = campHP;
    }

    public int getCampHP() {
        return campHP;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<ResourceType> getTradeRewardOptions() {
        return tradeRewardOptions;
    }

    public ResourceType getTradeRewardResource() {
        return tradeRewardOptions.isEmpty() ? null : tradeRewardOptions.get(0);
    }

    public double getTradeRate() {
        return tradeRate;
    }

    public boolean canTrade() {
        return canTrade;
    }
}
