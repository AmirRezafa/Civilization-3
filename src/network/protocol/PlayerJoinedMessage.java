package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerJoinedMessage extends Message {
    private final String playerId;
    private final String playerName;

    public PlayerJoinedMessage(String playerId, String playerName) {
        super(MessageType.PLAYER_JOINED);
        this.playerId = playerId;
        this.playerName = playerName;
    }

    private PlayerJoinedMessage(String playerId, String playerName, long timestamp) {
        super(MessageType.PLAYER_JOINED, timestamp);
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

    static PlayerJoinedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new PlayerJoinedMessage((String) payload.get("playerId"), (String) payload.get("playerName"), timestamp);
    }
}
