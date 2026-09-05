package network.protocol;

import java.util.Map;

public abstract class Message {
    private final MessageType type;
    private final long timestamp;

    protected Message(MessageType type) {
        this(type, System.currentTimeMillis());
    }

    protected Message(MessageType type, long timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract Map<String, Object> toPayload();
}
