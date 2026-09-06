package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeclareWarRequest extends Message {
    private final String targetPlayerId;

    public DeclareWarRequest(String targetPlayerId) {
        super(MessageType.DECLARE_WAR_REQUEST);
        this.targetPlayerId = targetPlayerId;
    }

    private DeclareWarRequest(String targetPlayerId, long timestamp) {
        super(MessageType.DECLARE_WAR_REQUEST, timestamp);
        this.targetPlayerId = targetPlayerId;
    }

    public String getTargetPlayerId() {
        return targetPlayerId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetPlayerId", targetPlayerId);
        return payload;
    }

    static DeclareWarRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new DeclareWarRequest((String) payload.get("targetPlayerId"), timestamp);
    }
}
