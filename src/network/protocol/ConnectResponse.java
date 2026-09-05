package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConnectResponse extends Message {
    private final boolean accepted;
    private final String playerId;
    private final String errorMessage;
    private final List<String> connectedPlayerNames;

    public ConnectResponse(boolean accepted, String playerId, String errorMessage, List<String> connectedPlayerNames) {
        super(MessageType.CONNECT_RESPONSE);
        this.accepted = accepted;
        this.playerId = playerId;
        this.errorMessage = errorMessage;
        this.connectedPlayerNames = connectedPlayerNames == null ? List.of() : connectedPlayerNames;
    }

    private ConnectResponse(boolean accepted, String playerId, String errorMessage, List<String> connectedPlayerNames, long timestamp) {
        super(MessageType.CONNECT_RESPONSE, timestamp);
        this.accepted = accepted;
        this.playerId = playerId;
        this.errorMessage = errorMessage;
        this.connectedPlayerNames = connectedPlayerNames == null ? List.of() : connectedPlayerNames;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getConnectedPlayerNames() {
        return connectedPlayerNames;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accepted", accepted);
        payload.put("playerId", playerId);
        payload.put("errorMessage", errorMessage);
        payload.put("connectedPlayerNames", connectedPlayerNames);
        return payload;
    }

    static ConnectResponse fromPayload(Map<String, Object> payload, long timestamp) {
        boolean accepted = Boolean.TRUE.equals(payload.get("accepted"));
        String playerId = (String) payload.get("playerId");
        String errorMessage = (String) payload.get("errorMessage");
        List<String> names = new ArrayList<>();
        Object rawNames = payload.get("connectedPlayerNames");
        if (rawNames instanceof List<?> list) {
            for (Object item : list) {
                names.add((String) item);
            }
        }
        return new ConnectResponse(accepted, playerId, errorMessage, names, timestamp);
    }
}
