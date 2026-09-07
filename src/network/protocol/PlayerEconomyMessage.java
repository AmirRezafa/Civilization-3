package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayerEconomyMessage extends Message {
    private final Map<String, Integer> resources;
    private final List<String> techs;
    private final Map<String, Integer> items;

    public PlayerEconomyMessage(Map<String, Integer> resources, List<String> techs, Map<String, Integer> items) {
        super(MessageType.PLAYER_ECONOMY);
        this.resources = resources;
        this.techs = techs;
        this.items = items;
    }

    private PlayerEconomyMessage(Map<String, Integer> resources, List<String> techs, Map<String, Integer> items, long timestamp) {
        super(MessageType.PLAYER_ECONOMY, timestamp);
        this.resources = resources;
        this.techs = techs;
        this.items = items;
    }

    public Map<String, Integer> getResources() {
        return resources;
    }

    public List<String> getTechs() {
        return techs;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resources", resources);
        payload.put("techs", techs);
        payload.put("items", items);
        return payload;
    }

    @SuppressWarnings("unchecked")
    static PlayerEconomyMessage fromPayload(Map<String, Object> payload, long timestamp) {
        Map<String, Integer> resources = new LinkedHashMap<>();
        if (payload.get("resources") instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    resources.put((String) entry.getKey(), number.intValue());
                }
            }
        }
        List<String> techs = new ArrayList<>();
        if (payload.get("techs") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) {
                    techs.add(s);
                }
            }
        }
        Map<String, Integer> items = new LinkedHashMap<>();
        if (payload.get("items") instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number number) {
                    items.put((String) entry.getKey(), number.intValue());
                }
            }
        }
        return new PlayerEconomyMessage(resources, techs, items, timestamp);
    }
}
