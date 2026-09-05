package controller;

import view.Ground;

import java.awt.event.*;

public class Camera implements MouseMotionListener, MouseListener, MouseWheelListener {
    private final Ground ground;

    private int xOffset = 0, yOffset = 0;
    private double xVelocity = 0, yVelocity = 0;

    private int left = 0, right = 0;
    private int up = 0, down = 0;

    private double scale = 2.0;

    public Camera(Ground ground) {
        this.ground = ground;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        left = Math.max(0, (int)((ground.getwidth() * 0.1) - e.getX()));
        right = Math.max(0, (int)(e.getX() - (ground.getwidth() * 0.9)));
        up = Math.max(0, (int)((ground.getheight() * 0.1) - e.getY()));
        down = Math.max(0, (int)(e.getY() - (ground.getheight() * 0.9)));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    public void run() {
        double targetXVelocity = (right / 3.0) - (left / 3.0);
        double targetYVelocity = (down / 3.0) - (up / 3.0);

        xVelocity = (xVelocity * 0.6) + (targetXVelocity * 0.4);
        yVelocity = (yVelocity * 0.6) + (targetYVelocity * 0.4);

        if (Math.abs(xVelocity) < 0.15 && targetXVelocity == 0) xVelocity = 0;
        if (Math.abs(yVelocity) < 0.15 && targetYVelocity == 0) yVelocity = 0;

        xOffset += (int) xVelocity;
        yOffset += (int) yVelocity;

        xOffset = Math.max(0, xOffset);
        yOffset = Math.max(0, yOffset);

        int a = getA();
        double h = a * Math.sqrt(3);

        int maxOffsetX = (int)(a * ((GameController.COLS * 1.5) + 2)) - ground.getwidth();
        int maxOffsetY = (int)(h * GameController.ROWS + 2 * a) - ground.getheight();

        if (xOffset > maxOffsetX) {
            xOffset = maxOffsetX;
            xVelocity = 0;
        }
        if (yOffset > maxOffsetY) {
            yOffset = maxOffsetY;
            yVelocity = 0;
        }

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int oldA = getA();

        if (e.getWheelRotation() < 0) {
            scale = Math.min(5, scale + 0.1);
        } else {
            scale = Math.max(0.6, scale - 0.1);
        }

        int newA = getA();

        if (oldA > 0) {
            double ratio = (double) newA / oldA;
            xOffset = (int) ((e.getX() + xOffset) * ratio - e.getX());
            yOffset = (int) ((e.getY() + yOffset) * ratio - e.getY());
        }

        xOffset = Math.max(0, xOffset);
        yOffset = Math.max(0, yOffset);
        double h = newA * Math.sqrt(3);
        int maxOffsetX = (int)(newA * ((GameController.COLS * 1.5) + 2)) - ground.getwidth();
        int maxOffsetY = (int)(h * GameController.ROWS + 2 * newA) - ground.getheight();
        if (xOffset > maxOffsetX) xOffset = maxOffsetX;
        if (yOffset > maxOffsetY) yOffset = maxOffsetY;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
        left = 0;
        right = 0;
        up = 0;
        down = 0;
    }

    public int getA() {
        int baseA = Math.min(ground.getheight(), ground.getwidth()) / 45;
        if (baseA <= 0) baseA = 16;
        return (int) (baseA * scale);
    }

    public int getB() {
        int baseB = Math.min(ground.getheight(), ground.getwidth()) / 45;
        if (baseB <= 0) baseB = 16;
        return baseB * 2;
    }
}
