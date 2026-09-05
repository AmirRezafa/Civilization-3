package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameState implements Serializable {
    public static final int CURRENT_SAVE_VERSION = 1;

    public int saveVersion = CURRENT_SAVE_VERSION;
    public long savedAtMillis;
    public ArrayList<Tile> tiles;
    public Tile[][] tileGrid;
    public ArrayList<Unit> units;
    public ArrayList<Building> buildings;
    public Map<HexEdge, EdgeFeature> edgeFeatures;
    public Set<HexEdge> riverEdges;
    public Map<HexEdge, Integer> wallHP;
    public Map<HexEdge, Tribe> edgeOwners;
    public GlobalResourceManager economy;
    public GlobalHappinessManager happinessManager;
    public List<Tribe> tribes;
    public Map<TechType, Boolean> researchedTechs;
    public Map<UnitType, Integer> unitCount;
    public int currentTurn;
    public int unitCapacity;
}
