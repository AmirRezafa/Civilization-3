package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoadGameRequest extends Message {
    public LoadGameRequest() {
        super(MessageType.LOAD_GAME_REQUEST);
    }

    private LoadGameRequest(long timestamp) {
        super(MessageType.LOAD_GAME_REQUEST, timestamp);
    }

    @Override
    public Map<String, Object> toPayload() {
        return new LinkedHashMap<>();
    }

    static LoadGameRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new LoadGameRequest(timestamp);
    }
}
