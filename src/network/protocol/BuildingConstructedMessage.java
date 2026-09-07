package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class BuildingConstructedMessage extends Message {
    private final int buildingId;
    private final String ownerId;
    private final String buildingType;
    private final int col;
    private final int row;

    public BuildingConstructedMessage(int buildingId, String ownerId, String buildingType, int col, int row) {
        super(MessageType.BUILDING_CONSTRUCTED);
        this.buildingId = buildingId;
        this.ownerId = ownerId;
        this.buildingType = buildingType;
        this.col = col;
        this.row = row;
    }

    private BuildingConstructedMessage(int buildingId, String ownerId, String buildingType, int col, int row, long timestamp) {
        super(MessageType.BUILDING_CONSTRUCTED, timestamp);
        this.buildingId = buildingId;
        this.ownerId = ownerId;
        this.buildingType = buildingType;
        this.col = col;
        this.row = row;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId);
        payload.put("ownerId", ownerId);
        payload.put("buildingType", buildingType);
        payload.put("col", col);
        payload.put("row", row);
        return payload;
    }

    static BuildingConstructedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int buildingId = ((Number) payload.get("buildingId")).intValue();
        String ownerId = (String) payload.get("ownerId");
        String buildingType = (String) payload.get("buildingType");
        int col = ((Number) payload.get("col")).intValue();
        int row = ((Number) payload.get("row")).intValue();
        return new BuildingConstructedMessage(buildingId, ownerId, buildingType, col, row, timestamp);
    }
}
