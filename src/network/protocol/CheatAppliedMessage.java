package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class CheatAppliedMessage extends Message {
    private final String command;
    private final String detail;

    public CheatAppliedMessage(String command, String detail) {
        super(MessageType.CHEAT_APPLIED);
        this.command = command;
        this.detail = detail;
    }

    private CheatAppliedMessage(String command, String detail, long timestamp) {
        super(MessageType.CHEAT_APPLIED, timestamp);
        this.command = command;
        this.detail = detail;
    }

    public String getCommand() {
        return command;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", command);
        payload.put("detail", detail);
        return payload;
    }

    static CheatAppliedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new CheatAppliedMessage((String) payload.get("command"), (String) payload.get("detail"), timestamp);
    }
}
