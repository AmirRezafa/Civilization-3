package controller.services;

import controller.events.CombatResultEvent;
import controller.events.EventBus;
import model.Building;
import model.Unit;
import model.UnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CombatService implements java.io.Serializable {
    private final Random random = new Random();

    private List<Integer> rollDice(List<Unit> units) {
        List<Integer> rolls = new ArrayList<>();
        for (Unit u : units) {
            if (u.getType() == UnitType.SWORDSMAN || u.getType() == UnitType.CAVALRY || u.getType() == UnitType.ARCHER) {
                rolls.add(random.nextInt(6) + 1);
            }
        }
        return rolls;
    }

    private List<Integer> rollRangedDice(List<Unit> units) {
        List<Integer> rolls = new ArrayList<>();
        boolean hasArcher = units.stream().anyMatch(u -> u.getType() == UnitType.ARCHER);
        if (hasArcher) rolls.add(random.nextInt(6) + 1);
        return rolls;
    }

    private void applyHits(List<Unit> units, int hitCount) {
        List<Unit> ordered = new ArrayList<>();
        for (Unit u : units) if (u.getType() == UnitType.SWORDSMAN) ordered.add(u);
        for (Unit u : units) if (u.getType() == UnitType.ARCHER) ordered.add(u);
        for (Unit u : units) if (u.getType() == UnitType.CAVALRY) ordered.add(u);

        int remainingHits = hitCount;
        for (Unit u : ordered) {
            while (remainingHits > 0 && !u.isDead()) {
                u.takeHit();
                remainingHits--;
            }
            if (remainingHits <= 0) break;
        }
    }

    public void resolveCombat(List<Unit> attackers, List<Unit> defenders, boolean defenderHasWall) {
        resolveCombat(attackers, defenders, defenderHasWall, false);
    }

    public void resolveCombat(List<Unit> attackers, List<Unit> defenders, boolean defenderHasWall, boolean isRangedAttack) {
        List<Integer> attackerRolls = isRangedAttack ? rollRangedDice(attackers) : rollDice(attackers);

        List<int[]> defenderPairs = new ArrayList<>();
        for (int raw : rollDice(defenders)) {
            int boosted = defenderHasWall ? Math.min(6, raw + 2) : raw;
            defenderPairs.add(new int[]{raw, boosted});
        }
        defenderPairs.sort((a, b) -> b[1] - a[1]);
        List<Integer> defenderRolls = new ArrayList<>();
        List<Integer> defenderRawRolls = new ArrayList<>();
        for (int[] pair : defenderPairs) {
            defenderRawRolls.add(pair[0]);
            defenderRolls.add(pair[1]);
        }

        attackerRolls.sort(Collections.reverseOrder());

        int pairs = Math.min(attackerRolls.size(), defenderRolls.size());
        int defenderHits = 0, attackerHits = 0;

        for (int i = 0; i < pairs; i++) {
            if (attackerRolls.get(i) > defenderRolls.get(i)) {
                defenderHits++;
            } else {
                attackerHits++;
            }
        }

        applyHits(defenders, defenderHits);
        applyHits(attackers, attackerHits);

        EventBus.publish(new CombatResultEvent(attackerRolls, defenderRolls, defenderRawRolls,
                attackerHits, defenderHits, defenderHasWall));
    }

    public int calculateStructureDamage(List<Unit> attackers) {
        int totalDamage = 0;
        for (Unit u : attackers) {
            totalDamage += u.getType().getAttackPower();
        }
        return totalDamage;
    }

    public void attackStructure(List<Unit> attackers, Building building) {
        building.takeDamage(calculateStructureDamage(attackers));
    }
}
