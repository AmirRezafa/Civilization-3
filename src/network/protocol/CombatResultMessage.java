package network.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CombatResultMessage extends Message {
    private final int targetCol;
    private final int targetRow;
    private final List<Integer> attackingUnitIds;
    private final List<Integer> attackerRolls;
    private final List<Integer> defenderRolls;
    private final int attackerHits;
    private final int defenderHits;
    private final int structureDamage;
    private final List<Integer> killedUnitIds;
    private final Integer destroyedBuildingId;

    public CombatResultMessage(int targetCol, int targetRow, List<Integer> attackingUnitIds,
                                List<Integer> attackerRolls, List<Integer> defenderRolls,
                                int attackerHits, int defenderHits, int structureDamage,
                                List<Integer> killedUnitIds, Integer destroyedBuildingId) {
        super(MessageType.COMBAT_RESULT);
        this.targetCol = targetCol;
        this.targetRow = targetRow;
        this.attackingUnitIds = attackingUnitIds;
        this.attackerRolls = attackerRolls;
        this.defenderRolls = defenderRolls;
        this.attackerHits = attackerHits;
        this.defenderHits = defenderHits;
        this.structureDamage = structureDamage;
        this.killedUnitIds = killedUnitIds;
        this.destroyedBuildingId = destroyedBuildingId;
    }

    private CombatResultMessage(int targetCol, int targetRow, List<Integer> attackingUnitIds,
                                 List<Integer> attackerRolls, List<Integer> defenderRolls,
                                 int attackerHits, int defenderHits, int structureDamage,
                                 List<Integer> killedUnitIds, Integer destroyedBuildingId, long timestamp) {
        super(MessageType.COMBAT_RESULT, timestamp);
        this.targetCol = targetCol;
        this.targetRow = targetRow;
        this.attackingUnitIds = attackingUnitIds;
        this.attackerRolls = attackerRolls;
        this.defenderRolls = defenderRolls;
        this.attackerHits = attackerHits;
        this.defenderHits = defenderHits;
        this.structureDamage = structureDamage;
        this.killedUnitIds = killedUnitIds;
        this.destroyedBuildingId = destroyedBuildingId;
    }

    public int getTargetCol() {
        return targetCol;
    }

    public int getTargetRow() {
        return targetRow;
    }

    public List<Integer> getAttackingUnitIds() {
        return attackingUnitIds;
    }

    public List<Integer> getAttackerRolls() {
        return attackerRolls;
    }

    public List<Integer> getDefenderRolls() {
        return defenderRolls;
    }

    public int getAttackerHits() {
        return attackerHits;
    }

    public int getDefenderHits() {
        return defenderHits;
    }

    public int getStructureDamage() {
        return structureDamage;
    }

    public List<Integer> getKilledUnitIds() {
        return killedUnitIds;
    }

    public Integer getDestroyedBuildingId() {
        return destroyedBuildingId;
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetCol", targetCol);
        payload.put("targetRow", targetRow);
        payload.put("attackingUnitIds", attackingUnitIds);
        payload.put("attackerRolls", attackerRolls);
        payload.put("defenderRolls", defenderRolls);
        payload.put("attackerHits", attackerHits);
        payload.put("defenderHits", defenderHits);
        payload.put("structureDamage", structureDamage);
        payload.put("killedUnitIds", killedUnitIds);
        payload.put("destroyedBuildingId", destroyedBuildingId);
        return payload;
    }

    @SuppressWarnings("unchecked")
    static CombatResultMessage fromPayload(Map<String, Object> payload, long timestamp) {
        int targetCol = ((Number) payload.get("targetCol")).intValue();
        int targetRow = ((Number) payload.get("targetRow")).intValue();
        List<Integer> attackingUnitIds = toIntList(payload.get("attackingUnitIds"));
        List<Integer> attackerRolls = toIntList(payload.get("attackerRolls"));
        List<Integer> defenderRolls = toIntList(payload.get("defenderRolls"));
        int attackerHits = ((Number) payload.get("attackerHits")).intValue();
        int defenderHits = ((Number) payload.get("defenderHits")).intValue();
        int structureDamage = ((Number) payload.get("structureDamage")).intValue();
        List<Integer> killedUnitIds = toIntList(payload.get("killedUnitIds"));
        Object rawDestroyedId = payload.get("destroyedBuildingId");
        Integer destroyedBuildingId = rawDestroyedId instanceof Number number ? number.intValue() : null;
        return new CombatResultMessage(targetCol, targetRow, attackingUnitIds, attackerRolls, defenderRolls,
                attackerHits, defenderHits, structureDamage, killedUnitIds, destroyedBuildingId, timestamp);
    }

    private static List<Integer> toIntList(Object raw) {
        List<Integer> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    result.add(number.intValue());
                }
            }
        }
        return result;
    }
}
