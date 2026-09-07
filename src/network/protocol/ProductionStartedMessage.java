package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProductionStartedMessage extends Message {
    private final int buildingId;
    private final String kind;
    private final String target;
    private final int turnsRemaining;

    public ProductionStartedMessage(int buildingId, String kind, String target, int turnsRemaining) {
        super(MessageType.PRODUCTION_STARTED);
        this.buildingId = buildingId;
        this.kind = kind;
        this.target = target;
        this.turnsRemaining = turnsRemaining;
    }

    private ProductionStartedMessage(int buildingId, String kind, String target, int turnsRemaining, long timestamp) {
        super(MessageType.PRODUCTION_STARTED, timestamp);
        this.buildingId = buildingId;
        this.kind = kind;
        this.target = target;
        this.turnsRemaining = turnsRemaining;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public String getKind() {
        return kind;
    }

    public String getTarget() {
        return target;
    }

    public int getTurnsRemaining() {
        return turnsRemaining;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId);
        payload.put("kind", kind);
        payload.put("target", target);
        payload.put("turnsRemaining", turnsRemaining);
        return payload;
    }

    static ProductionStartedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int buildingId = ((Number) payload.get("buildingId")).intValue();
        String kind = (String) payload.get("kind");
        String target = (String) payload.get("target");
        int turnsRemaining = ((Number) payload.get("turnsRemaining")).intValue();
        return new ProductionStartedMessage(buildingId, kind, target, turnsRemaining, timestamp);
    }
}
