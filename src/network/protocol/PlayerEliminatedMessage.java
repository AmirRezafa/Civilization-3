package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerEliminatedMessage extends Message {
    private final String playerId;

    public PlayerEliminatedMessage(String playerId) {
        super(MessageType.PLAYER_ELIMINATED);
        this.playerId = playerId;
    }

    private PlayerEliminatedMessage(String playerId, long timestamp) {
        super(MessageType.PLAYER_ELIMINATED, timestamp);
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", playerId);
        return payload;
    }

    static PlayerEliminatedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new PlayerEliminatedMessage((String) payload.get("playerId"), timestamp);
    }
}
