package controller.events;

public class TribeEvent {
    private final String tribeName;
    private final String message;

    public TribeEvent(String tribeName, String message) {
        this.tribeName = tribeName;
        this.message = message;
    }

    public String getTribeName() {
        return tribeName;
    }

    public String getMessage() {
        return message;
    }
}
