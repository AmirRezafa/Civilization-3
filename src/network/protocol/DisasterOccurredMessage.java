package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DisasterOccurredMessage extends Message {
    private final String affectedPlayerId;
    private final int centerCol;
    private final int centerRow;
    private final int radius;
    private final List<Integer> killedUnitIds;

    public DisasterOccurredMessage(String affectedPlayerId, int centerCol, int centerRow, int radius, List<Integer> killedUnitIds) {
        super(MessageType.DISASTER_OCCURRED);
        this.affectedPlayerId = affectedPlayerId;
        this.centerCol = centerCol;
        this.centerRow = centerRow;
        this.radius = radius;
        this.killedUnitIds = killedUnitIds;
    }

    private DisasterOccurredMessage(String affectedPlayerId, int centerCol, int centerRow, int radius, List<Integer> killedUnitIds, long timestamp) {
        super(MessageType.DISASTER_OCCURRED, timestamp);
        this.affectedPlayerId = affectedPlayerId;
        this.centerCol = centerCol;
        this.centerRow = centerRow;
        this.radius = radius;
        this.killedUnitIds = killedUnitIds;
    }

    public String getAffectedPlayerId() {
        return affectedPlayerId;
    }

    public int getCenterCol() {
        return centerCol;
    }

    public int getCenterRow() {
        return centerRow;
    }

    public int getRadius() {
        return radius;
    }

    public List<Integer> getKilledUnitIds() {
        return killedUnitIds;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("affectedPlayerId", affectedPlayerId);
        payload.put("centerCol", centerCol);
        payload.put("centerRow", centerRow);
        payload.put("radius", radius);
        payload.put("killedUnitIds", killedUnitIds);
        return payload;
    }

    static DisasterOccurredMessage fromPayload(Map<String, Object> payload, long timestamp) {
        String affectedPlayerId = (String) payload.get("affectedPlayerId");
        int centerCol = ((Number) payload.get("centerCol")).intValue();
        int centerRow = ((Number) payload.get("centerRow")).intValue();
        int radius = ((Number) payload.get("radius")).intValue();
        List<Integer> killedUnitIds = new ArrayList<>();
        if (payload.get("killedUnitIds") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    killedUnitIds.add(number.intValue());
                }
            }
        }
        return new DisasterOccurredMessage(affectedPlayerId, centerCol, centerRow, radius, killedUnitIds, timestamp);
    }
}
