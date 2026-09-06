package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LobbyStateMessage extends Message {
    private final List<PlayerEntry> players;
    private final String hostPlayerId;
    private final String selectedMapName;

    public LobbyStateMessage(List<PlayerEntry> players, String hostPlayerId, String selectedMapName) {
        super(MessageType.LOBBY_STATE);
        this.players = players;
        this.hostPlayerId = hostPlayerId;
        this.selectedMapName = selectedMapName;
    }

    private LobbyStateMessage(List<PlayerEntry> players, String hostPlayerId, String selectedMapName, long timestamp) {
        super(MessageType.LOBBY_STATE, timestamp);
        this.players = players;
        this.hostPlayerId = hostPlayerId;
        this.selectedMapName = selectedMapName;
    }

    public List<PlayerEntry> getPlayers() {
        return players;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public String getSelectedMapName() {
        return selectedMapName;
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
        payload.put("selectedMapName", selectedMapName);
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
        String selectedMapName = (String) payload.get("selectedMapName");
        return new LobbyStateMessage(players, hostPlayerId, selectedMapName, timestamp);
    }

    public record PlayerEntry(String playerId, String playerName, boolean ready) {
    }
}
