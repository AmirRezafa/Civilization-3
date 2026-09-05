package view.components;

import model.Building;
import model.Tile;
import java.awt.*;

public class StationedWorkerView {
    public static void show(Tile tile, int a, double x, double y, Graphics2D g2) {
        Building building = tile.getBuilding();

        if(building == null) return;
        if(building.getStationedWorkers().isEmpty()) return;


        int workerCount = building.getStationedWorkers().size();
        int r = a / 4;
        int gap = r + 4;

        int startX = (int) x - ((workerCount * gap) / 2) + (gap / 2);
        int startY = (int) y - (a / 2) - r;

        for (int i = 0; i < workerCount; i++) {
            int drawX = startX + (i * gap) - (r / 2);

            g2.setColor(new Color(230, 126, 34));
            g2.fillOval(drawX, startY, r, r);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(drawX, startY, r, r);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, r - 4));
            g2.drawString("W", drawX + 2, startY + r - 2);
        }
    }
}