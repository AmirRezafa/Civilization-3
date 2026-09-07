package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public class TradeOfferResolvedMessage extends Message {
    private final int offerId;
    private final boolean accepted;

    public TradeOfferResolvedMessage(int offerId, boolean accepted) {
        super(MessageType.TRADE_OFFER_RESOLVED);
        this.offerId = offerId;
        this.accepted = accepted;
    }

    private TradeOfferResolvedMessage(int offerId, boolean accepted, long timestamp) {
        super(MessageType.TRADE_OFFER_RESOLVED, timestamp);
        this.offerId = offerId;
        this.accepted = accepted;
    }

    public int getOfferId() {
        return offerId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("offerId", offerId);
        payload.put("accepted", accepted);
        return payload;
    }

    static TradeOfferResolvedMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int offerId = ((Number) payload.get("offerId")).intValue();
        boolean accepted = Boolean.TRUE.equals(payload.get("accepted"));
        return new TradeOfferResolvedMessage(offerId, accepted, timestamp);
    }
}
