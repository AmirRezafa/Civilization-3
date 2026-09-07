package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProductionCompletedMessage extends Message {
    private final int buildingId;
    private final String kind;
    private final String target;
    private final int createdUnitId;
    private final int createdUnitCol;
    private final int createdUnitRow;
    private final String ownerId;

    public ProductionCompletedMessage(int buildingId, String kind, String target, int createdUnitId,
                                       int createdUnitCol, int createdUnitRow, String ownerId) {
        super(MessageType.PRODUCTION_COMPLETED);
        this.buildingId = buildingId;
        this.kind = kind;
        this.target = target;
        this.createdUnitId = createdUnitId;
        this.createdUnitCol = createdUnitCol;
        this.createdUnitRow = createdUnitRow;
        this.ownerId = ownerId;
    }

    private ProductionCompletedMessage(int buildingId, String kind, String target, int createdUnitId,
                                        int createdUnitCol, int createdUnitRow, String ownerId, long timestamp) {
        super(MessageType.PRODUCTION_COMPLETED, timestamp);
        this.buildingId = buildingId;
        this.kind = kind;
        this.target = target;
        this.createdUnitId = createdUnitId;
        this.createdUnitCol = createdUnitCol;
        this.createdUnitRow = createdUnitRow;
        this.ownerId = ownerId;
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

    public int getCreatedUnitId() {
        return createdUnitId;
    }

    public int getCreatedUnitCol() {
        return createdUnitCol;
    }

    public int getCreatedUnitRow() {
        return createdUnitRow;
    }

    public String getOwnerId() {
        return ownerId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buildingId", buildingId);
        payload.put("kind", kind);
        payload.put("target", target);
        payload.put("createdUnitId", createdUnitId);
        payload.put("createdUnitCol", createdUnitCol);
        payload.put("createdUnitRow", createdUnitRow);
        payload.put("ownerId", ownerId);
        return payload;
    }

    static ProductionCompletedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int buildingId = ((Number) payload.get("buildingId")).intValue();
        String kind = (String) payload.get("kind");
        String target = (String) payload.get("target");
        int createdUnitId = ((Number) payload.get("createdUnitId")).intValue();
        int createdUnitCol = ((Number) payload.get("createdUnitCol")).intValue();
        int createdUnitRow = ((Number) payload.get("createdUnitRow")).intValue();
        String ownerId = (String) payload.get("ownerId");
        return new ProductionCompletedMessage(buildingId, kind, target, createdUnitId, createdUnitCol, createdUnitRow, ownerId, timestamp);
    }
}
