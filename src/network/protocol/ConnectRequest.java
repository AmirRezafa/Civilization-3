package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConnectRequest extends Message {
    private final String playerName;

    public ConnectRequest(String playerName) {
        super(MessageType.CONNECT_REQUEST);
        this.playerName = playerName;
    }

    private ConnectRequest(String playerName, long timestamp) {
        super(MessageType.CONNECT_REQUEST, timestamp);
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerName", playerName);
        return payload;
    }

    static ConnectRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new ConnectRequest((String) payload.get("playerName"), timestamp);
    }
}
