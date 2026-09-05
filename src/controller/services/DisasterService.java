package controller.services;

import controller.GameController;
import controller.events.DisasterEvent;
import controller.events.EventBus;
import model.Building;
import model.BuildingType;
import model.DisasterType;
import model.HexUtils;
import model.Season;
import model.TerrainType;
import model.Tile;
import model.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DisasterService implements java.io.Serializable {
    private static final double DISASTER_CHANCE = 0.05;
    private static final int EARTHQUAKE_RADIUS = 2;
    private static final int EARTHQUAKE_UNIT_DAMAGE = 10;
    private static final int EARTHQUAKE_TOWNHALL_DAMAGE = 50;
    private static final int FLOOD_UNIT_DAMAGE = 20;
    private static final int FLOOD_BUILDING_DAMAGE = 30;

    private final Random random = new Random();

    private int lastCenterCol, lastCenterRow, lastRadius;

    public DisasterType rollForDisaster(GameController gc) {
        if (random.nextDouble() >= DISASTER_CHANCE) return null;

        boolean isAutumn = gc.getCurrentSeason() == Season.AUTUMN;
        boolean tryFlood = isAutumn && random.nextBoolean();

        String message = tryFlood ? applyFlood(gc) : null;
        DisasterType type = DisasterType.FLOOD;

        if (message == null) {
            message = applyEarthquake(gc);
            type = DisasterType.EARTHQUAKE;
        }
        if (message == null) return null;

        EventBus.publish(new DisasterEvent(type, message, lastCenterCol, lastCenterRow, lastRadius));
        return type;
    }

    private String applyEarthquake(GameController gc) {
        List<Tile> dryTiles = new ArrayList<>();
        for (Tile t : gc.getTiles()) {
            if (t.getTerrain().isPassable() && t.getTerrain() != TerrainType.SEA) dryTiles.add(t);
        }
        if (dryTiles.isEmpty()) return null;

        Tile center = dryTiles.get(random.nextInt(dryTiles.size()));
        List<Tile> area = HexUtils.hexesWithinRadius(center.getCol(), center.getRow(), EARTHQUAKE_RADIUS, gc.getTiles());

        for (Tile t : area) {
            damageUnitsAt(gc, t.getCol(), t.getRow(), EARTHQUAKE_UNIT_DAMAGE, false);

            Building b = t.getBuilding();
            if (b != null && b.getType() == BuildingType.TOWN_HALL) {
                int damage = Math.min(EARTHQUAKE_TOWNHALL_DAMAGE, b.getHP() - 1);
                if (damage > 0) b.takeDamage(damage);
            }
        }
        lastCenterCol = center.getCol();
        lastCenterRow = center.getRow();
        lastRadius = EARTHQUAKE_RADIUS;
        return "An earthquake struck near hex (" + center.getCol() + ", " + center.getRow() + ")!";
    }

    private String applyFlood(GameController gc) {
        List<Tile> candidates = new ArrayList<>();
        for (Tile t : gc.getTiles()) {
            TerrainType terrain = t.getTerrain();
            boolean floodableTerrain = terrain == TerrainType.PLAIN || terrain == TerrainType.MEADOW ||
                    terrain == TerrainType.FOREST;
            if (!floodableTerrain) continue;
            if (isNearRiverOrCoast(gc, t)) candidates.add(t);
        }
        if (candidates.isEmpty()) return null;

        Tile center = candidates.get(random.nextInt(candidates.size()));
        List<Tile> area = new ArrayList<>();
        area.add(center);
        for (Tile t : gc.getTiles()) {
            TerrainType terrain = t.getTerrain();
            if (terrain == TerrainType.SEA || terrain == TerrainType.MOUNTAIN || terrain == TerrainType.MOUNTAIN_RANGE) {
                continue;
            }
            if (HexUtils.isNeighbor(center.getCol(), center.getRow(), t.getCol(), t.getRow())) area.add(t);
        }

        for (Tile t : area) {
            damageUnitsAt(gc, t.getCol(), t.getRow(), FLOOD_UNIT_DAMAGE, true);
            gc.destroyRoadsTouching(t.getCol(), t.getRow());

            Building b = t.getBuilding();
            if (b == null) continue;

            if (b.getType() == BuildingType.FARM) {
                gc.removeDestroyedBuilding(b);
            } else {
                b.takeDamage(FLOOD_BUILDING_DAMAGE);
                b.setDisabledUntilTurn(gc.getCurrentTurn() + 1);
                if (b.isDestroyed()) gc.removeDestroyedBuilding(b);
            }
        }
        lastCenterCol = center.getCol();
        lastCenterRow = center.getRow();
        lastRadius = 1;
        return "A flood struck near hex (" + center.getCol() + ", " + center.getRow() + ")!";
    }

    private boolean isNearRiverOrCoast(GameController gc, Tile t) {
        if (gc.hasRiverEdgeTouching(t.getCol(), t.getRow())) return true;

        for (Tile other : gc.getTiles()) {
            if (other.getTerrain() == TerrainType.SEA &&
                    HexUtils.isNeighbor(t.getCol(), t.getRow(), other.getCol(), other.getRow())) {
                return true;
            }
        }
        return false;
    }

    private void damageUnitsAt(GameController gc, int col, int row, int amount, boolean zeroAP) {
        for (Unit u : new ArrayList<>(gc.getUnits())) {
            if (u.getCol() != col || u.getRow() != row) continue;

            u.takeDamage(amount);
            if (zeroAP) u.setCurrentAP(0);
            if (u.isDead()) gc.deleteUnit(u);
        }
    }
}
