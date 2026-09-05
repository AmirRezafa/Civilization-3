package view.components;

import model.Unit;
import java.awt.*;

public class UnitView {
    public static void show(Unit unit, int a, boolean selected, Graphics2D g2) {
        show(unit, a, selected, false, g2);
    }

    public static void show(Unit unit, int a, boolean selected, boolean afloat, Graphics2D g2) {
        double x = unit.getX() * a;
        double y = unit.getY() * a;

        if (afloat) {
            int hullHalfWidth = a / 2;
            int hullTop = (int) y + a / 6;
            int hullBottom = (int) y + a / 2;
            int[] hullX = {(int) x - hullHalfWidth, (int) x + hullHalfWidth, (int) x + hullHalfWidth / 2, (int) x - hullHalfWidth / 2};
            int[] hullY = {hullTop, hullTop, hullBottom, hullBottom};
            g2.setColor(new Color(101, 67, 33));
            g2.fillPolygon(hullX, hullY, 4);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(hullX, hullY, 4);
        }

        Color unitColor;
        String label;

        switch (unit.getType()) {
            case EXPLORER:
                unitColor = new Color(52, 152, 219);
                label = "E";
                break;
            case WORKER:
                unitColor = new Color(230, 126, 34);
                label = "W";
                break;
            case BUILDER:
                unitColor = new Color(155, 89, 182);
                label = "B";
                break;
            case BORDER_EXPANDER:
                unitColor = new Color(26, 188, 156);
                label = "BE"; //Momkene bezane biroon choon 2 harfi e
                break;
            case SWORDSMAN:
                unitColor = new Color(192, 57, 43);
                label = "S";
                break;
            case ARCHER:
                unitColor = new Color(212, 172, 13);
                label = "A";
                break;
            case CAVALRY:
                unitColor = new Color(120, 66, 18);
                label = "C";
                break;
            default:
                unitColor = Color.RED;
                label = "U";
                break;
        }

        if (selected) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval((int)x - a/2, (int)y - a/2, a, a);
        }

        if (unit.getOwner() != null) {
            g2.setColor(new Color(139, 0, 0));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval((int)x - a/4 - 3, (int)y - a/4 - 3, a/2 + 6, a/2 + 6);
        }

        g2.setColor(unitColor);
        g2.fillOval((int)x - a/4, (int)y - a/4, a/2, a/2);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval((int)x - a/4, (int)y - a/4, a/2, a/2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, a / 4));

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textHeight = fm.getAscent() - fm.getDescent();

        g2.drawString(label, (int)x - (textWidth / 2), (int)y + (textHeight / 2));

        int maxHP = unit.getType().getMaxHP();
        if (maxHP > 0 && unit.getHP() < maxHP) {
            int barWidth = a / 2;
            int barHeight = Math.max(3, a / 12);
            int barX = (int) x - barWidth / 2;
            int barY = (int) y - a / 2 - barHeight - 2;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(barX, barY, barWidth, barHeight);

            double ratio = Math.max(0, Math.min(1.0, unit.getHP() / (double) maxHP));
            g2.setColor(ratio > 0.5 ? new Color(46, 204, 113) : ratio > 0.25 ? new Color(241, 196, 15) : new Color(231, 76, 60));
            g2.fillRect(barX, barY, (int) (barWidth * ratio), barHeight);

            g2.setColor(Color.BLACK);
            g2.drawRect(barX, barY, barWidth, barHeight);
        }
    }
}