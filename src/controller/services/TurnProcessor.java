package controller.services;

import controller.GameController;
import controller.events.EventBus;
import controller.events.StarvationEvent;
import model.*;

import java.util.List;

public class TurnProcessor {
    private static final int FOOD_REQUIREMENT = 1;
    private static final int BASE_PRODUCTION_RATE = 2;

    private final GameController gc;

    public TurnProcessor(GameController gc) {
        this.gc = gc;
    }

    public void processTurnProduction(Tile tile, GlobalResourceManager economy) {
        processTurnProduction(tile, economy, true);
    }

    /**
     * Runs one building's per-turn production step. When {@code commit} is true this is the real
     * turn-end effect (spends upkeep, mutates the tile/economy, tracks failed upkeep strikes).
     * When false, nothing real is touched - only {@link GlobalResourceManager}'s net-change
     * tracker is updated, which is exactly what the HUD's "next turn" preview reads from. Using
     * the same method for both means the preview can't drift from what actually happens.
     */
    private void processTurnProduction(Tile tile, GlobalResourceManager economy, boolean commit) {
        Building building = tile.getBuilding();
        BuildingType type = building.getType();

        // The real path runs after gc.incrementTurn(), so "current turn" already means the turn
        // being processed; the preview hasn't incremented yet, so it must look one turn ahead.
        int effectiveTurn = commit ? gc.getCurrentTurn() : gc.getCurrentTurn() + 1;
        if (effectiveTurn <= building.getDisabledUntilTurn()) return;

        if (commit) {
            boolean spended = true;
            if (!economy.spendResource(ResourceType.WOOD, type.getWoodCost() / 10)) spended = false;
            if (!economy.spendResource(ResourceType.STONE, type.getStoneCost() / 10)) spended = false;
            if (!economy.spendResource(ResourceType.IRON, type.getIronCost() / 10)) spended = false;

            if (!spended) building.upkeepFailed();
            if (building.getFailedCount() == 3) {
                gc.getBuildings().remove(building);
                return;
            }
        } else {
            economy.addNetChanges(ResourceType.WOOD, -(type.getWoodCost() / 10));
            economy.addNetChanges(ResourceType.STONE, -(type.getStoneCost() / 10));
            economy.addNetChanges(ResourceType.IRON, -(type.getIronCost() / 10));
        }

        int ratePerWorker = calculateRatePerWorker(type);
        Season season = commit ? gc.getCurrentSeason() : Season.fromTurn(gc.getCurrentTurn() + 1);

        type.produceResources(building, tile, economy, ratePerWorker, gc.getTiles(), commit);
        applyAdjacencyBonus(building, tile, economy, gc.getTiles(), commit);
        applySeasonalBonus(type, economy, season, commit);
    }

    /** Same worker-output rate the real production uses: base rate, Pro Tools/Golden Age bonuses,
     *  the Dissatisfaction/Riot penalty, and Steel Tools' extra mine bonus. Shared so the preview
     *  can't fall out of sync with a rule change here. */
    private int calculateRatePerWorker(BuildingType type) {
        GlobalHappinessManager happiness = gc.getHappinessManager();

        double multiplier = gc.hasProToolsTech() ? 1.5 : 1.0;
        if (happiness.isGoldenAge()) multiplier *= 1.10;

        int ratePerWorker = (int) (multiplier * BASE_PRODUCTION_RATE);
        if (happiness.isDissatisfied() || happiness.isRiot()) {
            ratePerWorker = Math.max(0, ratePerWorker - 1);
        }

        if ((type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) && gc.hasTech(TechType.STEEL_TOOLS)) {
            ratePerWorker = (int) (ratePerWorker * 1.5);
        }
        return ratePerWorker;
    }

    private void applySeasonalBonus(BuildingType type, GlobalResourceManager economy, Season season, boolean commit) {
        if (type == BuildingType.FARM) {
            applyDelta(economy, ResourceType.WHEAT, season.getFarmFoodBonus(), commit);
        } else if (type == BuildingType.STABLE && season.getFarmFoodBonus() > 0) {
            applyDelta(economy, ResourceType.CATTLE, season.getFarmFoodBonus(), commit);
        }
    }

    private void applyAdjacencyBonus(Building building, Tile tile, GlobalResourceManager economy, List<Tile> allTiles, boolean commit) {
        BuildingType type = building.getType();

        if (type == BuildingType.FARM) {
            for (Tile other : allTiles) {
                if (other.getBuilding() != null && other.getBuilding().getType() == BuildingType.FARM &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    boolean thisIsSmaller = tile.getCol() < other.getCol() ||
                            (tile.getCol() == other.getCol() && tile.getRow() < other.getRow());
                    if (thisIsSmaller) applyDelta(economy, ResourceType.WHEAT, 1, commit);
                }
            }
        } else if (type == BuildingType.LUMBER_MILL) {
            for (Tile other : allTiles) {
                if (other.getTerrain() == TerrainType.SEA &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    applyDelta(economy, ResourceType.WOOD, 2, commit);
                    break;
                }
            }
        } else if (type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) {
            int mountainCount = 0;
            for (Tile other : allTiles) {
                if (other.getTerrain() == TerrainType.MOUNTAIN &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    mountainCount++;
                }
            }
            if (mountainCount >= 2) {
                applyDelta(economy, type.getOutputResource(), 1, commit);
            }
        }
    }

    /** Routes a signed delta to the real ledger (commit) or just the preview tracker (not commit). */
    private void applyDelta(GlobalResourceManager economy, ResourceType type, int amount, boolean commit) {
        if (amount == 0) return;
        if (commit) {
            if (amount > 0) economy.addResource(type, amount);
            else economy.spendResource(type, -amount);
        } else {
            economy.addNetChanges(type, amount);
        }
    }

    /**
     * Recomputes the HUD's "next turn" resource preview using the exact same rules as
     * {@link #advanceTurn()}'s real production step, without mutating any real game state.
     * Replaces GameController's old hand-duplicated estimate, which had drifted out of sync
     * with seasons, adjacency bonuses, happiness penalties, and tile resource depletion.
     */
    public void previewNextTurnChanges() {
        GlobalResourceManager economy = gc.getEconomy();
        economy.resetNetChanges();

        for (Tile tile : gc.getTiles()) {
            if (tile.getBuilding() != null) {
                processTurnProduction(tile, economy, false);
            }
        }

        long wallCount = gc.getEdgeFeatures().values().stream().filter(f -> f == EdgeFeature.WALL).count();
        economy.addNetChanges(ResourceType.STONE, -(int) wallCount);

        // Mirrors GlobalResourceManager.spendFood(): wheat is drawn down first, cattle covers the rest.
        int foodNeeded = FOOD_REQUIREMENT * gc.getUnits().size();
        int projectedWheat = Math.max(0, economy.getResourceAmount(ResourceType.WHEAT) + economy.getNetChanges(ResourceType.WHEAT));
        int fromWheat = Math.min(foodNeeded, projectedWheat);
        economy.addNetChanges(ResourceType.WHEAT, -fromWheat);
        if (foodNeeded - fromWheat > 0) {
            economy.addNetChanges(ResourceType.CATTLE, -(foodNeeded - fromWheat));
        }
    }

    public void advanceTurn(){
        gc.incrementTurn();
        gc.resetTradeTurn();

        gc.rollForDisaster();

        for(Tile tile: gc.getTiles()){
            Building building = tile.getBuilding();
            if(building != null){
                processTurnProduction(tile, gc.getEconomy());
            }
        }
        gc.processWallUpkeep();

        GlobalHappinessManager happinessForAmenities = gc.getHappinessManager();
        int steadyBonus = 0;
        for (Building building : gc.getBuildings()) {
            if (building.getType() == BuildingType.MONUMENT && !building.isDestroyed()) {
                steadyBonus += 2;
            }
        }
        if (gc.hasMilitaryUnitInTownHall()) {
            steadyBonus += 1;
        }
        happinessForAmenities.setSteadyBonus(steadyBonus);
        boolean isStarvation = false;
        boolean isRiot = gc.getHappinessManager().isRiot();
        for(Unit unit: gc.getUnits()){
            boolean hasFed = gc.getEconomy().spendFood(FOOD_REQUIREMENT);
            if(!hasFed){
                isStarvation = true;
            };
            unit.resetActionPoints(!hasFed || unit.isAssigned());
            if (isRiot) {
                unit.setCurrentAP(Math.max(0, unit.getCurrentAP() - 1));
            }
        }
        if(isStarvation) EventBus.publish(new StarvationEvent());

        Tile townhall = gc.getTownhall();
        Building townHallBuilding = townhall.getBuilding();
        if(townHallBuilding.isProducing()){
            townHallBuilding.decrementProductionTurns();
            if (townHallBuilding.getProductionTurnsLeft() <= 0) {
                if (townHallBuilding.getProducingUnit() != null) {
                    Unit newUnit = new Unit(townHallBuilding.getProducingUnit(), townhall.getCol(), townhall.getRow());
                    gc.addUnit(newUnit);
                } else if (townHallBuilding.getUpgradingToLevel() != null) {
                    townHallBuilding.applyLevelUpgrade();
                    gc.applyTownHallStorage(townHallBuilding.getTownHallLevel());
                } else if (townHallBuilding.getResearchingTech() != null) {
                    gc.completeTechResearch(townHallBuilding.getResearchingTech());
                }

                townHallBuilding.clearProduction();
            }
        }

        gc.checkTribeQuests();
        processTribeZoneTrespass();
        processTribeGuardProduction();
        processProactiveTribeQuestOffers();
        applyAlliedTribeBonuses();
        if (!gc.autosave()) {
            EventBus.publish(new controller.events.AutosaveFailedEvent());
        }
    }

    private void applyAlliedTribeBonuses() {
        GlobalResourceManager economy = gc.getEconomy();
        for (Tribe tribe : gc.getTribes()) {
            if (!tribe.isAllianceActive()) continue;

            switch (tribe.getType()) {
                case FARMER -> economy.addResource(ResourceType.WHEAT, 2);
                case COASTAL -> economy.addResource(ResourceType.WHEAT, 2);
                case MOUNTAIN -> economy.addResource(ResourceType.STONE, 2);
                case TRADER -> {
                    economy.addResource(ResourceType.WOOD, 1);
                    economy.addResource(ResourceType.STONE, 1);
                    economy.addResource(ResourceType.IRON, 1);
                    economy.addResource(ResourceType.WHEAT, 1);
                }
                case WARRIOR -> { /* handled as a military unit cap bonus, not a resource trickle */ }
            }
        }
    }

    private void processProactiveTribeQuestOffers() {
        if (gc.getCurrentTurn() % 5 != 0) return;

        for (Tribe tribe : gc.getTribes()) {
            TribeRelationship relationship = tribe.getRelationship();
            if (relationship != TribeRelationship.FRIENDLY && relationship != TribeRelationship.ALLIED) continue;
            if (!gc.canOfferQuestToTribe(tribe)) continue;

            gc.issueQuestToTribe(tribe);
        }
    }

    /** Neutral tribes warn once, then dock relationship if the player's military lingers in their
     *  zone; Displeased tribes skip the warning and dock relationship immediately, faster. */
    private void processTribeZoneTrespass() {
        for (Tribe tribe : gc.getTribes()) {
            TribeRelationship relationship = tribe.getRelationship();
            if (relationship != TribeRelationship.NEUTRAL && relationship != TribeRelationship.DISPLEASED) {
                tribe.setTrespassWarned(false);
                continue;
            }
            if (!gc.hasPlayerMilitaryInTribeZone(tribe)) {
                tribe.setTrespassWarned(false);
                continue;
            }

            if (relationship == TribeRelationship.DISPLEASED) {
                tribe.changeRelationship(-4);
                gc.notifyTribeEvent(tribe, "This tribe is displeased by your military presence near their camp.");
            } else if (tribe.isTrespassWarned()) {
                tribe.changeRelationship(-2);
                gc.notifyTribeEvent(tribe, "Relationship worsened - your military unit is still near their camp.");
            } else {
                tribe.setTrespassWarned(true);
                gc.notifyTribeEvent(tribe, "Warning: this tribe is wary of your military unit near their camp.");
            }
        }
    }

    private void processTribeGuardProduction() {
        if (gc.getCurrentTurn() % 3 != 0) return;

        for (Tribe tribe : gc.getTribes()) {
            if (tribe.getRelationship() != TribeRelationship.ENEMY) continue;
            if (gc.countGuardUnits(tribe) >= tribe.getGuardUnitCap()) continue;

            UnitType nextType = nextGuardType(tribe);
            if (nextType != null) {
                gc.spawnTribeGuard(tribe, nextType);
                gc.notifyTribeEvent(tribe, "Trained a new " + nextType.getDisplayName() + " to defend its camp.");
            }
        }
    }

    private UnitType nextGuardType(Tribe tribe) {
        if (gc.countGuardUnitsOfType(tribe, UnitType.SWORDSMAN) < 2) return UnitType.SWORDSMAN;
        if (gc.countGuardUnitsOfType(tribe, UnitType.ARCHER) < 2) return UnitType.ARCHER;
        if (gc.countGuardUnitsOfType(tribe, UnitType.CAVALRY) < 1) return UnitType.CAVALRY;
        return null;
    }
}
