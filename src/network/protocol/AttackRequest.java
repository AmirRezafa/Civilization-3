package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class AttackRequest extends Message {
    private final int attackerUnitId;
    private final int targetCol;
    private final int targetRow;

    public AttackRequest(int attackerUnitId, int targetCol, int targetRow) {
        super(MessageType.ATTACK_REQUEST);
        this.attackerUnitId = attackerUnitId;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
    }

    private AttackRequest(int attackerUnitId, int targetCol, int targetRow, long timestamp) {
        super(MessageType.ATTACK_REQUEST, timestamp);
        this.attackerUnitId = attackerUnitId;
        this.targetCol = targetCol;
        this.targetRow = targetRow;
    }

    public int getAttackerUnitId() {
        return attackerUnitId;
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
        payload.put("attackerUnitId", attackerUnitId);
        payload.put("targetCol", targetCol);
        payload.put("targetRow", targetRow);
        return payload;
    }

    static AttackRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int attackerUnitId = ((Number) payload.get("attackerUnitId")).intValue();
        int targetCol = ((Number) payload.get("targetCol")).intValue();
        int targetRow = ((Number) payload.get("targetRow")).intValue();
        return new AttackRequest(attackerUnitId, targetCol, targetRow, timestamp);
    }
}
