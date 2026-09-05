package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class SetReadyRequest extends Message {
    private final boolean ready;

    public SetReadyRequest(boolean ready) {
        super(MessageType.SET_READY_REQUEST);
        this.ready = ready;
    }

    private SetReadyRequest(boolean ready, long timestamp) {
        super(MessageType.SET_READY_REQUEST, timestamp);
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ready", ready);
        return payload;
    }

    static SetReadyRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new SetReadyRequest(Boolean.TRUE.equals(payload.get("ready")), timestamp);
    }
}
