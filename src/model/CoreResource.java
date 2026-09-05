package model;

public enum CoreResource {
    FOOD("Food"),
    WOOD("Wood"),
    STONE("Stone"),
    IRON("Iron");

    private final String displayName;

    CoreResource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}