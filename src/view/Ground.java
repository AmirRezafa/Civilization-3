package view;

import controller.GameController;
import controller.events.DisasterEvent;
import controller.events.EventBus;
import model.EdgeFeature;
import model.HexEdge;
import model.HexUtils;
import model.Season;
import model.TerrainType;
import model.Tile;
import model.Unit;
import model.UnitType;
import view.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Ground extends JPanel{
    private static final int WEATHER_PARTICLE_COUNT = 140;
    private static final long DISASTER_EFFECT_DURATION_MS = 3000;

    private int width, height;
    private GameController GC;

    private final Random weatherRandom = new Random();
    private final List<double[]> weatherParticles = new ArrayList<>();
    private Season lastWeatherSeason = null;

    private DisasterEvent lastDisaster;
    private long lastDisasterAtMillis = -1;

    Ground(){
        setBackground(Color.GRAY);
        EventBus.subscribe(DisasterEvent.class, e -> {
            lastDisaster = e;
            lastDisasterAtMillis = System.currentTimeMillis();
        });
    }

    public void setController(GameController controller) {
        GC = controller;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        width = getWidth();
        height = getHeight();

        g2.translate(-GC.getXOffset(), -GC.getYOffset());

        int a = GC.getA();

        int xOffset = GC.getXOffset();
        int yOffset = GC.getYOffset();

        for(Tile tile : GC.getTiles()) {

            double x = HexUtils.centerX(tile.getCol()) * a;
            double y = HexUtils.centerY(tile.getCol(), tile.getRow()) * a;

            if (x < xOffset - a * 2 || x > xOffset + width + a * 2 ||
                    y < yOffset - a * 2 || y > yOffset + height + a * 2){
                continue;
            }

            TileView.show(x, y, a, g2, getTerrainColor(tile.getTerrain()), tile);
            ResourceView.show(tile, a, x, y, g2);
            StationedWorkerView.show(tile, a, x, y, g2);
            if(tile.getBuilding() != null && tile.isVisible()){
                BuildingView.show(tile.getBuilding(), x, y, a, g2);
                if (GC.hasAdjacencyBonus(tile)) {
                    drawAdjacencyBadge(g2, x, y, a);
                }
            }
        }

        java.util.Set<HexEdge> drawnEdges = new java.util.HashSet<>();
        for (Map.Entry<HexEdge, EdgeFeature> entry : GC.getEdgeFeatures().entrySet()) {
            HexEdge edge = entry.getKey();
            Tile t1 = GC.getTileAt(edge.getCol1(), edge.getRow1());
            Tile t2 = GC.getTileAt(edge.getCol2(), edge.getRow2());
            if (!t1.isVisible() || !t2.isVisible()) continue;
            drawnEdges.add(edge);

            int wallHP = entry.getValue() == EdgeFeature.WALL
                    ? GC.getWallHP(edge.getCol1(), edge.getRow1(), edge.getCol2(), edge.getRow2())
                    : 0;
            boolean hasRiver = GC.hasRiverEdge(edge.getCol1(), edge.getRow1(), edge.getCol2(), edge.getRow2());
            EdgeView.show(edge.getCol1(), edge.getRow1(), edge.getCol2(), edge.getRow2(),
                    entry.getValue(), hasRiver, wallHP, a, g2);
        }
        for (HexEdge edge : GC.getRiverEdges()) {
            if (drawnEdges.contains(edge)) continue;
            Tile t1 = GC.getTileAt(edge.getCol1(), edge.getRow1());
            Tile t2 = GC.getTileAt(edge.getCol2(), edge.getRow2());
            if (!t1.isVisible() || !t2.isVisible()) continue;

            EdgeView.show(edge.getCol1(), edge.getRow1(), edge.getCol2(), edge.getRow2(),
                    EdgeFeature.NONE, true, 0, a, g2);
        }

        drawActiveDisasterEffect(g2, a);

        g2.setColor(Color.RED);
        for (Unit unit : GC.getUnits()) {
            if(unit.isAssigned()) continue;
            boolean afloat = GC.getTileAt(unit.getCol(), unit.getRow()).getTerrain() == TerrainType.SEA;
            UnitView.show(unit, a, unit == GC.getSelectedUnit(), afloat, g2);
        }

        g2.translate(GC.getXOffset(), GC.getYOffset());

        drawWeatherOverlay(g2);

        if (GC.getSelectedUnit() != null) {
            drawSelectedUnitInfo(g2);
        }

        g2.dispose();
    }

    /**
     * Clicking a hex only ever selects the first unit found there, so multiple stacked units
     * (e.g. a full 2 Swordsman / 2 Archer / 1 Cavalry attack stack) were otherwise invisible to
     * the player - this panel lists everyone actually standing on the selected hex, grouped by
     * type, so an attack's real composition is known before committing to it.
     */
    private void drawSelectedUnitInfo(Graphics2D g2) {
        Unit selectedUnit = GC.getSelectedUnit();
        List<Unit> stack = GC.getUnitsAt(selectedUnit.getCol(), selectedUnit.getRow());

        Map<UnitType, Integer> counts = new LinkedHashMap<>();
        for (Unit u : stack) counts.merge(u.getType(), 1, Integer::sum);

        int lineHeight = 18;
        int lines = 3 + (stack.size() > 1 ? 1 + counts.size() : 0);
        int boxWidth = 280;
        int boxHeight = 30 + lines * lineHeight;
        int boxX = 20;
        // UnitActionPanel sits in its own layer directly over this bottom strip (see
        // MainFrame.updateLayeredLayoutBounds, which reserves GC.getB()*3.4 px for it) - anchor
        // above that reserved area, with a small gap, instead of a fixed offset from the bottom
        // that a taller stack list would grow straight through.
        int reservedActionBarHeight = (int) (GC.getB() * 3.4);
        int boxY = height - reservedActionBarHeight - 20 - boxHeight;

        g2.setColor(new Color(20, 20, 20, 220));
        g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15);
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15);

        int textX = boxX + 20;
        int textY = boxY + 25;

        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(" UNIT SELECTED", textX, textY);
        textY += lineHeight;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("Coordinates: [ X: " + selectedUnit.getCol() + " , Y: " + selectedUnit.getRow() + " ]", textX, textY);
        textY += lineHeight;

        g2.drawString("Terrain Type: " + GC.getTileUnderUnit().getTerrain().toString(), textX, textY);
        textY += lineHeight;

        if (stack.size() > 1) {
            g2.setColor(new Color(220, 220, 220));
            g2.drawString("Stack here (" + stack.size() + " units):", textX, textY);
            textY += lineHeight;
            for (Map.Entry<UnitType, Integer> entry : counts.entrySet()) {
                g2.drawString("  " + entry.getKey().getDisplayName() + " x" + entry.getValue(), textX, textY);
                textY += lineHeight;
            }
        }
    }

    private void drawAdjacencyBadge(Graphics2D g2, double x, double y, int a) {
        int r = Math.max(4, a / 8);
        int bx = (int) x + a / 3;
        int by = (int) y - a / 3;
        g2.setColor(new Color(241, 196, 15));
        g2.fillOval(bx - r, by - r, r * 2, r * 2);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(bx - r, by - r, r * 2, r * 2);
    }

    private void drawActiveDisasterEffect(Graphics2D g2, int a) {
        if (lastDisaster == null) return;
        long elapsed = System.currentTimeMillis() - lastDisasterAtMillis;
        if (elapsed > DISASTER_EFFECT_DURATION_MS) return;

        Tile centerTile = GC.getTileAt(lastDisaster.getCenterCol(), lastDisaster.getCenterRow());
        if (centerTile == null || !centerTile.isVisible()) return;

        float fade = 1f - (float) elapsed / DISASTER_EFFECT_DURATION_MS;
        double cx = HexUtils.centerX(lastDisaster.getCenterCol()) * a;
        double cy = HexUtils.centerY(lastDisaster.getCenterCol(), lastDisaster.getCenterRow()) * a;
        double pulse = 1.0 + 0.15 * Math.sin(elapsed / 120.0);
        int radiusPx = (int) (a * (lastDisaster.getRadius() + 0.8) * pulse);

        Color effectColor = lastDisaster.getType() == model.DisasterType.FLOOD
                ? new Color(52, 152, 219)
                : new Color(211, 84, 0);

        g2.setColor(new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), (int) (90 * fade)));
        g2.fillOval((int) cx - radiusPx, (int) cy - radiusPx, radiusPx * 2, radiusPx * 2);
        g2.setColor(new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), (int) (200 * fade)));
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval((int) cx - radiusPx, (int) cy - radiusPx, radiusPx * 2, radiusPx * 2);
    }

    private void drawWeatherOverlay(Graphics2D g2) {
        Season season = GC.getCurrentSeason();
        if (season != Season.WINTER && season != Season.AUTUMN) {
            weatherParticles.clear();
            lastWeatherSeason = season;
            return;
        }

        if (season != lastWeatherSeason || weatherParticles.isEmpty()) {
            weatherParticles.clear();
            for (int i = 0; i < WEATHER_PARTICLE_COUNT; i++) {
                weatherParticles.add(newParticle(true));
            }
            lastWeatherSeason = season;
        }

        boolean snow = season == Season.WINTER;
        g2.setColor(snow ? new Color(255, 255, 255, 210) : new Color(180, 210, 255, 160));
        g2.setStroke(new BasicStroke(snow ? 1f : 1.5f));

        for (int i = 0; i < weatherParticles.size(); i++) {
            double[] p = weatherParticles.get(i);
            double x = p[0], y = p[1], speedY = p[2], speedX = p[3];

            if (snow) {
                g2.fillOval((int) x, (int) y, 3, 3);
            } else {
                g2.drawLine((int) x, (int) y, (int) (x - speedX * 3), (int) (y - speedY * 3));
            }

            x += speedX;
            y += speedY;
            if (y > height || x < -10 || x > width + 10) {
                double[] fresh = newParticle(false);
                p[0] = fresh[0];
                p[1] = fresh[1];
                p[2] = fresh[2];
                p[3] = fresh[3];
            } else {
                p[0] = x;
                p[1] = y;
            }
        }
    }

    private double[] newParticle(boolean randomStartY) {
        double x = weatherRandom.nextDouble() * (width + 40) - 20;
        double y = randomStartY ? weatherRandom.nextDouble() * height : -10;
        boolean snow = GC.getCurrentSeason() == Season.WINTER;
        double speedY = snow ? 1.0 + weatherRandom.nextDouble() * 1.5 : 6.0 + weatherRandom.nextDouble() * 4.0;
        double speedX = snow ? -0.3 + weatherRandom.nextDouble() * 0.6 : 3.0 + weatherRandom.nextDouble() * 2.0;
        return new double[]{x, y, speedY, speedX};
    }

    private Color getTerrainColor(TerrainType type) {
        return switch (type) {
            case PLAIN -> new Color(180, 200, 100);
            case FOREST -> new Color(34, 139, 34);
            case MOUNTAIN -> new Color(128, 128, 128);
            case MEADOW -> new Color(144, 238, 144);
            case SEA -> new Color(65, 105, 225);
            case MOUNTAIN_RANGE -> new Color(90, 90, 90);
        };
    }

    public int getwidth() {
        return width;
    }

    public int getheight() {
        return height;
    }
}
