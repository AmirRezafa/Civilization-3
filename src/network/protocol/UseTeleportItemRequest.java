package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class UseTeleportItemRequest extends Message {
    private final int unitId;
    private final int targetCol;
    private final int targetRow;

    public UseTeleportItemRequest(int unitId, int targetCol, int targetRow) {
        super(MessageType.USE_TELEPORT_ITEM_REQUEST);
        this.unitId = unitId;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
    }

    private UseTeleportItemRequest(int unitId, int targetCol, int targetRow, long timestamp) {
        super(MessageType.USE_TELEPORT_ITEM_REQUEST, timestamp);
        this.unitId = unitId;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
    }

    public int getUnitId() {
        return unitId;
    }

    public int getTargetCol() {
        return targetCol;
    }

    public int getTargetRow() {
        return targetRow;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unitId", unitId);
        payload.put("targetCol", targetCol);
        payload.put("targetRow", targetRow);
        return payload;
    }

    static UseTeleportItemRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int unitId = ((Number) payload.get("unitId")).intValue();
        int targetCol = ((Number) payload.get("targetCol")).intValue();
        int targetRow = ((Number) payload.get("targetRow")).intValue();
        return new UseTeleportItemRequest(unitId, targetCol, targetRow, timestamp);
    }
}
