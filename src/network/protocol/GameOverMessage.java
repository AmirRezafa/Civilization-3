package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class GameOverMessage extends Message {
    private final String winnerId;
    private final String winnerName;

    public GameOverMessage(String winnerId, String winnerName) {
        super(MessageType.GAME_OVER);
        this.winnerId = winnerId;
        this.winnerName = winnerName;
    }

    private GameOverMessage(String winnerId, String winnerName, long timestamp) {
        super(MessageType.GAME_OVER, timestamp);
        this.winnerId = winnerId;
        this.winnerName = winnerName;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("winnerId", winnerId);
        payload.put("winnerName", winnerName);
        return payload;
    }

    static GameOverMessage fromPayload(Map<String, Object> payload, long timestamp) {
        return new GameOverMessage((String) payload.get("winnerId"), (String) payload.get("winnerName"), timestamp);
    }
}
