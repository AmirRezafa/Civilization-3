package model;

public class Quest implements java.io.Serializable {
    private final QuestType type;
    private final int deadlineTurn;
    private boolean completed = false;
    private boolean readyToDeliver = false;
    private int defeatsRecorded = 0;

    public Quest(QuestType type, int issuedTurn) {
        this.type = type;
        this.deadlineTurn = issuedTurn + type.getDeadlineTurns();
    }

    public QuestType getType() {
        return type;
    }

    public String getDescription() {
        return type.getDisplayName();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }

    public boolean isReadyToDeliver() {
        return readyToDeliver;
    }

    public void markReadyToDeliver() {
        readyToDeliver = true;
    }

    public int getDeadlineTurn() {
        return deadlineTurn;
    }

    public boolean isExpired(int currentTurn) {
        return !completed && !readyToDeliver && currentTurn > deadlineTurn;
    }

    public void recordDefeat() {
        defeatsRecorded++;
    }

    public int getDefeatsRecorded() {
        return defeatsRecorded;
    }
}
