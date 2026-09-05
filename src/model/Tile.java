package model;

import java.util.Map;

public class Tile implements java.io.Serializable {
    private int col, row;

    private TerrainType terrain;
    private Map<ResourceType, Integer> resources;
    private Building building;

    private boolean isVisible = false;
    private boolean Explored = false;

    private boolean isOwned;

    public Tile(int col, int row, TerrainType terrain, Map<ResourceType, Integer> resources) {
        this.col = col;
        this.row = row;
        this.terrain = terrain;
        this.resources = resources;
        this.isOwned = false;
    }

    public boolean hasResource(ResourceType type) {
        return (resources != null && resources.containsKey(type) && resources.get(type) > 0);
    }

    public int extractResource(ResourceType type, int amount) {
        int currentAmount = resources.get(type);
        int newAmount = Math.max(0, currentAmount - amount);
        resources.put(type, newAmount);
        return currentAmount - newAmount;
    }

    public boolean hasEnough(ResourceType type, int amount){
        return (resources.get(type) >= amount);
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
        if (visible) this.Explored = true;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isExplored() {
        return Explored;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public Map<ResourceType, Integer> getResources() {
        return resources;
    }

    public boolean isOwned() {
        return isOwned;
    }

    public void setOwned() {
        isOwned = true;
    }
}
