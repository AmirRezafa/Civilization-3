package model;

public class Unit implements java.io.Serializable {
    private UnitType type;
    private int col;
    private int row;

    private int currentAP;
    private boolean isMoving;
    private double x, y;
    private double targetX, targetY;

    private int charge;
    private boolean assigned = false;

    private int hp;

    private Tribe owner;

    public Unit(UnitType type, int startCol, int startRow) {
        this.type = type;
        this.col = startCol;
        this.row = startRow;
        x = HexUtils.centerX(col);
        y = HexUtils.centerY(col, row);
        this.currentAP = type.getMaxAP();
        this.charge = type.getChargesCount();
        this.isMoving = false;
        this.hp = type.getMaxHP();
    }

    public void placeAt(int newCol, int newRow) {
        this.col = newCol;
        this.row = newRow;
        this.isMoving = false;
        x = HexUtils.centerX(col);
        y = HexUtils.centerY(col, row);
        targetX = x;
        targetY = y;
    }

    public boolean move(int targetCol, int targetRow, int movementCost) {
        if (this.currentAP >= movementCost) {
            this.currentAP -= movementCost;

            this.col = targetCol;
            this.row = targetRow;
            targetX = HexUtils.centerX(col);
            targetY = HexUtils.centerY(col, row);

            this.isMoving = true;

            return true;
        }
        return false;
    }

    public UnitType getType() {
        return type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public int getCurrentAP() {
        return currentAP;
    }

    public void setCurrentAP(int currentAP) {
        this.currentAP = currentAP;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void animationDone() {
        isMoving = false;
        x = targetX;
        y = targetY;
    }

    public void useCharge(){
        charge--;
    }
    public int getCharge() {
        return charge;
    }

    public void resetActionPoints(boolean less){
        if(less) currentAP = type.getMaxAP() - 2;
        currentAP = type.getMaxAP();
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public int getHP() {
        return hp;
    }

    public void takeHit() {
        hp--;
    }

    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
    }

    public boolean isDead() {
        return hp <= 0;
    }

    public Tribe getOwner() {
        return owner;
    }

    public void setOwner(Tribe owner) {
        this.owner = owner;
    }
}
