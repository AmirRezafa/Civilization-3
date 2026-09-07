package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class StartUpgradeRequest extends Message {
    private final int buildingId;

    public StartUpgradeRequest(int buildingId) {
        super(MessageType.START_UPGRADE_REQUEST);
        this.buildingId = buildingId;
    }

    private StartUpgradeRequest(int buildingId, long timestamp) {
        super(MessageType.START_UPGRADE_REQUEST, timestamp);
        this.buildingId = buildingId;
    }

    public int getBuildingId() {
        return buildingId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId);
        return payload;
    }

    static StartUpgradeRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int buildingId = ((Number) payload.get("buildingId")).intValue();
        return new StartUpgradeRequest(buildingId, timestamp);
    }
}
