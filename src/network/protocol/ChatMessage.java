package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChatMessage extends Message {
    private final String senderName;
    private final String text;

    public ChatMessage(String senderName, String text) {
        super(MessageType.CHAT_MESSAGE);
        this.senderName = senderName;
        this.text = text;
    }

    private ChatMessage(String senderName, String text, long timestamp) {
        super(MessageType.CHAT_MESSAGE, timestamp);
        this.senderName = senderName;
        this.text = text;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("senderName", senderName);
        payload.put("text", text);
        return payload;
    }

    static ChatMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new ChatMessage((String) payload.get("senderName"), (String) payload.get("text"), timestamp);
    }
}
