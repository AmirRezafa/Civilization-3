package network.protocol;

public enum ItemType {
    TELEPORT("Teleport"),
    MOBILITY("Mobility"),
    COMBAT_BOOST("Combat Boost");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
