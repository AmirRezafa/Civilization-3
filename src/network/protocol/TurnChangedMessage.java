package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class TurnChangedMessage extends Message {
    private final String currentPlayerId;
    private final String currentPlayerName;

    public TurnChangedMessage(String currentPlayerId, String currentPlayerName) {
        super(MessageType.TURN_CHANGED);
        this.currentPlayerId = currentPlayerId;
        this.currentPlayerName = currentPlayerName;
    }

    private TurnChangedMessage(String currentPlayerId, String currentPlayerName, long timestamp) {
        super(MessageType.TURN_CHANGED, timestamp);
        this.currentPlayerId = currentPlayerId;
        this.currentPlayerName = currentPlayerName;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("currentPlayerId", currentPlayerId);
        payload.put("currentPlayerName", currentPlayerName);
        return payload;
    }

    static TurnChangedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new TurnChangedMessage((String) payload.get("currentPlayerId"), (String) payload.get("currentPlayerName"), timestamp);
    }
}
