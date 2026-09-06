package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class UnitMovedMessage extends Message {
    private final int unitId;
    private final int newCol;
    private final int newRow;
    private final int remainingAP;

    public UnitMovedMessage(int unitId, int newCol, int newRow, int remainingAP) {
        super(MessageType.UNIT_MOVED);
        this.unitId = unitId;
        this.newCol = newCol;
        this.newRow = newRow;
        this.remainingAP = remainingAP;
    }

    private UnitMovedMessage(int unitId, int newCol, int newRow, int remainingAP, long timestamp) {
        super(MessageType.UNIT_MOVED, timestamp);
        this.unitId = unitId;
        this.newCol = newCol;
        this.newRow = newRow;
        this.remainingAP = remainingAP;
    }

    public int getUnitId() {
        return unitId;
    }

    public int getNewCol() {
        return newCol;
    }

    public int getNewRow() {
        return newRow;
    }

    public int getRemainingAP() {
        return remainingAP;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unitId", unitId);
        payload.put("newCol", newCol);
        payload.put("newRow", newRow);
        payload.put("remainingAP", remainingAP);
        return payload;
    }

    static UnitMovedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int unitId = ((Number) payload.get("unitId")).intValue();
        int newCol = ((Number) payload.get("newCol")).intValue();
        int newRow = ((Number) payload.get("newRow")).intValue();
        int remainingAP = ((Number) payload.get("remainingAP")).intValue();
        return new UnitMovedMessage(unitId, newCol, newRow, remainingAP, timestamp);
    }
}
