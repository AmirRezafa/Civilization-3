package controller.events;

import model.DisasterType;

public class DisasterEvent {
    private final DisasterType type;
    private final String message;
    private final int centerCol;
    private final int centerRow;
    private final int radius;

    public DisasterEvent(DisasterType type, String message, int centerCol, int centerRow, int radius) {
        this.type = type;
        this.message = message;
        this.centerCol = centerCol;
        this.centerRow = centerRow;
        this.radius = radius;
    }

    public DisasterType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getCenterCol() {
        return centerCol;
    }

    public int getCenterRow() {
        return centerRow;
    }

    public int getRadius() {
        return radius;
    }
}
