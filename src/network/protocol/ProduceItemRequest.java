package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProduceItemRequest extends Message {
    private final int apothecaryBuildingId;
    private final String itemType;

    public ProduceItemRequest(int apothecaryBuildingId, String itemType) {
        super(MessageType.PRODUCE_ITEM_REQUEST);
        this.apothecaryBuildingId = apothecaryBuildingId;
        this.itemType = itemType;
    }

    private ProduceItemRequest(int apothecaryBuildingId, String itemType, long timestamp) {
        super(MessageType.PRODUCE_ITEM_REQUEST, timestamp);
        this.apothecaryBuildingId = apothecaryBuildingId;
        this.itemType = itemType;
    }

    public int getApothecaryBuildingId() {
        return apothecaryBuildingId;
    }

    public String getItemType() {
        return itemType;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("apothecaryBuildingId", apothecaryBuildingId);
        payload.put("itemType", itemType);
        return payload;
    }

    static ProduceItemRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int apothecaryBuildingId = ((Number) payload.get("apothecaryBuildingId")).intValue();
        String itemType = (String) payload.get("itemType");
        return new ProduceItemRequest(apothecaryBuildingId, itemType, timestamp);
    }
}
