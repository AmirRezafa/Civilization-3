package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerLeftMessage extends Message {
    private final String playerId;
    private final String playerName;

    public PlayerLeftMessage(String playerId, String playerName) {
        super(MessageType.PLAYER_LEFT);
        this.playerId = playerId;
        this.playerName = playerName;
    }

    private PlayerLeftMessage(String playerId, String playerName, long timestamp) {
        super(MessageType.PLAYER_LEFT, timestamp);
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", playerId);
        payload.put("playerName", playerName);
        return payload;
    }

    static PlayerLeftMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new PlayerLeftMessage((String) payload.get("playerId"), (String) payload.get("playerName"), timestamp);
    }
}
