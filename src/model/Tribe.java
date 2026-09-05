package model;

public class Tribe implements java.io.Serializable {
    private final int col, row;
    private final TribeType type;
    private final String name;
    private int relationshipValue = 0;
    private Quest activeQuest;
    private int questCooldownUntilTurn = 0;
    private int lastQuestFailedTurn = -999;
    private boolean allianceActive = false;
    private boolean trespassWarned = false;

    public Tribe(int col, int row, TribeType type, String name) {
        this.col = col;
        this.row = row;
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TribeType getType() {
        return type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getRelationshipValue() {
        return relationshipValue;
    }

    public void changeRelationship(int delta) {
        relationshipValue = Math.max(-100, Math.min(100, relationshipValue + delta));
        deactivateAllianceIfBelowThreshold();
    }

    public void setRelationshipValue(int value) {
        relationshipValue = Math.max(-100, Math.min(100, value));
        deactivateAllianceIfBelowThreshold();
    }

    private void deactivateAllianceIfBelowThreshold() {
        if (allianceActive && relationshipValue < 70) allianceActive = false;
    }

    public boolean isAllianceActive() {
        return allianceActive;
    }

    public void setAllianceActive(boolean active) {
        this.allianceActive = active;
    }

    public TribeRelationship getRelationship() {
        return TribeRelationship.fromValue(relationshipValue);
    }

    public Quest getActiveQuest() {
        return activeQuest;
    }

    public void setActiveQuest(Quest quest) {
        this.activeQuest = quest;
    }

    public int getQuestCooldownUntilTurn() {
        return questCooldownUntilTurn;
    }

    public void setQuestCooldownUntilTurn(int turn) {
        this.questCooldownUntilTurn = turn;
    }

    public int getLastQuestFailedTurn() {
        return lastQuestFailedTurn;
    }

    public void setLastQuestFailedTurn(int turn) {
        this.lastQuestFailedTurn = turn;
    }

    public int getGuardUnitCap() {
        return type == TribeType.WARRIOR ? 5 : 3;
    }

    public boolean isTrespassWarned() {
        return trespassWarned;
    }

    public void setTrespassWarned(boolean trespassWarned) {
        this.trespassWarned = trespassWarned;
    }
}
