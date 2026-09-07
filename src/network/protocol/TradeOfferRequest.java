package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class TradeOfferRequest extends Message {
    private final String targetPlayerId;
    private final Map<String, Integer> offering;
    private final Map<String, Integer> requesting;

    public TradeOfferRequest(String targetPlayerId, Map<String, Integer> offering, Map<String, Integer> requesting) {
        super(MessageType.TRADE_OFFER_REQUEST);
        this.targetPlayerId = targetPlayerId;
        this.offering = offering;
        this.requesting = requesting;
    }

    private TradeOfferRequest(String targetPlayerId, Map<String, Integer> offering, Map<String, Integer> requesting, long timestamp) {
        super(MessageType.TRADE_OFFER_REQUEST, timestamp);
        this.targetPlayerId = targetPlayerId;
        this.offering = offering;
        this.requesting = requesting;
    }

    public String getTargetPlayerId() {
        return targetPlayerId;
    }

    public Map<String, Integer> getOffering() {
        return offering;
    }

    public Map<String, Integer> getRequesting() {
        return requesting;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetPlayerId", targetPlayerId);
        payload.put("offering", offering);
        payload.put("requesting", requesting);
        return payload;
    }

    @SuppressWarnings("unchecked")
    static TradeOfferRequest fromPayload(Map<String, Object> payload, long timestamp) {
        String targetPlayerId = (String) payload.get("targetPlayerId");
        Map<String, Integer> offering = toIntMap(payload.get("offering"));
        Map<String, Integer> requesting = toIntMap(payload.get("requesting"));
        return new TradeOfferRequest(targetPlayerId, offering, requesting, timestamp);
    }

    private static Map<String, Integer> toIntMap(Object raw) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    result.put((String) entry.getKey(), number.intValue());
                }
            }
        }
        return result;
    }
}
