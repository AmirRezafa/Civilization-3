package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class StartTechResearchRequest extends Message {
    private final int buildingId;
    private final String techType;

    public StartTechResearchRequest(int buildingId, String techType) {
        super(MessageType.START_TECH_RESEARCH_REQUEST);
        this.buildingId = buildingId;
        this.techType = techType;
    }

    private StartTechResearchRequest(int buildingId, String techType, long timestamp) {
        super(MessageType.START_TECH_RESEARCH_REQUEST, timestamp);
        this.buildingId = buildingId;
        this.techType = techType;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public String getTechType() {
        return techType;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId);
        payload.put("techType", techType);
        return payload;
    }

    static StartTechResearchRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int buildingId = ((Number) payload.get("buildingId")).intValue();
        String techType = (String) payload.get("techType");
        return new StartTechResearchRequest(buildingId, techType, timestamp);
    }
}
