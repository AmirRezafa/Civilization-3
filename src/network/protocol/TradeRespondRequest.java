package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class TradeRespondRequest extends Message {
    private final int offerId;
    private final boolean accept;

    public TradeRespondRequest(int offerId, boolean accept) {
        super(MessageType.TRADE_RESPOND_REQUEST);
        this.offerId = offerId;
        this.accept = accept;
    }

    private TradeRespondRequest(int offerId, boolean accept, long timestamp) {
        super(MessageType.TRADE_RESPOND_REQUEST, timestamp);
        this.offerId = offerId;
        this.accept = accept;
    }

    public int getOfferId() {
        return offerId;
    }

    public boolean isAccept() {
        return accept;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("offerId", offerId);
        payload.put("accept", accept);
        return payload;
    }

    static TradeRespondRequest fromPayload(Map<String, Object> payload, long timestamp) {
        int offerId = ((Number) payload.get("offerId")).intValue();
        boolean accept = Boolean.TRUE.equals(payload.get("accept"));
        return new TradeRespondRequest(offerId, accept, timestamp);
    }
}
