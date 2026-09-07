package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class UseMobilityItemRequest extends Message {
    private final int unitId;

    public UseMobilityItemRequest(int unitId) {
        super(MessageType.USE_MOBILITY_ITEM_REQUEST);
        this.unitId = unitId;
    }

    private UseMobilityItemRequest(int unitId, long timestamp) {
        super(MessageType.USE_MOBILITY_ITEM_REQUEST, timestamp);
        this.unitId = unitId;
    }

    public int getUnitId() {
        return unitId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unitId", unitId);
        return payload;
    }

    static UseMobilityItemRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int unitId = ((Number) payload.get("unitId")).intValue();
        return new UseMobilityItemRequest(unitId, timestamp);
    }
}
