package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class StartGameRequest extends Message {
    public StartGameRequest() {
        super(MessageType.START_GAME_REQUEST);
    }

    private StartGameRequest(long timestamp) {
        super(MessageType.START_GAME_REQUEST, timestamp);
    }

    @Override
    public Map<String, Object> toPayload() {
        return new LinkedHashMap<>();
    }

    static StartGameRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new StartGameRequest(timestamp);
    }
}
