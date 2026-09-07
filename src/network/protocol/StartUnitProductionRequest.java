package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class StartUnitProductionRequest extends Message {
    private final int buildingId;
    private final String unitType;

    public StartUnitProductionRequest(int buildingId, String unitType) {
        super(MessageType.START_UNIT_PRODUCTION_REQUEST);
        this.buildingId = buildingId;
        this.unitType = unitType;
    }

    private StartUnitProductionRequest(int buildingId, String unitType, long timestamp) {
        super(MessageType.START_UNIT_PRODUCTION_REQUEST, timestamp);
        this.buildingId = buildingId;
        this.unitType = unitType;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public String getUnitType() {
        return unitType;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId);
        payload.put("unitType", unitType);
        return payload;
    }

    static StartUnitProductionRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int buildingId = ((Number) payload.get("buildingId")).intValue();
        String unitType = (String) payload.get("unitType");
        return new StartUnitProductionRequest(buildingId, unitType, timestamp);
    }
}
