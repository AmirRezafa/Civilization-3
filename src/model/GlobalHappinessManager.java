package model;

public class GlobalHappinessManager implements java.io.Serializable {
    private int accumulatedEvents = 0;
    private int steadyBonus = 0;

    public void addHappiness(int delta) {
        accumulatedEvents += delta;
    }

    public void setSteadyBonus(int value) {
        steadyBonus = value;
    }

    public int getHappiness() {
        return accumulatedEvents + steadyBonus;
    }

    public boolean isGoldenAge() {
        return getHappiness() >= 3;
    }

    public boolean isDissatisfied() {
        return getHappiness() <= -3 && getHappiness() >= -4;
    }

    public boolean isRiot() {
        return getHappiness() <= -5;
    }

    public String getLevelLabel() {
        if (isGoldenAge()) return "Golden Age";
        if (isRiot()) return "Riot";
        if (isDissatisfied()) return "Dissatisfaction";
        return "Normal";
    }
}
