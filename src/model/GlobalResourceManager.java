package model;

import java.util.HashMap;
import java.util.Map;

public class GlobalResourceManager implements java.io.Serializable {
    private final Map<ResourceType, Integer> resources;
    private final Map<ResourceType, Integer> resourcesCapacity;
    private final Map<ResourceType, Integer> resourceNetChanges;

    public GlobalResourceManager() {
        this.resources = new HashMap<>();
        this.resourcesCapacity = new HashMap<>();
        this.resourceNetChanges = new HashMap<>();

        updateStorage(100, 100, 100, 100, 100, 100);

        initRegistry();
    }

    private void initRegistry() {
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE) {
                this.resources.put(type, 0);
            }
        }

        this.resources.put(ResourceType.WHEAT, 25);
        this.resources.put(ResourceType.CATTLE, 25);
        this.resources.put(ResourceType.WOOD, 50);
        this.resources.put(ResourceType.IRON, 20);
        this.resources.put(ResourceType.STONE, 30);

    }

    public boolean addResource(ResourceType type, int amount) {
        int current = resources.getOrDefault(type, 0);
        if(current + amount > resourcesCapacity.getOrDefault(type, 0)) return false;
        resources.put(type, current + amount);
        return true;
    }

    public boolean spendResource(ResourceType type, int amount) {
        int current = resources.getOrDefault(type, 0);
        if (current >= amount) {
            resources.put(type, current - amount);
            return true;
        }
        return false;
    }

    public boolean spendFood(int amount){
        int current = resources.getOrDefault(ResourceType.WHEAT, 0);
        if(current >= amount){
            resources.put(ResourceType.WHEAT, current - amount);
            return true;
        }else{
            int secCurrent = resources.getOrDefault(ResourceType.CATTLE, 0);
            if(secCurrent + current >= amount){
                resources.put(ResourceType.WHEAT, 0);
                resources.put(ResourceType.CATTLE, secCurrent - (amount - current));
                return true;
            }
        }
        return false;
    }

    public boolean hasEnough(ResourceType type, int amount){
        int current = resources.getOrDefault(type, 0);
        return (amount <= current);
    }

    public boolean hasCapacityFor(ResourceType type, int amount) {
        int current = resources.getOrDefault(type, 0);
        return current + amount <= resourcesCapacity.getOrDefault(type, 0);
    }

    public int getResourceAmount(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }

    public boolean hasEnoughFood(int amount) {
        int current = resources.getOrDefault(ResourceType.WHEAT, 0) +
                resources.getOrDefault(ResourceType.CATTLE, 0);
        return (amount <= current);
    }

    public int getResourceCapacityAmount(ResourceType type) {
        return resourcesCapacity.getOrDefault(type, 0);
    }

    public void updateStorage(int cattle, int wheat, int wood, int stone, int iron, int fish) {
        resourcesCapacity.put(ResourceType.CATTLE, cattle);
        resourcesCapacity.put(ResourceType.WHEAT, wheat);
        resourcesCapacity.put(ResourceType.WOOD, wood);
        resourcesCapacity.put(ResourceType.STONE, stone);
        resourcesCapacity.put(ResourceType.IRON, iron);
        resourcesCapacity.put(ResourceType.FISH, fish);
    }

    public void addNetChanges(ResourceType type, int netChange){
        int current = resourceNetChanges.getOrDefault(type, 0);
        resourceNetChanges.put(type, current + netChange);
    }

    public int getNetChanges(ResourceType type){
        return resourceNetChanges.getOrDefault(type, 0);
    }

    public void resetNetChanges() {
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.NONE) {
                resourceNetChanges.put(type, 0);
            }
        }
    }
}
