package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class SaveGameRequest extends Message {
    public SaveGameRequest() {
        super(MessageType.SAVE_GAME_REQUEST);
    }

    private SaveGameRequest(long timestamp) {
        super(MessageType.SAVE_GAME_REQUEST, timestamp);
    }

    @Override
    public Map<String, Object> toPayload() {
        return new LinkedHashMap<>();
    }

    static SaveGameRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new SaveGameRequest(timestamp);
    }
}
