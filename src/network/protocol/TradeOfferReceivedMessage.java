package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class TradeOfferReceivedMessage extends Message {
    private final int offerId;
    private final String fromPlayerId;
    private final String fromPlayerName;
    private final Map<String, Integer> offering;
    private final Map<String, Integer> requesting;

    public TradeOfferReceivedMessage(int offerId, String fromPlayerId, String fromPlayerName,
                                      Map<String, Integer> offering, Map<String, Integer> requesting) {
        super(MessageType.TRADE_OFFER_RECEIVED);
        this.offerId = offerId;
        this.fromPlayerId = fromPlayerId;
        this.fromPlayerName = fromPlayerName;
        this.offering = offering;
        this.requesting = requesting;
    }

    private TradeOfferReceivedMessage(int offerId, String fromPlayerId, String fromPlayerName,
                                       Map<String, Integer> offering, Map<String, Integer> requesting, long timestamp) {
        super(MessageType.TRADE_OFFER_RECEIVED, timestamp);
        this.offerId = offerId;
        this.fromPlayerId = fromPlayerId;
        this.fromPlayerName = fromPlayerName;
        this.offering = offering;
        this.requesting = requesting;
    }

    public int getOfferId() {
        return offerId;
    }

    public String getFromPlayerId() {
        return fromPlayerId;
    }

    public String getFromPlayerName() {
        return fromPlayerName;
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
        payload.put("offerId", offerId);
        payload.put("fromPlayerId", fromPlayerId);
        payload.put("fromPlayerName", fromPlayerName);
        payload.put("offering", offering);
        payload.put("requesting", requesting);
        return payload;
    }

    @SuppressWarnings("unchecked")
    static TradeOfferReceivedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int offerId = ((Number) payload.get("offerId")).intValue();
        String fromPlayerId = (String) payload.get("fromPlayerId");
        String fromPlayerName = (String) payload.get("fromPlayerName");
        Map<String, Integer> offering = toIntMap(payload.get("offering"));
        Map<String, Integer> requesting = toIntMap(payload.get("requesting"));
        return new TradeOfferReceivedMessage(offerId, fromPlayerId, fromPlayerName, offering, requesting, timestamp);
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
