package controller.services;

import model.GlobalResourceManager;
import model.ResourceType;

public class TradeService implements java.io.Serializable {
    private boolean bazaarTradeUsedThisTurn = false;
    private boolean tradingPostTradeUsedThisTurn = false;

    public boolean canUseBazaar() {
        return !bazaarTradeUsedThisTurn;
    }

    public boolean canUseTradingPost() {
        return !tradingPostTradeUsedThisTurn;
    }

    public double bazaarRateForTier(int amount) {
        if (amount == 10) return 0.50;
        if (amount == 100) return 0.60;
        if (amount == 500) return 0.70;
        return 0;
    }

    public boolean tradeAtBazaar(GlobalResourceManager economy, ResourceType from, ResourceType to, int tierAmount) {
        if (bazaarTradeUsedThisTurn || from == to) return false;

        double rate = bazaarRateForTier(tierAmount);
        if (rate <= 0) return false;
        if (!economy.hasEnough(from, tierAmount)) return false;

        int reward = (int) Math.floor(tierAmount * rate);
        if (!economy.hasCapacityFor(to, reward)) return false;

        economy.spendResource(from, tierAmount);
        economy.addResource(to, reward);
        bazaarTradeUsedThisTurn = true;
        return true;
    }

    public boolean tradeAtTradingPost(GlobalResourceManager economy, ResourceType from, ResourceType to, int amount) {
        if (tradingPostTradeUsedThisTurn || from == to || amount <= 0) return false;
        if (!economy.hasEnough(from, amount)) return false;

        int reward = (int) Math.floor(amount * 0.80);
        if (!economy.hasCapacityFor(to, reward)) return false;

        economy.spendResource(from, amount);
        economy.addResource(to, reward);
        tradingPostTradeUsedThisTurn = true;
        return true;
    }

    public void resetTurn() {
        bazaarTradeUsedThisTurn = false;
        tradingPostTradeUsedThisTurn = false;
    }
}
