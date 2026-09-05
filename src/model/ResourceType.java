package model;

public enum ResourceType {
    NONE("No Resource", null),
    WOOD("Woodlands", CoreResource.WOOD),
    STONE("Stone Outcrops", CoreResource.STONE),
    IRON("Iron Veins", CoreResource.IRON),
    WHEAT("Wheat and Rice Fields", CoreResource.FOOD),
    CATTLE("Livestock (Cattle and Sheep)", CoreResource.FOOD),
    FISH("Fish Shoals", CoreResource.FOOD);

    private final String displayName;
    private final CoreResource coreResourceType;

    ResourceType(String displayName, CoreResource coreResourceType) {
        this.displayName = displayName;
        this.coreResourceType = coreResourceType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CoreResource getCoreResourceType() {
        return coreResourceType;
    }
}