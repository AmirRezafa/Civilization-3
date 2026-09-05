package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class EndTurnRequest extends Message {
    public EndTurnRequest() {
        super(MessageType.END_TURN_REQUEST);
    }

    private EndTurnRequest(long timestamp) {
        super(MessageType.END_TURN_REQUEST, timestamp);
    }

    @Override
    public Map<String, Object> toPayload() {
        return new LinkedHashMap<>();
    }

    static EndTurnRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new EndTurnRequest(timestamp);
    }
}
