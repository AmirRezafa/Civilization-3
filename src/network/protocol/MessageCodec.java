package network.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageCodec {

    private MessageCodec() {
    }

    public static String encode(Message message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", message.getType().name());
        map.put("timestamp", message.getTimestamp());
        map.putAll(message.toPayload());
        return JsonWriter.write(map);
    }

    @SuppressWarnings("unchecked")
    public static Message decode(String json) {
        Object parsed = JsonParser.parse(json);
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        Map<String, Object> map = (Map<String, Object>) parsed;
        Object typeValue = map.get("type");
        if (!(typeValue instanceof String)) {
            throw new IllegalArgumentException("Missing message type");
        }
        MessageType type = MessageType.valueOf((String) typeValue);
        long timestamp = map.get("timestamp") instanceof Number
                ? ((Number) map.get("timestamp")).longValue()
                : System.currentTimeMillis();

        return switch (type) {
            case CONNECT_REQUEST -> ConnectRequest.fromPayload(map, timestamp);
            case CONNECT_RESPONSE -> ConnectResponse.fromPayload(map, timestamp);
            case PLAYER_JOINED -> PlayerJoinedMessage.fromPayload(map, timestamp);
            case PLAYER_LEFT -> PlayerLeftMessage.fromPayload(map, timestamp);
            case ERROR -> ErrorMessage.fromPayload(map, timestamp);
            case LOBBY_STATE -> LobbyStateMessage.fromPayload(map, timestamp);
            case SET_READY_REQUEST -> SetReadyRequest.fromPayload(map, timestamp);
            case START_GAME_REQUEST -> StartGameRequest.fromPayload(map, timestamp);
            case GAME_STARTED -> GameStartedMessage.fromPayload(map, timestamp);
            case END_TURN_REQUEST -> EndTurnRequest.fromPayload(map, timestamp);
            case TURN_CHANGED -> TurnChangedMessage.fromPayload(map, timestamp);
            case CHAT_MESSAGE -> ChatMessage.fromPayload(map, timestamp);
            case DECLARE_WAR_REQUEST -> DeclareWarRequest.fromPayload(map, timestamp);
            case DIPLOMACY_CHANGED -> DiplomacyChangedMessage.fromPayload(map, timestamp);
            case SELECT_MAP_REQUEST -> SelectMapRequest.fromPayload(map, timestamp);
            case GAME_STATE_SNAPSHOT -> GameStateSnapshotMessage.fromPayload(map, timestamp);
            case MOVE_UNIT_REQUEST -> MoveUnitRequest.fromPayload(map, timestamp);
            case UNIT_MOVED -> UnitMovedMessage.fromPayload(map, timestamp);
            case ATTACK_REQUEST -> AttackRequest.fromPayload(map, timestamp);
            case COMBAT_RESULT -> CombatResultMessage.fromPayload(map, timestamp);
            case START_UNIT_PRODUCTION_REQUEST -> StartUnitProductionRequest.fromPayload(map, timestamp);
            case START_UPGRADE_REQUEST -> StartUpgradeRequest.fromPayload(map, timestamp);
            case START_TECH_RESEARCH_REQUEST -> StartTechResearchRequest.fromPayload(map, timestamp);
            case PRODUCTION_STARTED -> ProductionStartedMessage.fromPayload(map, timestamp);
            case PRODUCTION_COMPLETED -> ProductionCompletedMessage.fromPayload(map, timestamp);
            case PLAYER_ECONOMY -> PlayerEconomyMessage.fromPayload(map, timestamp);
            case PLAYER_ELIMINATED -> PlayerEliminatedMessage.fromPayload(map, timestamp);
            case GAME_OVER -> GameOverMessage.fromPayload(map, timestamp);
            case CHEAT_APPLIED -> CheatAppliedMessage.fromPayload(map, timestamp);
            case CONSTRUCT_BUILDING_REQUEST -> ConstructBuildingRequest.fromPayload(map, timestamp);
            case BUILDING_CONSTRUCTED -> BuildingConstructedMessage.fromPayload(map, timestamp);
            case UNIT_REMOVED -> UnitRemovedMessage.fromPayload(map, timestamp);
            case PRODUCE_ITEM_REQUEST -> ProduceItemRequest.fromPayload(map, timestamp);
            case USE_TELEPORT_ITEM_REQUEST -> UseTeleportItemRequest.fromPayload(map, timestamp);
            case USE_MOBILITY_ITEM_REQUEST -> UseMobilityItemRequest.fromPayload(map, timestamp);
            case USE_COMBAT_BOOST_ITEM_REQUEST -> UseCombatBoostItemRequest.fromPayload(map, timestamp);
            case ITEM_USED -> ItemUsedMessage.fromPayload(map, timestamp);
            case TRADE_OFFER_REQUEST -> TradeOfferRequest.fromPayload(map, timestamp);
            case TRADE_OFFER_RECEIVED -> TradeOfferReceivedMessage.fromPayload(map, timestamp);
            case TRADE_RESPOND_REQUEST -> TradeRespondRequest.fromPayload(map, timestamp);
            case TRADE_OFFER_RESOLVED -> TradeOfferResolvedMessage.fromPayload(map, timestamp);
            case DISASTER_OCCURRED -> DisasterOccurredMessage.fromPayload(map, timestamp);
            case CONSTRUCT_TOWN_HALL_REQUEST -> ConstructTownHallRequest.fromPayload(map, timestamp);
            case SAVE_GAME_REQUEST -> SaveGameRequest.fromPayload(map, timestamp);
            case LOAD_GAME_REQUEST -> LoadGameRequest.fromPayload(map, timestamp);
            case GAME_SAVED -> GameSavedMessage.fromPayload(map, timestamp);
        };
    }
}
