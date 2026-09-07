package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConstructTownHallRequest extends Message {
    private final int builderUnitId;

    public ConstructTownHallRequest(int builderUnitId) {
        super(MessageType.CONSTRUCT_TOWN_HALL_REQUEST);
        this.builderUnitId = builderUnitId;
    }

    private ConstructTownHallRequest(int builderUnitId, long timestamp) {
        super(MessageType.CONSTRUCT_TOWN_HALL_REQUEST, timestamp);
        this.builderUnitId = builderUnitId;
    }

    public int getBuilderUnitId() {
        return builderUnitId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("builderUnitId", builderUnitId);
        return payload;
    }

    static ConstructTownHallRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int builderUnitId = ((Number) payload.get("builderUnitId")).intValue();
        return new ConstructTownHallRequest(builderUnitId, timestamp);
    }
}
