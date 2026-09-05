package model;

public enum Season {
    SPRING(1, 0, 0),
    SUMMER(0, 0, 0),
    AUTUMN(0, 0, 1),
    WINTER(-1, 1, 0);

    private final int farmFoodBonus;
    private final int landMovementPenalty;
    private final int waterMovementPenalty;

    Season(int farmFoodBonus, int landMovementPenalty, int waterMovementPenalty) {
        this.farmFoodBonus = farmFoodBonus;
        this.landMovementPenalty = landMovementPenalty;
        this.waterMovementPenalty = waterMovementPenalty;
    }

    public int getFarmFoodBonus() {
        return farmFoodBonus;
    }

    public int getLandMovementPenalty() {
        return landMovementPenalty;
    }

    public int getWaterMovementPenalty() {
        return waterMovementPenalty;
    }

    public static Season fromTurn(int turn) {
        int cycle = ((turn - 1) % 40) / 10;
        return values()[cycle];
    }
}
