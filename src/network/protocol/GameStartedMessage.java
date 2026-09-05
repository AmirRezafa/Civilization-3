package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class GameStartedMessage extends Message {
    public GameStartedMessage() {
        super(MessageType.GAME_STARTED);
    }

    private GameStartedMessage(long timestamp) {
        super(MessageType.GAME_STARTED, timestamp);
    }

    @Override
    public Map<String, Object> toPayload() {
        return new LinkedHashMap<>();
    }

    static GameStartedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new GameStartedMessage(timestamp);
    }
}
