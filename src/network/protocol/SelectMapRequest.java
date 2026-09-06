package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class SelectMapRequest extends Message {
    private final String mapName;

    public SelectMapRequest(String mapName) {
        super(MessageType.SELECT_MAP_REQUEST);
        this.mapName = mapName;
    }

    private SelectMapRequest(String mapName, long timestamp) {
        super(MessageType.SELECT_MAP_REQUEST, timestamp);
        this.mapName = mapName;
    }

    public String getMapName() {
        return mapName;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mapName", mapName);
        return payload;
    }

    static SelectMapRequest fromPayload(Map<String, Object> payload, long timestamp) {
        return new SelectMapRequest((String) payload.get("mapName"), timestamp);
    }
}
