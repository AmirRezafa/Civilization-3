package view.components;

import model.Building;
import model.BuildingType;
import java.awt.*;

public class BuildingView {

    public static void show(Building building, double x, double y, int a, Graphics2D g2) {
        if (building == null) return;

        int cx = (int) x;
        int cy = (int) y;

        int size = (int) (a * 0.55);
        int half = size / 2;

        BuildingType type = building.getType();

        Color bgColor;
        String label;

        switch (type) {
            case FARM:
                bgColor = new Color(241, 196, 15);
                label = "F";
                break;
            case STONE_MINE, IRON_MINE:
                bgColor = new Color(169, 169, 169);
                label = "M";
                break;
            case LUMBER_MILL:
                bgColor = new Color(139, 69, 19);
                label = "L";
                break;
            case STABLE:
                bgColor = new Color(210, 105, 30);
                label = "S";
                break;
            case TOWN_HALL:
                bgColor = new Color(41, 128, 185);
                label = "TH";
                break;
            case SETTLEMENT:
                bgColor = new Color(46, 204, 113);
                label = "C";
                break;
            case DOCK:
                bgColor = new Color(52, 73, 94);
                label = "D";
                break;
            case BAZAAR:
                bgColor = new Color(211, 84, 0);
                label = "BZ";
                break;
            case TRADING_POST:
                bgColor = new Color(127, 140, 141);
                label = "TP";
                break;
            case TRIBE_CAMP:
                bgColor = new Color(155, 40, 40);
                label = "TC";
                break;
            case OUTPOST:
                bgColor = new Color(39, 174, 96);
                label = "O";
                break;
            case MONUMENT:
                bgColor = new Color(155, 89, 182);
                label = "MN";
                break;
            case APOTHECARY:
                bgColor = new Color(46, 134, 130);
                label = "AP";
                break;
            default:
                bgColor = Color.WHITE;
                label = "?";
                break;
        }

        g2.setColor(bgColor);
        g2.fillRoundRect(cx - half, cy - half, size, size, 8, 8);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cx - half, cy - half, size, size, 8, 8);

        g2.setFont(new Font("SansSerif", Font.BOLD, size / 2));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(label);

        int textAscent = fm.getAscent();
        int textDescent = fm.getDescent();

        g2.drawString(label, cx - (textWidth / 2), cy + (textAscent - textDescent) / 2);

        int maxHP = building.getMaxHP();
        if (maxHP > 0 && building.getHP() < maxHP) {
            int barWidth = size;
            int barHeight = Math.max(3, a / 12);
            int barX = cx - half;
            int barY = cy - half - barHeight - 2;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(barX, barY, barWidth, barHeight);

            double ratio = Math.max(0, Math.min(1.0, building.getHP() / (double) maxHP));
            g2.setColor(ratio > 0.5 ? new Color(46, 204, 113) : ratio > 0.25 ? new Color(241, 196, 15) : new Color(231, 76, 60));
            g2.fillRect(barX, barY, (int) (barWidth * ratio), barHeight);

            g2.setColor(Color.BLACK);
            g2.drawRect(barX, barY, barWidth, barHeight);
        }
    }
}