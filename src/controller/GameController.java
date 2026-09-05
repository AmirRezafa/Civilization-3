package controller;

import controller.events.EventBus;
import controller.events.HUDChangedEvent;
import controller.events.StarvationEvent;
import controller.events.UnitActionsChangedEvent;
import controller.services.CombatService;
import controller.services.DisasterService;
import controller.services.FogOfWarService;
import controller.services.PersistedServices;
import controller.services.SaveLoadService;
import controller.services.TradeService;
import controller.services.TribeService;
import controller.services.TurnProcessor;
import controller.services.WorldGenerator;
import model.*;
import view.Ground;

import javax.swing.*;
import javax.swing.Timer;
import java.util.*;

public class GameController {
    private final static int BUILD_COST = 2;

    private AnimationController animationController;
    private Ground ground;
    private Camera camera;
    private FogOfWarService fogOfWarService;
    private TurnProcessor turnProcessor;
    private TradeService tradeService = new TradeService();
    private GlobalHappinessManager happinessManager = new GlobalHappinessManager();
    private TribeService tribeService = new TribeService();
    private List<Tribe> tribes;
    private DisasterService disasterService = new DisasterService();
    private final SaveLoadService saveLoadService = new SaveLoadService();

    final static int ROWS = 40, COLS = 40;

    private ArrayList<Tile> Tiles;
    private Tile[][] tileGrid;
    private ArrayList<Unit> units = new ArrayList<>();
    private ArrayList<Building> buildings = new ArrayList<>();
    private Map<HexEdge, EdgeFeature> edgeFeatures;
    private Set<HexEdge> riverEdges;
    private Map<HexEdge, Integer> wallHP = new HashMap<>();
    private Map<HexEdge, Integer> wallFailedUpkeep = new HashMap<>();
    private Map<HexEdge, Tribe> edgeOwners = new HashMap<>();
    private static final int WALL_MAX_HP = 100;

    private GlobalResourceManager economy;

    private Unit selectedUnit = null;
    private Tile tileUnderUnit = null;
    private EdgeFeature pendingEdgeBuild = null;
    private boolean pendingEdgeDeconstruct = false;
    private boolean pendingAttack = false;
    private CombatService combatService = new CombatService();

    private int currentTurn = 1;

    private Tile Townhall;

    private int TownhallX = 20, TownhallY = 20;

    private int unitCapacity = 9;
    private Map<UnitType, Integer> unitCount = new HashMap<>();

    private Map<TechType, Boolean> researchedTechs = new HashMap<>();

    public GameController(Ground ground) {
        this.ground = ground;
        camera = new Camera(ground);
        ground.addMouseMotionListener(camera);
        ground.addMouseListener(camera);
        ground.addMouseWheelListener(camera);

        ground.addMouseListener(new InputHandler(this));

        WorldGenerator.WorldData worldData = new WorldGenerator().generate(ROWS, COLS, TownhallX, TownhallY);
        this.Tiles = worldData.tiles;
        this.tileGrid = worldData.tileGrid;
        this.Townhall = worldData.townhall;
        this.edgeFeatures = worldData.edgeFeatures;
        this.riverEdges = worldData.riverEdges;
        this.buildings.add(worldData.townhallBuilding);
        this.buildings.addAll(worldData.neutralBuildings);
        this.tribes = worldData.tribes;
        for (Unit unit : worldData.initialUnits) {
            addUnit(unit);
        }
        markTilesOwned(Townhall);

        fogOfWarService = new FogOfWarService(ROWS, COLS, tileGrid, Tiles, units, buildings);
        updateFog();

        animationController = new AnimationController(this);
        economy = new GlobalResourceManager();
        applyTownHallStorage(Townhall.getBuilding().getTownHallLevel());
        turnProcessor = new TurnProcessor(this);

        Timer timer = new Timer(
                8,
                e -> frameGenerator()
        );
        timer.start();
    }

    public void advanceTurn(){
        turnProcessor.advanceTurn();
    }

    public int getA() {
        return camera.getA();
    }

    public int getB() {
        return camera.getB();
    }

    public void updateFog() {
        fogOfWarService.updateFog();
    }

    private void frameGenerator() {
        animationController.run();
        camera.run();
//        updateFog();
        ground.repaint();
    }

    public int getXOffset(){
        return camera.getXOffset();
    }

    public int getYOffset(){
        return camera.getYOffset();
    }

    public ArrayList<Tile> getTiles() {
        return Tiles;
    }

    public ArrayList<Unit> getUnits() {
        return units;
    }

    /** Every non-garrisoned unit standing on this hex - used to show what's actually in a stack,
     *  since clicking a hex only ever selects the first one found. */
    public List<Unit> getUnitsAt(int col, int row) {
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (u.isAssigned()) continue;
            if (u.getCol() == col && u.getRow() == row) result.add(u);
        }
        return result;
    }

    public ArrayList<Building> getBuildings() {
        return buildings;
    }

    public EdgeFeature getEdgeFeature(int col1, int row1, int col2, int row2) {
        return edgeFeatures.getOrDefault(new HexEdge(col1, row1, col2, row2), EdgeFeature.NONE);
    }

    public Map<HexEdge, EdgeFeature> getEdgeFeatures() {
        return Collections.unmodifiableMap(edgeFeatures);
    }

    public Tile getTileAt(int col, int row) {
        return tileGrid[col][row];
    }

    public boolean hasAdjacencyBonus(Tile tile) {
        Building building = tile.getBuilding();
        if (building == null) return false;
        BuildingType type = building.getType();

        if (type == BuildingType.FARM) {
            for (Tile other : Tiles) {
                if (other.getBuilding() != null && other.getBuilding().getType() == BuildingType.FARM &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    return true;
                }
            }
            return false;
        }

        if (type == BuildingType.LUMBER_MILL) {
            for (Tile other : Tiles) {
                if (other.getTerrain() == TerrainType.SEA &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    return true;
                }
            }
            return false;
        }

        if (type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) {
            int mountainCount = 0;
            for (Tile other : Tiles) {
                if (other.getTerrain() == TerrainType.MOUNTAIN &&
                        HexUtils.isNeighbor(tile.getCol(), tile.getRow(), other.getCol(), other.getRow())) {
                    mountainCount++;
                }
            }
            return mountainCount >= 2;
        }

        return false;
    }

    public void startBuildingEdge(EdgeFeature feature) {
        pendingEdgeBuild = feature;
    }

    public EdgeFeature getPendingEdgeBuild() {
        return pendingEdgeBuild;
    }

    public boolean buildEdgeFeature(int col1, int row1, int col2, int row2, EdgeFeature feature) {
        int woodCost = feature == EdgeFeature.ROAD ? 10 : 0;
        int stoneCost = feature == EdgeFeature.WALL ? 30 : 0;
        int apCost = feature == EdgeFeature.ROAD ? 1 : 2;

        if (selectedUnit == null || selectedUnit.getCurrentAP() < apCost) return false;
        if (!(economy.hasEnough(ResourceType.WOOD, woodCost) && economy.hasEnough(ResourceType.STONE, stoneCost)))
            return false;

        Tile tileA = tileGrid[col1][row1];
        Tile tileB = tileGrid[col2][row2];
        if (!tileA.isExplored() || !tileB.isExplored()) return false;
        if (tileA.getTerrain() == TerrainType.SEA || tileA.getTerrain() == TerrainType.MOUNTAIN_RANGE) return false;
        if (tileB.getTerrain() == TerrainType.SEA || tileB.getTerrain() == TerrainType.MOUNTAIN_RANGE) return false;
        if (feature == EdgeFeature.WALL && !tileA.isOwned() && !tileB.isOwned()) return false;

        economy.spendResource(ResourceType.WOOD, woodCost);
        economy.spendResource(ResourceType.STONE, stoneCost);
        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - apCost);

        HexEdge edge = new HexEdge(col1, row1, col2, row2);
        edgeFeatures.put(edge, feature);
        if (feature == EdgeFeature.WALL) wallHP.put(edge, WALL_MAX_HP);
        pendingEdgeBuild = null;
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public int getWallHP(int col1, int row1, int col2, int row2) {
        return wallHP.getOrDefault(new HexEdge(col1, row1, col2, row2), 0);
    }

    /** null means player-owned, matching Unit.owner/Building.owner - no tribe currently builds edges. */
    public Tribe getEdgeOwner(int col1, int row1, int col2, int row2) {
        return edgeOwners.get(new HexEdge(col1, row1, col2, row2));
    }

    public void destroyRoadsTouching(int col, int row) {
        List<HexEdge> toRemove = new ArrayList<>();
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.ROAD) continue;
            HexEdge edge = entry.getKey();
            if (touchesHex(edge, col, row)) toRemove.add(edge);
        }
        for (HexEdge edge : toRemove) edgeFeatures.remove(edge);
    }

    public boolean hasRiverEdgeTouching(int col, int row) {
        for (HexEdge edge : riverEdges) {
            if (touchesHex(edge, col, row)) return true;
        }
        return false;
    }

    public boolean hasRiverEdge(int col1, int row1, int col2, int row2) {
        return riverEdges.contains(new HexEdge(col1, row1, col2, row2));
    }

    public Set<HexEdge> getRiverEdges() {
        return Collections.unmodifiableSet(riverEdges);
    }

    private boolean touchesHex(HexEdge edge, int col, int row) {
        return (edge.getCol1() == col && edge.getRow1() == row) ||
                (edge.getCol2() == col && edge.getRow2() == row);
    }

    public void startDeconstructingEdge() {
        pendingEdgeDeconstruct = true;
    }

    public boolean isPendingEdgeDeconstruct() {
        return pendingEdgeDeconstruct;
    }

    public boolean deconstructEdge(int col1, int row1, int col2, int row2) {
        EdgeFeature feature = getEdgeFeature(col1, row1, col2, row2);
        if (feature != EdgeFeature.ROAD && feature != EdgeFeature.WALL) return false;
        if (selectedUnit == null || selectedUnit.getCurrentAP() < 1) return false;

        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);
        HexEdge edge = new HexEdge(col1, row1, col2, row2);
        edgeFeatures.remove(edge);
        wallHP.remove(edge);
        wallFailedUpkeep.remove(edge);
        edgeOwners.remove(edge);
        pendingEdgeDeconstruct = false;
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    private static final Set<BuildingType> NON_DEMOLISHABLE = Set.of(
            BuildingType.TOWN_HALL, BuildingType.TRIBE_CAMP, BuildingType.TRADING_POST);

    private boolean pendingBuildingDeconstruct = false;

    public void startDeconstructingBuilding() {
        pendingBuildingDeconstruct = true;
    }

    public boolean isPendingBuildingDeconstruct() {
        return pendingBuildingDeconstruct;
    }

    public void cancelPendingBuildingDeconstruct() {
        pendingBuildingDeconstruct = false;
    }

    public boolean canDeconstructBuilding() {
        if (selectedUnit == null || selectedUnit.getType() != UnitType.BUILDER) return false;
        if (tileUnderUnit == null || tileUnderUnit.getBuilding() == null) return false;
        Building building = tileUnderUnit.getBuilding();
        if (building.getOwner() != null) return false;
        if (NON_DEMOLISHABLE.contains(building.getType())) return false;
        return selectedUnit.getCurrentAP() >= 1;
    }

    public boolean deconstructBuilding() {
        if (!canDeconstructBuilding()) return false;
        return deconstructBuildingAt(tileUnderUnit.getCol(), tileUnderUnit.getRow());
    }

    public boolean canDeconstructBuildingAt(int col, int row) {
        if (selectedUnit == null || selectedUnit.getType() != UnitType.BUILDER) return false;
        if (selectedUnit.getCurrentAP() < 1) return false;

        boolean sameHex = selectedUnit.getCol() == col && selectedUnit.getRow() == row;
        boolean adjacent = HexUtils.isNeighbor(selectedUnit.getCol(), selectedUnit.getRow(), col, row);
        if (!sameHex && !adjacent) return false;

        Tile target = tileGrid[col][row];
        Building building = target.getBuilding();
        return building != null && building.getOwner() == null && !NON_DEMOLISHABLE.contains(building.getType());
    }

    public boolean deconstructBuildingAt(int col, int row) {
        if (!canDeconstructBuildingAt(col, row)) return false;

        Tile targetTile = tileGrid[col][row];
        Building building = targetTile.getBuilding();
        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - 1);

        Tile refuge = findNearestEmptyLandHex(targetTile);
        for (Unit worker : new ArrayList<>(building.getStationedWorkers())) {
            worker.setAssigned(false);
            if (refuge != null) worker.placeAt(refuge.getCol(), refuge.getRow());
        }
        building.getStationedWorkers().clear();

        buildings.remove(building);
        targetTile.setBuilding(null);
        pendingBuildingDeconstruct = false;
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    private Tile findNearestEmptyLandHex(Tile from) {
        if (from.getTerrain().isPassable() && from.getTerrain() != TerrainType.SEA) return from;

        for (int radius = 1; radius <= 6; radius++) {
            for (Tile t : HexUtils.hexesWithinRadius(from.getCol(), from.getRow(), radius, Tiles)) {
                if (t.getTerrain().isPassable() && t.getTerrain() != TerrainType.SEA && t.getBuilding() == null) {
                    return t;
                }
            }
        }
        return null;
    }

    public void startAttacking() {
        pendingAttack = true;
    }

    public boolean isPendingAttack() {
        return pendingAttack;
    }

    public boolean attackHex(int col, int row) {
        return attackHex(col, row, true);
    }

    /** attackWallFirst only matters when a wall separates the two hexes AND the target hex has
     *  defenders - the player then has the spec's two choices: knock the wall down first, or
     *  attack straight through and let the defender's dice get the wall's +2 bonus. */
    public boolean attackHex(int col, int row, boolean attackWallFirst) {
        if (selectedUnit == null) return false;

        int anchorCol = selectedUnit.getCol(), anchorRow = selectedUnit.getRow();
        boolean adjacent = HexUtils.isNeighbor(anchorCol, anchorRow, col, row);

        List<Unit> attackers = new ArrayList<>();
        for (Unit u : units) {
            if (u.getOwner() != null) continue;
            if (u.getCol() != anchorCol || u.getRow() != anchorRow) continue;
            if (!isMilitaryUnit(u.getType())) continue;
            if (u.getCurrentAP() < 1) continue;
            if (!adjacent && u.getType() != UnitType.ARCHER) continue;
            attackers.add(u);
        }
        if (attackers.isEmpty()) return false;

        Tribe defendingTribe = getTribeAt(col, row);
        List<Unit> defenders = new ArrayList<>();
        if (defendingTribe != null) {
            for (Unit u : units) {
                if (u.getOwner() == defendingTribe) defenders.add(u);
            }
        }

        boolean wallPresent = false;
        HexEdge edge = null;
        if (adjacent) {
            edge = new HexEdge(anchorCol, anchorRow, col, row);
            wallPresent = edgeFeatures.getOrDefault(edge, EdgeFeature.NONE) == EdgeFeature.WALL;
        }

        if (wallPresent && (defenders.isEmpty() || attackWallFirst)) {
            int damage = combatService.calculateStructureDamage(attackers);
            int remaining = wallHP.getOrDefault(edge, WALL_MAX_HP) - damage;
            if (remaining <= 0) {
                edgeFeatures.remove(edge);
                wallHP.remove(edge);
                wallFailedUpkeep.remove(edge);
                edgeOwners.remove(edge);
            } else {
                wallHP.put(edge, remaining);
            }
            for (Unit u : attackers) u.setCurrentAP(u.getCurrentAP() - 1);
            pendingAttack = false;
            EventBus.publish(new HUDChangedEvent());
            return true;
        }

        if (defendingTribe != null) {
            tribeService.recordAttack(defendingTribe, happinessManager);
            notifyTribeEvent(defendingTribe, "You attacked their camp. War has started.");
        }

        if (!defenders.isEmpty()) {
            combatService.resolveCombat(attackers, defenders, wallPresent, !adjacent);

            int defeatedCount = 0;
            for (Unit d : new ArrayList<>(defenders)) {
                if (d.isDead()) {
                    deleteUnit(d);
                    defeatedCount++;
                }
            }
            recordDefeatsForNearbyQuests(col, row, defeatedCount);

            for (Unit u : attackers) u.setCurrentAP(u.getCurrentAP() - 1);
            pendingAttack = false;

            for (Unit u : new ArrayList<>(attackers)) {
                if (u.isDead()) {
                    deleteUnit(u);
                    if (u == selectedUnit) selectedUnit = null;
                }
            }

            EventBus.publish(new HUDChangedEvent());
            return true;
        }

        if (!adjacent) return false;

        Tile targetTile = tileGrid[col][row];
        Building target = targetTile.getBuilding();
        if (target == null) {
            markTilesOwned(targetTile);
            for (Unit u : attackers) u.setCurrentAP(u.getCurrentAP() - 1);
            pendingAttack = false;
            EventBus.publish(new HUDChangedEvent());
            return true;
        }
        if (target.getType() == BuildingType.TOWN_HALL) return false;

        combatService.attackStructure(attackers, target);
        for (Unit u : attackers) u.setCurrentAP(u.getCurrentAP() - 1);
        pendingAttack = false;

        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public boolean hasWallToward(int col, int row) {
        if (selectedUnit == null) return false;
        return edgeFeatures.getOrDefault(new HexEdge(selectedUnit.getCol(), selectedUnit.getRow(), col, row),
                EdgeFeature.NONE) == EdgeFeature.WALL;
    }

    public boolean hasDefendersAt(int col, int row) {
        Tribe tribe = getTribeAt(col, row);
        if (tribe == null) return false;
        for (Unit u : units) {
            if (u.getOwner() == tribe) return true;
        }
        return false;
    }

    public boolean hasArcherAvailable() {
        if (selectedUnit == null) return false;
        for (Unit u : units) {
            if (u.getOwner() != null) continue;
            if (u.getCol() != selectedUnit.getCol() || u.getRow() != selectedUnit.getRow()) continue;
            if (u.getType() == UnitType.ARCHER && u.getCurrentAP() >= 1) return true;
        }
        return false;
    }

    private void recordDefeatsForNearbyQuests(int col, int row, int defeatedCount) {
        if (defeatedCount <= 0) return;

        for (Tribe tribe : tribes) {
            Quest quest = tribe.getActiveQuest();
            if (quest == null || quest.isCompleted() || quest.getType() != QuestType.WARRIOR_DEFEAT) continue;
            if (!tribeService.hexDistanceWithin(tribe.getCol(), tribe.getRow(), col, row, Tiles, 5)) continue;

            for (int i = 0; i < defeatedCount; i++) quest.recordDefeat();
        }
    }

    private Tribe getTribeAt(int col, int row) {
        for (Tribe t : tribes) {
            if (t.getCol() == col && t.getRow() == row) return t;
        }
        return null;
    }

    /** The camp hex plus its 6 neighbors - the tribe's off-limits territory per the spec. */
    public boolean hasPlayerMilitaryInTribeZone(Tribe tribe) {
        for (Tile t : HexUtils.hexesWithinRadius(tribe.getCol(), tribe.getRow(), 1, Tiles)) {
            for (Unit u : units) {
                if (u.getOwner() == null && isMilitaryUnit(u.getType()) &&
                        u.getCol() == t.getCol() && u.getRow() == t.getRow()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Blocks building inside an un-friendly, uncaptured tribe's zone (camp hex + 6 neighbors). */
    public boolean isInTribeForbiddenZone(int col, int row) {
        for (Tribe tribe : tribes) {
            Building camp = tileGrid[tribe.getCol()][tribe.getRow()].getBuilding();
            if (camp == null || camp.getType() != BuildingType.TRIBE_CAMP) continue;
            TribeRelationship relationship = tribe.getRelationship();
            if (relationship == TribeRelationship.FRIENDLY || relationship == TribeRelationship.ALLIED) continue;

            for (Tile t : HexUtils.hexesWithinRadius(tribe.getCol(), tribe.getRow(), 1, Tiles)) {
                if (t.getCol() == col && t.getRow() == row) return true;
            }
        }
        return false;
    }

    public Tile getTownhall() {
        return Townhall;
    }

    public void incrementTurn() {
        currentTurn++;
    }

    public void addUnit(Unit unit){
        units.add(unit);
        unitCount.put(unit.getType(), unitCount.getOrDefault(unit.getType(), 0) + 1);
        if (getUnitCounts() == unitCapacity) {
            happinessManager.addHappiness(-1);
        }
        if (isMilitaryUnit(unit.getType()) && getMilitaryUnitCount() == getEffectiveMilitaryUnitCap()) {
            happinessManager.addHappiness(-1);
        }
    }

    public void deleteUnit(Unit unit){
        units.remove(unit);
        if (unit.getOwner() == null) {
            unitCount.put(unit.getType(), unitCount.getOrDefault(unit.getType(), 0) - 1);
        }
    }

    public void spawnTribeGuard(Tribe tribe, UnitType type) {
        Unit guard = new Unit(type, tribe.getCol(), tribe.getRow());
        guard.setOwner(tribe);
        units.add(guard);
    }

    public int countGuardUnits(Tribe tribe) {
        int count = 0;
        for (Unit u : units) {
            if (u.getOwner() == tribe) count++;
        }
        return count;
    }

    public int countGuardUnitsOfType(Tribe tribe, UnitType type) {
        int count = 0;
        for (Unit u : units) {
            if (u.getOwner() == tribe && u.getType() == type) count++;
        }
        return count;
    }

    public Unit getSelectedUnit() {
        return selectedUnit;
    }

    public void setSelectedUnit(Unit unit) {
        selectedUnit = unit;
    }

    public Tile getTileUnderUnit() {
        return tileUnderUnit;
    }

    public void setTileUnderUnit(Tile tile) {
        tileUnderUnit = tile;
    }

    public GlobalResourceManager getEconomy() {
        return economy;
    }

    public GlobalHappinessManager getHappinessManager() {
        return happinessManager;
    }

    public List<Tribe> getTribes() {
        return tribes;
    }

    public void notifyTribeEvent(Tribe tribe, String message) {
        EventBus.publish(new controller.events.TribeEvent(tribe.getName(), message));
    }

    private void checkRelationshipTierChange(Tribe tribe, TribeRelationship before) {
        TribeRelationship after = tribe.getRelationship();
        if (after == before) return;

        String message = "Relationship is now " + after + " (" + tribe.getRelationshipValue() + ").";
        boolean nowTradeable = (after == TribeRelationship.FRIENDLY || after == TribeRelationship.ALLIED);
        boolean wasTradeable = (before == TribeRelationship.FRIENDLY || before == TribeRelationship.ALLIED);
        if (nowTradeable && !wasTradeable) message += " Trade is now available.";
        notifyTribeEvent(tribe, message);
    }

    public boolean canSendGiftToTribe(Tribe tribe) {
        return tribeService.canSendGift(tribe);
    }

    public boolean sendGiftToTribe(Tribe tribe, ResourceType resource, int amount) {
        TribeRelationship before = tribe.getRelationship();
        boolean success = tribeService.sendGift(tribe, economy, resource, amount);
        if (success) {
            EventBus.publish(new HUDChangedEvent());
            checkRelationshipTierChange(tribe, before);
        }
        return success;
    }

    public boolean captureTribeCamp(Tribe tribe) {
        Tile tile = tileGrid[tribe.getCol()][tribe.getRow()];
        Building camp = tile.getBuilding();
        if (camp == null || camp.getType() != BuildingType.TRIBE_CAMP) return false;
        if (!camp.isDestroyed()) return false;

        buildings.remove(camp);
        Building outpost = new Building(BuildingType.OUTPOST, tribe.getCol(), tribe.getRow());
        buildings.add(outpost);
        tile.setBuilding(outpost);

        tribeService.awardCaptureLoot(tribe, economy);
        tribeService.recordWar(tribe);

        notifyTribeEvent(tribe, "Camp captured and converted to an Outpost.");
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public boolean canDeclareWarOnTribe(Tribe tribe) {
        return tribe.getRelationship() != TribeRelationship.ENEMY;
    }

    public void declareWarOnTribe(Tribe tribe) {
        tribeService.recordAttack(tribe, happinessManager);
        notifyTribeEvent(tribe, "War declared. Relationship dropped to -100.");
        EventBus.publish(new HUDChangedEvent());
    }

    public boolean canRequestPeaceWithTribe(Tribe tribe) {
        return tribeService.canRequestPeace(tribe);
    }

    public boolean requestPeaceWithTribe(Tribe tribe) {
        boolean success = tribeService.requestPeace(tribe, economy);
        if (success) {
            notifyTribeEvent(tribe, "Peace agreed. Relationship is now Displeased.");
            EventBus.publish(new HUDChangedEvent());
        }
        return success;
    }

    public boolean canRequestAllianceWithTribe(Tribe tribe) {
        return tribeService.canRequestAlliance(tribe, tribes, currentTurn);
    }

    public String allianceExclusivityReason(Tribe tribe) {
        return tribeService.allianceExclusivityReason(tribe, tribes);
    }

    public boolean requestAllianceWithTribe(Tribe tribe) {
        boolean success = tribeService.requestAlliance(tribe, tribes, currentTurn);
        if (success) {
            notifyTribeEvent(tribe, "Alliance formed: " + describeAllianceBonus(tribe.getType()) + " is now active.");
            EventBus.publish(new HUDChangedEvent());
        }
        return success;
    }

    public String describeAllianceBonus(TribeType type) {
        return switch (type) {
            case FARMER -> "+2 food per turn";
            case MOUNTAIN -> "+2 stone per turn";
            case TRADER -> "+1 wood, +1 stone, +1 iron, +1 food per turn";
            case COASTAL -> "+2 food per turn";
            case WARRIOR -> "+1 military unit cap";
        };
    }

    public Season getCurrentSeason() {
        return Season.fromTurn(currentTurn);
    }

    public boolean canOfferQuestToTribe(Tribe tribe) {
        return tribeService.canOfferQuest(tribe, currentTurn);
    }

    public void issueQuestToTribe(Tribe tribe) {
        tribeService.issueQuest(tribe, currentTurn);
        EventBus.publish(new HUDChangedEvent());
    }

    public void checkTribeQuests() {
        for (Tribe tribe : tribes) {
            Quest quest = tribe.getActiveQuest();
            if (quest == null || quest.isCompleted() || quest.isReadyToDeliver()) continue;

            if (tribeService.isQuestSatisfied(tribe, quest, economy, edgeFeatures, buildings, Tiles)) {
                quest.markReadyToDeliver();
                notifyTribeEvent(tribe, "Quest ready to deliver: " + quest.getDescription());
                EventBus.publish(new HUDChangedEvent());
            } else if (quest.isExpired(currentTurn)) {
                tribeService.expireQuest(tribe, currentTurn);
                notifyTribeEvent(tribe, "Quest failed: " + quest.getDescription());
                EventBus.publish(new HUDChangedEvent());
            }
        }
    }

    public boolean canDeliverQuest(Tribe tribe) {
        return tribeService.canDeliverQuest(tribe.getActiveQuest(), economy);
    }

    public boolean deliverQuest(Tribe tribe) {
        Quest quest = tribe.getActiveQuest();
        if (quest == null) return false;

        boolean isWarriorQuest = quest.getType() == QuestType.WARRIOR_DEFEAT;
        String description = quest.getDescription();
        TribeRelationship before = tribe.getRelationship();
        if (!tribeService.completeQuest(tribe, quest, economy)) return false;

        if (isWarriorQuest) {
            for (int i = 0; i < 3; i++) {
                addUnit(new Unit(UnitType.SWORDSMAN, Townhall.getCol(), Townhall.getRow()));
            }
        }

        notifyTribeEvent(tribe, "Quest completed: " + description);
        checkRelationshipTierChange(tribe, before);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public boolean cancelTribeQuest(Tribe tribe) {
        boolean success = tribeService.cancelQuest(tribe);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public void removeDestroyedBuilding(Building building) {
        buildings.remove(building);
        Tile tile = tileGrid[building.getCol()][building.getRow()];
        if (tile.getBuilding() == building) {
            tile.setBuilding(null);
        }
    }

    public DisasterType rollForDisaster() {
        DisasterType disaster = disasterService.rollForDisaster(this);
        if (disaster != null) EventBus.publish(new HUDChangedEvent());
        return disaster;
    }

    public GameState captureState() {
        GameState state = new GameState();
        state.savedAtMillis = System.currentTimeMillis();
        state.tiles = Tiles;
        state.tileGrid = tileGrid;
        state.units = units;
        state.buildings = buildings;
        state.edgeFeatures = edgeFeatures;
        state.riverEdges = riverEdges;
        state.wallHP = wallHP;
        state.edgeOwners = edgeOwners;
        state.economy = economy;
        state.happinessManager = happinessManager;
        state.tribes = tribes;
        state.researchedTechs = researchedTechs;
        state.unitCount = unitCount;
        state.currentTurn = currentTurn;
        state.unitCapacity = unitCapacity;
        return state;
    }

    public PersistedServices captureServices() {
        PersistedServices services = new PersistedServices();
        services.combatService = combatService;
        services.disasterService = disasterService;
        services.tradeService = tradeService;
        services.tribeService = tribeService;
        return services;
    }

    public void restoreServices(PersistedServices services) {
        if (services == null) return;
        this.combatService = services.combatService;
        this.disasterService = services.disasterService;
        this.tradeService = services.tradeService;
        this.tribeService = services.tribeService;
    }

    public void restoreState(GameState state) {
        this.Tiles = state.tiles;
        this.tileGrid = state.tileGrid;
        this.units = state.units;
        this.buildings = state.buildings;
        this.edgeFeatures = state.edgeFeatures;
        this.riverEdges = state.riverEdges != null ? state.riverEdges : new HashSet<>();
        this.wallHP = state.wallHP != null ? state.wallHP : new HashMap<>();
        this.wallFailedUpkeep = new HashMap<>();
        this.edgeOwners = state.edgeOwners != null ? state.edgeOwners : new HashMap<>();
        this.economy = state.economy;
        this.happinessManager = state.happinessManager;
        this.tribes = state.tribes;
        this.researchedTechs = state.researchedTechs;
        this.unitCount = state.unitCount;
        this.currentTurn = state.currentTurn;
        this.unitCapacity = state.unitCapacity;

        this.Townhall = tileGrid[TownhallX][TownhallY];
        this.selectedUnit = null;
        this.tileUnderUnit = null;

        fogOfWarService = new FogOfWarService(ROWS, COLS, tileGrid, Tiles, units, buildings);
        updateFog();

        EventBus.publish(new HUDChangedEvent());
        EventBus.publish(new UnitActionsChangedEvent());
    }

    public boolean isSaveAllowed() {
        return !pendingAttack && pendingEdgeBuild == null && !pendingEdgeDeconstruct;
    }

    public boolean saveGame(int slot) {
        if (!isSaveAllowed()) return false;
        return saveLoadService.save(this, slot);
    }

    public boolean loadGame(int slot) {
        return saveLoadService.load(this, slot);
    }

    public boolean autosave() {
        return saveLoadService.autosave(this);
    }

    public String peekSaveSummary(int slot) {
        return saveLoadService.peekSummary(slot);
    }

    public boolean isSaveSlotCorrupted(int slot) {
        return saveLoadService.isSlotCorrupted(slot);
    }

    public boolean deleteSaveSlot(int slot) {
        return saveLoadService.deleteSlot(slot);
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean constructBuilding(BuildingType bType) {
        if (isInTribeForbiddenZone(tileUnderUnit.getCol(), tileUnderUnit.getRow())) return false;
        if(selectedUnit.getCurrentAP() < bType.getApCost()) return false;
        selectedUnit.setCurrentAP(selectedUnit.getCurrentAP() - BUILD_COST);

        if(!(economy.hasEnough(ResourceType.WOOD, bType.getWoodCost()) &&
            economy.hasEnough(ResourceType.STONE, bType.getStoneCost()) &&
            economy.hasEnough(ResourceType.IRON, bType.getIronCost())))
            return false;

        economy.spendResource(ResourceType.WOOD, bType.getWoodCost());
        economy.spendResource(ResourceType.STONE, bType.getStoneCost());
        economy.spendResource(ResourceType.IRON, bType.getIronCost());

        unitCapacity += bType.getUnitCapacityBonus();

        selectedUnit.useCharge();
        if(selectedUnit.getCharge() == 0){
            deleteUnit(selectedUnit);
            selectedUnit = null;
        }

        Building building = new Building(bType, tileUnderUnit.getCol(), tileUnderUnit.getRow());
        buildings.add(building);

        tileUnderUnit.setBuilding(building);
        if (bType == BuildingType.SETTLEMENT) happinessManager.addHappiness(-1);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public void assignWorkerToBuilding() {
        tileUnderUnit.getBuilding().addWorker(selectedUnit);
        selectedUnit.setAssigned(true);

        selectedUnit = null;
        tileUnderUnit = null;
        EventBus.publish(new HUDChangedEvent());
    }

    public void removeWorker() {
        Building building = tileUnderUnit.getBuilding();
        Unit worker = building.getLastWorker();
        building.removeWorker(worker);
        worker.setAssigned(false);
    }

    public boolean hasEnoughFood(int foodCost) {
        return(economy.hasEnoughFood(foodCost));
    }
    public boolean hasEnoughWood(int woodCost) {
        return(economy.hasEnough(ResourceType.WOOD, woodCost));
    }
    public boolean hasEnoughStone(int stoneCost) {
        return(economy.hasEnough(ResourceType.STONE, stoneCost));
    }
    public boolean hasEnoughIron(int ironCost) {
        return(economy.hasEnough(ResourceType.IRON, ironCost));
    }

    public void startProducingUnitInTownHall(UnitType uType) {
        if(Townhall.getBuilding().isProducing()){
            System.out.println("Townhall is busy :((");
            return;
        }
        if (uType == UnitType.CAVALRY && getTownHallLevel().getLevelNumber() < TownHallLevel.LEVEL_2.getLevelNumber()) {
            return;
        }
        if (uType == UnitType.CAVALRY && !hasStable()) {
            return;
        }

        int woodCost = uType == UnitType.SWORDSMAN ? 10 : 0;
        if (woodCost > 0 && !economy.hasEnough(ResourceType.WOOD, woodCost)) return;

        int cost = uType.getFoodCost();
        boolean isPaid = economy.spendFood(cost);
        EventBus.publish(new HUDChangedEvent());

        if(isPaid){
            if (woodCost > 0) economy.spendResource(ResourceType.WOOD, woodCost);
            Townhall.getBuilding().startProducing(uType);
            System.out.println("Producing Started :)))");
        }
    }

    public void cancelTownHallProduction() {
        Townhall.getBuilding().clearProduction();
        EventBus.publish(new HUDChangedEvent());
    }

    public boolean checkUnitCap(){
        return (getUnitCounts() < unitCapacity);
    }

    public boolean canStackAt(int col, int row, UnitType type) {
        if (type != UnitType.SWORDSMAN && type != UnitType.ARCHER && type != UnitType.CAVALRY) return true;

        int cap = (type == UnitType.CAVALRY) ? 1 : 2;
        int count = 0;
        for (Unit u : units) {
            if (u.getOwner() != null) continue;
            if (u.getCol() == col && u.getRow() == row && u.getType() == type) count++;
        }
        return count < cap;
    }

    public int getMilitaryUnitCount() {
        int count = 0;
        for (Unit u : units) {
            if (u.getOwner() != null) continue;
            if (u.getType() == UnitType.SWORDSMAN || u.getType() == UnitType.ARCHER || u.getType() == UnitType.CAVALRY) {
                count++;
            }
        }
        return count;
    }

    public int getEffectiveMilitaryUnitCap() {
        int cap = getTownHallLevel().getMilitaryUnitCap();
        for (Tribe tribe : tribes) {
            if (tribe.getType() == TribeType.WARRIOR && tribe.isAllianceActive()) {
                cap += 1;
            }
        }
        return cap;
    }

    public boolean checkMilitaryUnitCap() {
        return getMilitaryUnitCount() < getEffectiveMilitaryUnitCap();
    }

    public boolean isMilitaryUnit(UnitType type) {
        return type == UnitType.SWORDSMAN || type == UnitType.ARCHER || type == UnitType.CAVALRY;
    }

    public boolean hasMilitaryUnitInTownHall() {
        for (Unit u : units) {
            if (u.getOwner() != null) continue;
            if (isMilitaryUnit(u.getType()) && u.getCol() == Townhall.getCol() && u.getRow() == Townhall.getRow()) {
                return true;
            }
        }
        return false;
    }

    public void processWallUpkeep() {
        List<HexEdge> toRemove = new ArrayList<>();
        for (Map.Entry<HexEdge, EdgeFeature> entry : edgeFeatures.entrySet()) {
            if (entry.getValue() != EdgeFeature.WALL) continue;
            HexEdge edge = entry.getKey();
            if (economy.spendResource(ResourceType.STONE, 1)) {
                wallFailedUpkeep.remove(edge);
            } else {
                int fails = wallFailedUpkeep.getOrDefault(edge, 0) + 1;
                if (fails >= 3) {
                    toRemove.add(edge);
                } else {
                    wallFailedUpkeep.put(edge, fails);
                }
            }
        }
        for (HexEdge edge : toRemove) {
            edgeFeatures.remove(edge);
            wallHP.remove(edge);
            wallFailedUpkeep.remove(edge);
            edgeOwners.remove(edge);
        }
    }

    public boolean hasStable() {
        for (Building b : buildings) {
            if (b.getType() == BuildingType.STABLE) return true;
        }
        return false;
    }

    public boolean hasBuildingType(BuildingType type) {
        for (Building b : buildings) {
            if (b.getType() == type) return true;
        }
        return false;
    }

    public boolean canUseBazaar() {
        return tradeService.canUseBazaar();
    }

    public boolean canUseTradingPost() {
        return tradeService.canUseTradingPost();
    }

    public double bazaarRateForTier(int amount) {
        return tradeService.bazaarRateForTier(amount);
    }

    public boolean tradeAtBazaar(ResourceType from, ResourceType to, int tierAmount) {
        boolean success = tradeService.tradeAtBazaar(economy, from, to, tierAmount);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public boolean tradeAtTradingPost(ResourceType from, ResourceType to, int amount) {
        boolean success = tradeService.tradeAtTradingPost(economy, from, to, amount);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    public void resetTradeTurn() {
        tradeService.resetTurn();
        tribeService.resetTradeTurn();
    }

    public boolean canTradeWithTribe(Tribe tribe) {
        return tribeService.canTradeWith(tribe);
    }

    public boolean tradeWithTribe(Tribe tribe, ResourceType sellResource, int sellAmount, ResourceType rewardResource) {
        boolean success = tribeService.tradeWithTribe(tribe, economy, sellResource, sellAmount, rewardResource);
        if (success) EventBus.publish(new HUDChangedEvent());
        return success;
    }

    private void markTilesOwned(Tile center) {
        if (center.getTerrain() != TerrainType.MOUNTAIN_RANGE) center.setOwned();
        for (Tile tile: Tiles) {
            if (tile.getTerrain() == TerrainType.MOUNTAIN_RANGE) continue;
            if (HexUtils.isNeighbor(tile.getCol(), tile.getRow(), center.getCol(), center.getRow())) {
                tile.setOwned();
            }
        }
    }

    public void expandTerritory() {
        markTilesOwned(tileUnderUnit);
        happinessManager.addHappiness(-1);
        if(selectedUnit == null) return;
        deleteUnit(selectedUnit);
        selectedUnit = null;
    }

    public TownHallLevel getTownHallLevel() {
        return Townhall.getBuilding().getTownHallLevel();
    }

    public void applyTownHallStorage(TownHallLevel level) {
        economy.updateStorage(level.getCattleCapacity(), level.getWheatCapacity(), level.getWoodCapacity(),
                level.getStoneCapacity(), level.getIronCapacity(), level.getFishCapacity());
    }

    public boolean upgradeTownHall() {
        Building townHallBuilding = Townhall.getBuilding();
        if (townHallBuilding.isProducing()) return false;

        TownHallLevel nextLevel = townHallBuilding.getTownHallLevel().getNextLevel();
        if (nextLevel == null) return false;

        if(!(economy.hasEnough(ResourceType.WOOD, nextLevel.getUpgradeWoodCost()) &&
            economy.hasEnough(ResourceType.STONE, nextLevel.getUpgradeStoneCost()) &&
            economy.hasEnough(ResourceType.IRON, nextLevel.getUpgradeIronCost())))
            return false;

        economy.spendResource(ResourceType.WOOD, nextLevel.getUpgradeWoodCost());
        economy.spendResource(ResourceType.STONE, nextLevel.getUpgradeStoneCost());
        economy.spendResource(ResourceType.IRON, nextLevel.getUpgradeIronCost());

        townHallBuilding.startUpgrading(nextLevel);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    // "is" ha ro "has" kardam ke tamiz tar beshe yeho nagid ai e :((

    public boolean hasStoneTech() {
        return hasTech(TechType.STONE_MINING);
    }

    public boolean hasIronTech() {
        return hasTech(TechType.IRON_MINING);
    }

    public boolean hasSettlementTech() {
        return hasTech(TechType.SETTLEMENT_TECH);
    }

    public boolean hasProToolsTech() {
        return hasTech(TechType.PRO_TOOLS);
    }

    public boolean hasTech(TechType tech) {
        return researchedTechs.getOrDefault(tech, false);
    }

    public boolean hasEnoughResource(ResourceType type, int amount) {
        return economy.hasEnough(type, amount);
    }

    public boolean researchTech(TechType tech) {
        if (hasTech(tech)) return false;
        if (getTownHallLevel().getLevelNumber() < tech.getRequiredLevel().getLevelNumber()) return false;
        if (!economy.hasEnough(tech.getCostResource(), tech.getCostAmount())) return false;

        if (tech.isInstant()) {
            if (tech.getCostAmount() > 0) economy.spendResource(tech.getCostResource(), tech.getCostAmount());
            completeTechResearch(tech);
            return true;
        }

        Building townHallBuilding = Townhall.getBuilding();
        if (townHallBuilding.isProducing()) return false;

        if (tech.getCostAmount() > 0) economy.spendResource(tech.getCostResource(), tech.getCostAmount());
        townHallBuilding.startResearching(tech);
        EventBus.publish(new HUDChangedEvent());
        return true;
    }

    public void completeTechResearch(TechType tech) {
        researchedTechs.put(tech, true);

        if (tech == TechType.DEFENSIVE_ARCHITECTURE) {
            Building townHallBuilding = Townhall.getBuilding();
            int hpGain = 350 - townHallBuilding.getMaxHP();
            townHallBuilding.setMaxHP(350);
            if (hpGain > 0) townHallBuilding.heal(hpGain);
            buildWallsAroundTownHall();
        }

        EventBus.publish(new HUDChangedEvent());
    }

    private void buildWallsAroundTownHall() {
        for (Tile t : Tiles) {
            if (!HexUtils.isNeighbor(Townhall.getCol(), Townhall.getRow(), t.getCol(), t.getRow())) continue;
            if (t.getTerrain() == TerrainType.SEA || t.getTerrain() == TerrainType.MOUNTAIN_RANGE) continue;

            HexEdge edge = new HexEdge(Townhall.getCol(), Townhall.getRow(), t.getCol(), t.getRow());
            edgeFeatures.put(edge, EdgeFeature.WALL);
            wallHP.put(edge, WALL_MAX_HP);
        }
    }

    public void updateNetChanges(){
        turnProcessor.previewNextTurnChanges();
    }

    public int getUnitCounts(){
        int count = 0;
        for (Unit u : units) {
            if (u.getOwner() == null) count++;
        }
        return count;
    }

    public int getUnitCounts(UnitType type){
        return unitCount.getOrDefault(type, 0);
    }

    public int getUnitCapacity(){
        return unitCapacity;
    }

    public boolean hasUnitsWithRemainingAP() {
        for(Unit unit: units){
            if(unit.getCurrentAP() > 0) return true;
        }
        return false;
    }
}
