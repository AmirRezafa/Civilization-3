package controller.events;

import java.util.List;

public class CombatResultEvent {
    private final List<Integer> attackerRolls;
    private final List<Integer> defenderRolls;
    private final List<Integer> defenderRawRolls;
    private final int attackerHits;
    private final int defenderHits;
    private final boolean defenderHadWall;

    public CombatResultEvent(List<Integer> attackerRolls, List<Integer> defenderRolls, List<Integer> defenderRawRolls,
                              int attackerHits, int defenderHits, boolean defenderHadWall) {
        this.attackerRolls = attackerRolls;
        this.defenderRolls = defenderRolls;
        this.defenderRawRolls = defenderRawRolls;
        this.attackerHits = attackerHits;
        this.defenderHits = defenderHits;
        this.defenderHadWall = defenderHadWall;
    }

    public List<Integer> getAttackerRolls() {
        return attackerRolls;
    }

    public List<Integer> getDefenderRolls() {
        return defenderRolls;
    }

    public List<Integer> getDefenderRawRolls() {
        return defenderRawRolls;
    }

    public int getAttackerHits() {
        return attackerHits;
    }

    public int getDefenderHits() {
        return defenderHits;
    }

    public boolean isDefenderHadWall() {
        return defenderHadWall;
    }
}
