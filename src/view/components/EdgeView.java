package view.components;

import model.EdgeFeature;
import model.HexUtils;

import java.awt.*;

public class EdgeView {
    public static void show(int col1, int row1, int col2, int row2, EdgeFeature feature, boolean hasRiver,
                             int wallHP, int a, Graphics2D g2) {
        if (feature == EdgeFeature.NONE && !hasRiver) return;

        double x1 = HexUtils.centerX(col1) * a;
        double y1 = HexUtils.centerY(col1, row1) * a;
        double x2 = HexUtils.centerX(col2) * a;
        double y2 = HexUtils.centerY(col2, row2) * a;

        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;

        double px = -dy / len;
        double py = dx / len;
        double halfSpan = a * 0.42;

        if (hasRiver) {
            double riverOffset = feature != EdgeFeature.NONE ? a * 0.1 : 0;
            int rx1 = (int) (midX - px * halfSpan + px * riverOffset);
            int ry1 = (int) (midY - py * halfSpan + py * riverOffset);
            int rx2 = (int) (midX + px * halfSpan + px * riverOffset);
            int ry2 = (int) (midY + py * halfSpan + py * riverOffset);
            g2.setColor(new Color(50, 120, 220));
            g2.setStroke(new BasicStroke(Math.max(3f, a * 0.1f)));
            g2.drawLine(rx1, ry1, rx2, ry2);
        }

        int lx1 = (int) (midX - px * halfSpan);
        int ly1 = (int) (midY - py * halfSpan);
        int lx2 = (int) (midX + px * halfSpan);
        int ly2 = (int) (midY + py * halfSpan);

        switch (feature) {
            case ROAD -> {
                g2.setColor(new Color(160, 120, 70));
                g2.setStroke(new BasicStroke(Math.max(3f, a * 0.12f)));
                g2.drawLine(lx1, ly1, lx2, ly2);
            }
            case WALL -> {
                g2.setColor(new Color(70, 70, 80));
                g2.setStroke(new BasicStroke(Math.max(5f, a * 0.2f)));
                g2.drawLine(lx1, ly1, lx2, ly2);

                if (wallHP > 0 && wallHP < 100) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, (int) (a * 0.22))));
                    g2.drawString(String.valueOf(wallHP), (int) midX - 8, (int) midY - 4);
                }
            }
            default -> {
            }
        }
    }
}
