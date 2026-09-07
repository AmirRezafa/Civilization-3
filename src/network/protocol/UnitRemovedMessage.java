package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class UnitRemovedMessage extends Message {
    private final int unitId;
    private final String reason;

    public UnitRemovedMessage(int unitId, String reason) {
        super(MessageType.UNIT_REMOVED);
        this.unitId = unitId;
        this.reason = reason;
    }

    private UnitRemovedMessage(int unitId, String reason, long timestamp) {
        super(MessageType.UNIT_REMOVED, timestamp);
        this.unitId = unitId;
        this.reason = reason;
    }

    public int getUnitId() {
        return unitId;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("unitId", unitId);
        payload.put("reason", reason);
        return payload;
    }

    static UnitRemovedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int unitId = ((Number) payload.get("unitId")).intValue();
        String reason = (String) payload.get("reason");
        return new UnitRemovedMessage(unitId, reason, timestamp);
    }
}
