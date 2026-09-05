package model;

public enum TribeRelationship {
    ENEMY, DISPLEASED, NEUTRAL, FRIENDLY, ALLIED;

    public static TribeRelationship fromValue(int value) {
        if (value <= -50) return ENEMY;
        if (value <= -20) return DISPLEASED;
        if (value <= 19) return NEUTRAL;
        if (value <= 69) return FRIENDLY;
        return ALLIED;
    }
}
