package controller.services;

import model.Building;
import model.BuildingType;
import model.EdgeFeature;
import model.GlobalHappinessManager;
import model.GlobalResourceManager;
import model.HexEdge;
import model.HexUtils;
import model.Quest;
import model.QuestType;
import model.ResourceType;
import model.Tile;
import model.Tribe;
import model.TribeRelationship;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TribeService implements java.io.Serializable {
    private final Set<Tribe> tribesTradedThisTurn = new HashSet<>();

    public boolean canTradeWith(Tribe tribe) {
        if (!tribe.getType().canTrade()) return false;
        if (tribesTradedThisTurn.contains(tribe)) return false;
        TribeRelationship relationship = tribe.getRelationship();
        return relationship == TribeRelationship.FRIENDLY || relationship == TribeRelationship.ALLIED;
    }

    public boolean tradeWithTribe(Tribe tribe, GlobalResourceManager economy, ResourceType sellResource,
                                   int sellAmount, ResourceType rewardResource) {
        if (!canTradeWith(tribe)) return false;
        if (!tribe.getType().getTradeRewardOptions().contains(rewardResource)) return false;
        if (!economy.hasEnough(sellResource, sellAmount)) return false;

        int rewardAmount = (int) Math.floor(sellAmount * tribe.getType().getTradeRate());
        if (rewardAmount <= 0) return false;
        if (!economy.hasCapacityFor(rewardResource, rewardAmount)) return false;

        economy.spendResource(sellResource, sellAmount);
        economy.addResource(rewardResource, rewardAmount);
        tribesTradedThisTurn.add(tribe);
        return true;
    }

    public void resetTradeTurn() {
        tribesTradedThisTurn.clear();
    }

    public boolean canSendGift(Tribe tribe) {
        return tribe.getRelationship() != TribeRelationship.ENEMY;
    }

    public boolean sendGift(Tribe tribe, GlobalResourceManager economy, ResourceType resource, int resourceAmount) {
        if (!canSendGift(tribe)) return false;
        if (!economy.hasEnough(resource, resourceAmount)) return false;

        economy.spendResource(resource, resourceAmount);

        int relationshipBoost;
        if (resource == ResourceType.IRON) {
            relationshipBoost = (resourceAmount / 5) * 3;
        } else if (resource == ResourceType.STONE) {
            relationshipBoost = (resourceAmount / 10) * 3;
        } else {
            relationshipBoost = (resourceAmount / 10) * 2;
        }
        tribe.changeRelationship(relationshipBoost);
        return true;
    }

    public void recordWar(Tribe tribe) {
        tribe.setRelationshipValue(-100);
    }

    /** Any attack (guard combat or structure damage) immediately sets a tribe to Enemy. */
    public void recordAttack(Tribe tribe, GlobalHappinessManager happiness) {
        TribeRelationship priorRelationship = tribe.getRelationship();
        if (priorRelationship == TribeRelationship.ALLIED) {
            happiness.addHappiness(-15);
        } else if (priorRelationship == TribeRelationship.FRIENDLY) {
            happiness.addHappiness(-5);
        }

        tribe.setRelationshipValue(-100);
        tribe.setActiveQuest(null);
        tribesTradedThisTurn.remove(tribe);
    }

    public boolean canRequestPeace(Tribe tribe) {
        return tribe.getRelationship() == TribeRelationship.ENEMY;
    }

    public boolean requestPeace(Tribe tribe, GlobalResourceManager economy) {
        if (!canRequestPeace(tribe)) return false;
        if (!economy.hasEnough(ResourceType.WHEAT, 30) || !economy.hasEnough(ResourceType.WOOD, 30) ||
                !economy.hasEnough(ResourceType.IRON, 30)) {
            return false;
        }

        economy.spendResource(ResourceType.WHEAT, 30);
        economy.spendResource(ResourceType.WOOD, 30);
        economy.spendResource(ResourceType.IRON, 30);
        tribe.setRelationshipValue(-10);
        return true;
    }

    public boolean canRequestAlliance(Tribe tribe, List<Tribe> allTribes, int currentTurn) {
        if (tribe.getRelationshipValue() < 70) return false;
        if (tribe.getRelationship() == TribeRelationship.ENEMY) return false;
        if (currentTurn - tribe.getLastQuestFailedTurn() < 5) return false;
        return allianceExclusivityReason(tribe, allTribes) == null;
    }

    public String allianceExclusivityReason(Tribe tribe, List<Tribe> allTribes) {
        boolean anyOtherAllied = allTribes.stream().anyMatch(t -> t != tribe && t.isAllianceActive());

        if (tribe.getType() == model.TribeType.WARRIOR && anyOtherAllied) {
            return "Alliance with the Warrior tribe requires no other active alliances.";
        }
        boolean anyWarriorAllied = allTribes.stream()
                .anyMatch(t -> t != tribe && t.getType() == model.TribeType.WARRIOR && t.isAllianceActive());
        if (anyWarriorAllied) {
            return "Already allied with the Warrior tribe, which requires exclusivity.";
        }

        if (tribe.getType() == model.TribeType.FARMER || tribe.getType() == model.TribeType.MOUNTAIN) {
            model.TribeType opposite = tribe.getType() == model.TribeType.FARMER ? model.TribeType.MOUNTAIN : model.TribeType.FARMER;
            boolean oppositeAllied = allTribes.stream()
                    .anyMatch(t -> t.getType() == opposite && t.isAllianceActive());
            if (oppositeAllied) {
                return "Cannot be allied with both the Farmer and Mountain tribes at once.";
            }
        }
        return null;
    }

    public boolean requestAlliance(Tribe tribe, List<Tribe> allTribes, int currentTurn) {
        if (!canRequestAlliance(tribe, allTribes, currentTurn)) return false;
        tribe.changeRelationship(5);
        tribe.setAllianceActive(true);
        return true;
    }

    public void awardCaptureLoot(Tribe tribe, GlobalResourceManager economy) {
        switch (tribe.getType()) {
            case FARMER -> economy.addResource(ResourceType.WHEAT, 40);
            case MOUNTAIN -> economy.addResource(ResourceType.STONE, 40);
            case COASTAL -> {
                economy.addResource(ResourceType.WHEAT, 30);
                economy.addResource(ResourceType.WOOD, 20);
            }
            case WARRIOR -> {
                economy.addResource(ResourceType.WOOD, 20);
                economy.addResource(ResourceType.STONE, 20);
                economy.addResource(ResourceType.IRON, 20);
            }
            case TRADER -> {
                economy.addResource(ResourceType.WOOD, 20);
                economy.addResource(ResourceType.STONE, 20);
            }
        }
    }

    public boolean canOfferQuest(Tribe tribe, int currentTurn) {
        if (tribe.getRelationshipValue() < 20) return false;
        if (tribe.getRelationship() == TribeRelationship.ENEMY) return false;
        return tribe.getActiveQuest() == null && currentTurn >= tribe.getQuestCooldownUntilTurn();
    }

    public void issueQuest(Tribe tribe, int currentTurn) {
        QuestType type = QuestType.forTribeType(tribe.getType());
        tribe.setActiveQuest(new Quest(type, currentTurn));
    }

    private boolean isRoadQuestSatisfied(Tribe tribe, Map<HexEdge, EdgeFeature> edgeFeatures) {
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.ROAD) continue;

            HexEdge edge = entry.getKey();
            boolean touchesCamp = (edge.getCol1() == tribe.getCol() && edge.getRow1() == tribe.getRow()) ||
                    (edge.getCol2() == tribe.getCol() && edge.getRow2() == tribe.getRow());
            if (touchesCamp) return true;
        }
        return false;
    }

    private boolean hasDockWithinDistance(Tribe tribe, List<Building> buildings, List<Tile> allTiles, int maxDist) {
        for (Building b : buildings) {
            if (b.getType() != BuildingType.DOCK) continue;
            if (hexDistanceWithin(tribe.getCol(), tribe.getRow(), b.getCol(), b.getRow(), allTiles, maxDist)) return true;
        }
        return false;
    }

    public boolean hexDistanceWithin(int fromCol, int fromRow, int toCol, int toRow, List<Tile> allTiles, int maxDist) {
        if (fromCol == toCol && fromRow == toRow) return true;

        Set<Long> visited = new HashSet<>();
        Queue<int[]> frontier = new ArrayDeque<>();
        frontier.add(new int[]{fromCol, fromRow, 0});
        visited.add(key(fromCol, fromRow));

        while (!frontier.isEmpty()) {
            int[] cur = frontier.poll();
            if (cur[2] >= maxDist) continue;

            for (Tile t : allTiles) {
                long k = key(t.getCol(), t.getRow());
                if (visited.contains(k)) continue;
                if (!HexUtils.isNeighbor(cur[0], cur[1], t.getCol(), t.getRow())) continue;

                if (t.getCol() == toCol && t.getRow() == toRow) return true;
                visited.add(k);
                frontier.add(new int[]{t.getCol(), t.getRow(), cur[2] + 1});
            }
        }
        return false;
    }

    private long key(int col, int row) {
        return ((long) col << 32) | (row & 0xffffffffL);
    }

    public boolean isQuestSatisfied(Tribe tribe, Quest quest, GlobalResourceManager economy,
                                     Map<HexEdge, EdgeFeature> edgeFeatures, List<Building> buildings,
                                     List<Tile> allTiles) {
        return switch (quest.getType()) {
            case FARMER_SUPPLIES, MOUNTAIN_TOOLS -> economy.hasEnough(quest.getType().getCostResource1(), quest.getType().getCostAmount1()) &&
                    economy.hasEnough(quest.getType().getCostResource2(), quest.getType().getCostAmount2());
            case TRADER_ROUTE -> isRoadQuestSatisfied(tribe, edgeFeatures);
            case WARRIOR_DEFEAT -> quest.getDefeatsRecorded() >= 2;
            case COASTAL_DOCK -> hasDockWithinDistance(tribe, buildings, allTiles, 4);
        };
    }

    public boolean canDeliverQuest(Quest quest, GlobalResourceManager economy) {
        if (quest == null || quest.isCompleted() || !quest.isReadyToDeliver()) return false;
        QuestType type = quest.getType();
        if (type.getRewardResource() == null) return true;
        return economy.hasCapacityFor(type.getRewardResource(), type.getRewardAmount());
    }

    public boolean completeQuest(Tribe tribe, Quest quest, GlobalResourceManager economy) {
        if (!canDeliverQuest(quest, economy)) return false;

        QuestType type = quest.getType();
        if (type.getCostResource1() != null) economy.spendResource(type.getCostResource1(), type.getCostAmount1());
        if (type.getCostResource2() != null) economy.spendResource(type.getCostResource2(), type.getCostAmount2());
        if (type.getRewardResource() != null) economy.addResource(type.getRewardResource(), type.getRewardAmount());

        quest.markCompleted();
        tribe.changeRelationship(type.getRewardRelationship());
        return true;
    }

    public void expireQuest(Tribe tribe, int currentTurn) {
        tribe.setActiveQuest(null);
        tribe.changeRelationship(-10);
        tribe.setQuestCooldownUntilTurn(currentTurn + 5);
        tribe.setLastQuestFailedTurn(currentTurn);
    }

    public boolean cancelQuest(Tribe tribe) {
        if (tribe.getActiveQuest() == null) return false;
        tribe.setActiveQuest(null);
        tribe.changeRelationship(-5);
        return true;
    }
}
