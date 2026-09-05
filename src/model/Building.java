package model;

import java.util.ArrayList;

public class Building implements java.io.Serializable {
    private int col, row;

    private final BuildingType type;
    private boolean isOccupied;
    private ArrayList<Unit> workers;

    private UnitType producingUnit = null;
    private TownHallLevel upgradingToLevel = null;
    private TechType researchingTech = null;
    private int productionTurnsLeft = 0;

    private int failedCount = 0;

    private TownHallLevel townHallLevel;
    private int hp;
    private int maxHP;
    private int disabledUntilTurn = 0;
    private Tribe owner;


    public Building(BuildingType type, int col, int row) {
        this.col = col;
        this.row = row;

        this.type = type;
        this.isOccupied = false;
        workers = new ArrayList<>();

        if (type == BuildingType.TOWN_HALL) {
            this.townHallLevel = TownHallLevel.LEVEL_1;
            this.maxHP = townHallLevel.getMaxHP();
        } else {
            this.maxHP = 50;
        }
        this.hp = maxHP;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public BuildingType getType() {
        return type;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void addWorker(Unit worker){
        workers.add(worker);
        isOccupied = true;
    }

    public void removeWorker(Unit worker){
        workers.remove(worker);
        if(workers.isEmpty()) isOccupied = false;
    }

    public boolean needWorker() {
        return workers.size() < type.getMaxWorkerCapacity();
    }

    public ArrayList<Unit> getStationedWorkers() {
        return workers;
    }

    public Unit getLastWorker() {
        return workers.get(workers.size() - 1);
    }

    public void startProducing(UnitType type) {
        this.producingUnit = type;
        this.productionTurnsLeft = type.getBuildTurns();
    }

    public void startUpgrading(TownHallLevel targetLevel) {
        this.upgradingToLevel = targetLevel;
        this.productionTurnsLeft = targetLevel.getUpgradeTurns();
    }

    public void startResearching(TechType tech) {
        this.researchingTech = tech;
        this.productionTurnsLeft = tech.getResearchTurns();
    }

    public boolean isProducing() {
        return (producingUnit != null || upgradingToLevel != null || researchingTech != null);
    }

    public UnitType getProducingUnit() {
        return producingUnit;
    }

    public TownHallLevel getUpgradingToLevel() {
        return upgradingToLevel;
    }

    public TechType getResearchingTech() {
        return researchingTech;
    }

    public int getProductionTurnsLeft() {
        return productionTurnsLeft;
    }

    public void decrementProductionTurns() {
        productionTurnsLeft--;
    }

    public void clearProduction() {
        producingUnit = null;
        upgradingToLevel = null;
        researchingTech = null;
        productionTurnsLeft = 0;
    }

    public void upkeepFailed() {
        failedCount++;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public TownHallLevel getTownHallLevel() {
        return townHallLevel;
    }

    public void applyLevelUpgrade() {
        this.townHallLevel = upgradingToLevel;
        heal(townHallLevel.getHealOnUpgrade());
    }

    public int getHP() {
        return hp;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public void setMaxHP(int maxHP) {
        this.maxHP = maxHP;
    }

    public void heal(int amount) {
        hp = Math.min(maxHP, hp + amount);
    }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    public int getDisabledUntilTurn() {
        return disabledUntilTurn;
    }

    public void setDisabledUntilTurn(int turn) {
        this.disabledUntilTurn = turn;
    }

    public Tribe getOwner() {
        return owner;
    }

    public void setOwner(Tribe owner) {
        this.owner = owner;
    }
}
