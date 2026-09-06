package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiplomacyChangedMessage extends Message {
    private final String playerAId;
    private final String playerAName;
    private final String playerBId;
    private final String playerBName;
    private final String status;

    public DiplomacyChangedMessage(String playerAId, String playerAName, String playerBId, String playerBName, String status) {
        super(MessageType.DIPLOMACY_CHANGED);
        this.playerAId = playerAId;
        this.playerAName = playerAName;
        this.playerBId = playerBId;
        this.playerBName = playerBName;
        this.status = status;
    }

    private DiplomacyChangedMessage(String playerAId, String playerAName, String playerBId, String playerBName, String status, long timestamp) {
        super(MessageType.DIPLOMACY_CHANGED, timestamp);
        this.playerAId = playerAId;
        this.playerAName = playerAName;
        this.playerBId = playerBId;
        this.playerBName = playerBName;
        this.status = status;
    }

    public String getPlayerAId() {
        return playerAId;
    }

    public String getPlayerAName() {
        return playerAName;
    }

    public String getPlayerBId() {
        return playerBId;
    }

    public String getPlayerBName() {
        return playerBName;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerAId", playerAId);
        payload.put("playerAName", playerAName);
        payload.put("playerBId", playerBId);
        payload.put("playerBName", playerBName);
        payload.put("status", status);
        return payload;
    }

    static DiplomacyChangedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new DiplomacyChangedMessage(
                (String) payload.get("playerAId"),
                (String) payload.get("playerAName"),
                (String) payload.get("playerBId"),
                (String) payload.get("playerBName"),
                (String) payload.get("status"),
                timestamp);
    }
}
