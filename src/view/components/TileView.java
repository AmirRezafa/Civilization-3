package view.components;

import model.TerrainType;
import model.Tile;

import java.awt.*;

import static view.Utils.brighten;
import static view.Utils.darken;

public class TileView {
    public static void show(double centerX, double centerY, int radius, Graphics2D g2, Color terrainColor, Tile tile) {
        boolean isVisible = tile.isVisible();
        boolean wasExplored = tile.isExplored();

        double drawRadius = radius * 0.92;

        int[] x = new int[6];
        int[] y = new int[6];

        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            x[i] = (int)(centerX + (drawRadius * Math.cos(angle)));
            y[i] = (int)(centerY + (drawRadius * Math.sin(angle)));
        }

        Polygon hex = new Polygon(x, y, 6);

        Color finalColor;
        if (!wasExplored) {
            finalColor = Color.BLACK;
        } else if (!isVisible) {
            finalColor = darken(terrainColor, 0.5f);
        } else {
            finalColor = terrainColor;
        }

        if (wasExplored && isVisible) {
            Color lighter = brighten(finalColor, 0.3f);

            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point((int)centerX, (int)centerY),
                    (float)drawRadius,
                    new float[]{0.0f, 1.0f},
                    new Color[]{lighter, finalColor}
            );
            g2.setPaint(rgp);
        } else {
            g2.setColor(finalColor);
        }

        g2.fillPolygon(hex);

        if (wasExplored) {
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(0, 0, 0, 120));
            g2.drawPolygon(hex);
        }

        if (wasExplored && isVisible && tile.getTerrain() == TerrainType.MOUNTAIN_RANGE) {
            Shape oldClip = g2.getClip();
            g2.clip(hex);
            g2.setColor(new Color(20, 20, 25, 160));
            g2.setStroke(new BasicStroke(Math.max(1.5f, radius * 0.06f)));
            int step = Math.max(4, (int) (radius * 0.3));
            for (int off = -(int) drawRadius * 2; off < drawRadius * 2; off += step) {
                int lx1 = (int) (centerX - drawRadius) + off;
                int ly1 = (int) (centerY - drawRadius);
                int lx2 = lx1 + (int) (drawRadius * 2);
                int ly2 = (int) (centerY + drawRadius);
                g2.drawLine(lx1, ly1, lx2, ly2);
            }
            g2.setClip(oldClip);
        }

        g2.setStroke(new BasicStroke(1.0f));

        if (wasExplored && tile.isOwned()) {
            g2.setColor(new Color(52, 152, 219, 50));
            g2.fillPolygon(hex);

            g2.setColor(new Color(52, 152, 219, 200));
            g2.setStroke(new BasicStroke(3f));
            g2.drawPolygon(hex);
        }
    }

}
