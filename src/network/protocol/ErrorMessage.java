package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ErrorMessage extends Message {
    private final String errorText;

    public ErrorMessage(String errorText) {
        super(MessageType.ERROR);
        this.errorText = errorText;
    }

    private ErrorMessage(String errorText, long timestamp) {
        super(MessageType.ERROR, timestamp);
        this.errorText = errorText;
    }

    public String getErrorText() {
        return errorText;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("errorText", errorText);
        return payload;
    }

    static ErrorMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new ErrorMessage((String) payload.get("errorText"), timestamp);
    }
}
