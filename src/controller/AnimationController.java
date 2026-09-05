package controller;

import model.Unit;

import java.sql.SQLOutput;

public class AnimationController {
    private final double INTERPOLATION_FACTOR = 0.12;

    private GameController GC;

    public AnimationController(GameController GC) {
        this.GC = GC;
    }

    public void run(){
        for(Unit unit: GC.getUnits()){
            if(unit.isMoving()){
                unit.setX(unit.getX() + (unit.getTargetX() - unit.getX()) * INTERPOLATION_FACTOR);
                unit.setY(unit.getY() + (unit.getTargetY() - unit.getY()) * INTERPOLATION_FACTOR);

                double distance = Math.hypot(unit.getTargetX() - unit.getX(),
                        unit.getTargetY() - unit.getY());
                if(distance < 0.001){
                    unit.animationDone();
                }
            }
        }
    }
}
