package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LobbyStateMessage extends Message {
    private final List<PlayerEntry> players;
    private final String hostPlayerId;

    public LobbyStateMessage(List<PlayerEntry> players, String hostPlayerId) {
        super(MessageType.LOBBY_STATE);
        this.players = players;
        this.hostPlayerId = hostPlayerId;
    }

    private LobbyStateMessage(List<PlayerEntry> players, String hostPlayerId, long timestamp) {
        super(MessageType.LOBBY_STATE, timestamp);
        this.players = players;
        this.hostPlayerId = hostPlayerId;
    }

    public List<PlayerEntry> getPlayers() {
        return players;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    @Override
    public Map<String, Object> toPayload() {
        List<Object> encodedPlayers = new ArrayList<>();
        for (PlayerEntry entry : players) {
            Map<String, Object> entryMap = new LinkedHashMap<>();
            entryMap.put("playerId", entry.playerId());
            entryMap.put("playerName", entry.playerName());
            entryMap.put("ready", entry.ready());
            encodedPlayers.add(entryMap);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("players", encodedPlayers);
        payload.put("hostPlayerId", hostPlayerId);
        return payload;
    }

    static LobbyStateMessage fromPayload(Map<String, Object> payload, long timestamp) {
        List<PlayerEntry> players = new ArrayList<>();
        Object rawPlayers = payload.get("players");
        if (rawPlayers instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String playerId = (String) map.get("playerId");
                    String playerName = (String) map.get("playerName");
                    boolean ready = Boolean.TRUE.equals(map.get("ready"));
                    players.add(new PlayerEntry(playerId, playerName, ready));
                }
            }
        }
        String hostPlayerId = (String) payload.get("hostPlayerId");
        return new LobbyStateMessage(players, hostPlayerId, timestamp);
    }

    public record PlayerEntry(String playerId, String playerName, boolean ready) {
    }
}
