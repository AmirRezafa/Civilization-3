package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class GameSavedMessage extends Message {
    private final boolean success;
    private final String detail;

    public GameSavedMessage(boolean success, String detail) {
        super(MessageType.GAME_SAVED);
        this.success = success;
        this.detail = detail;
    }

    private GameSavedMessage(boolean success, String detail, long timestamp) {
        super(MessageType.GAME_SAVED, timestamp);
        this.success = success;
        this.detail = detail;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        payload.put("detail", detail);
        return payload;
    }

    static GameSavedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        boolean success = Boolean.TRUE.equals(payload.get("success"));
        String detail = (String) payload.get("detail");
        return new GameSavedMessage(success, detail, timestamp);
    }
}
