package controller.services;

import java.io.Serializable;

/** Bundles the mutable, Random-holding services so their RNG state survives save/load. */
public class PersistedServices implements Serializable {
    public CombatService combatService;
    public DisasterService disasterService;
    public TradeService tradeService;
    public TribeService tribeService;
}
