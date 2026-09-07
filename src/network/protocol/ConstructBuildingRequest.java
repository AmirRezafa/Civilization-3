package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConstructBuildingRequest extends Message {
    private final int builderUnitId;
    private final String buildingType;

    public ConstructBuildingRequest(int builderUnitId, String buildingType) {
        super(MessageType.CONSTRUCT_BUILDING_REQUEST);
        this.builderUnitId = builderUnitId;
        this.buildingType = buildingType;
    }

    private ConstructBuildingRequest(int builderUnitId, String buildingType, long timestamp) {
        super(MessageType.CONSTRUCT_BUILDING_REQUEST, timestamp);
        this.builderUnitId = builderUnitId;
        this.buildingType = buildingType;
    }

    public int getBuilderUnitId() {
        return builderUnitId;
    }

    public String getBuildingType() {
        return buildingType;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("builderUnitId", builderUnitId);
        payload.put("buildingType", buildingType);
        return payload;
    }

    static ConstructBuildingRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int builderUnitId = ((Number) payload.get("builderUnitId")).intValue();
        String buildingType = (String) payload.get("buildingType");
        return new ConstructBuildingRequest(builderUnitId, buildingType, timestamp);
    }
}
