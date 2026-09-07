package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemUsedMessage extends Message {
    private final String itemType;
    private final int unitId;
    private final String detail;

    public ItemUsedMessage(String itemType, int unitId, String detail) {
        super(MessageType.ITEM_USED);
        this.itemType = itemType;
        this.unitId = unitId;
        this.detail = detail;
    }

    private ItemUsedMessage(String itemType, int unitId, String detail, long timestamp) {
        super(MessageType.ITEM_USED, timestamp);
        this.itemType = itemType;
        this.unitId = unitId;
        this.detail = detail;
    }

    public String getItemType() {
        return itemType;
    }

    public int getUnitId() {
        return unitId;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("itemType", itemType);
        payload.put("unitId", unitId);
        payload.put("detail", detail);
        return payload;
    }

    static ItemUsedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        String itemType = (String) payload.get("itemType");
        int unitId = ((Number) payload.get("unitId")).intValue();
        String detail = (String) payload.get("detail");
        return new ItemUsedMessage(itemType, unitId, detail, timestamp);
    }
}
