package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class MoveUnitRequest extends Message {
    private final int unitId;
    private final int targetCol;
    private final int targetRow;

    public MoveUnitRequest(int unitId, int targetCol, int targetRow) {
        super(MessageType.MOVE_UNIT_REQUEST);
        this.unitId = unitId;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
    }

    private MoveUnitRequest(int unitId, int targetCol, int targetRow, long timestamp) {
        super(MessageType.MOVE_UNIT_REQUEST, timestamp);
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

    static MoveUnitRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int unitId = ((Number) payload.get("unitId")).intValue();
        int targetCol = ((Number) payload.get("targetCol")).intValue();
        int targetRow = ((Number) payload.get("targetRow")).intValue();
        return new MoveUnitRequest(unitId, targetCol, targetRow, timestamp);
    }
}
