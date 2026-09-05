package view.components;

import model.Tile;
import model.ResourceType;
import java.awt.*;

public class ResourceView {
    public static void show(Tile tile, int a, double x, double y, Graphics2D g2) {
        if (!tile.isExplored() || tile.getResources() == null) {
            return;
        }

        int iconX = (int) x - (a / 6);
        int iconY = (int) y + (a / 3);
        int size = a / 3;
        int offset = 0;

        if (tile.hasResource(ResourceType.WOOD)) {
            drawTree(g2, iconX + offset, iconY, size);
            offset += (int) (size * 1.2);
        }

        if (tile.hasResource(ResourceType.STONE)) {
            drawRock(g2, iconX + offset, iconY, size);
            offset += (int) (size * 1.2);
        }

        if (tile.hasResource(ResourceType.IRON)) {
            drawIron(g2, iconX + offset, iconY, size);
            offset += (int) (size * 1.2);
        }

        if (tile.hasResource(ResourceType.WHEAT)) {
            drawWheat(g2, iconX + offset, iconY, size);
            offset += (int) (size * 1.2);
        }

        if (tile.hasResource(ResourceType.CATTLE)) {
            drawCattle(g2, iconX + offset, iconY, size);
        }
    }

    private static void drawTree(Graphics2D g2, int x, int y, int size) {
        g2.setColor(new Color(34, 139, 34));
        int[] xPoints = {x, x - size / 2, x + size / 2};
        int[] yPoints = {y - size / 2, y + size / 2, y + size / 2};
        g2.fillPolygon(xPoints, yPoints, 3);

        int[] yPoints2 = {y - size, y, y};
        g2.fillPolygon(xPoints, yPoints2, 3);
    }

    private static void drawRock(Graphics2D g2, int x, int y, int size) {
        g2.setColor(Color.GRAY);
        int[] xPoints = {x, x - size / 2, x - size / 4, x + size / 2, x + size / 3};
        int[] yPoints = {y - size / 2, y, y + size / 2, y + size / 3, y - size / 4};
        g2.fillPolygon(xPoints, yPoints, 5);
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(xPoints, yPoints, 5);
    }

    private static void drawIron(Graphics2D g2, int x, int y, int size) {
        g2.setColor(new Color(176, 196, 222));
        int[] xPoints = {x, x - size / 2, x, x + size / 2};
        int[] yPoints = {y - size / 2, y, y + size / 2, y};
        g2.fillPolygon(xPoints, yPoints, 4);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(xPoints, yPoints, 4);
    }

    private static void drawWheat(Graphics2D g2, int x, int y, int size) {
        g2.setColor(new Color(241, 196, 15));
        g2.fillOval(x - size / 4, y - size / 2, size / 2, size);
        g2.setColor(new Color(211, 84, 0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x, y - size / 2, x, y + size / 2);
    }

    private static void drawCattle(Graphics2D g2, int x, int y, int size) {
        g2.setColor(new Color(211, 84, 0));
        g2.fillOval(x - size / 2, y - size / 4, size, (int)(size / 1.5));
        g2.setColor(new Color(236, 240, 241));
        g2.fillOval(x - size / 3, y - size / 3, size / 3, size / 3);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x - size / 2, y - size / 4, size, (int)(size / 1.5));
    }
}