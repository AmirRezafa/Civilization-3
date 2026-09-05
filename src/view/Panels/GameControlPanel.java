package view.Panels;

import controller.GameController;
import controller.events.CombatResultEvent;
import controller.events.DisasterEvent;
import controller.events.EventBus;
import controller.events.HUDChangedEvent;
import controller.events.StarvationEvent;
import controller.events.TribeEvent;
import controller.events.AutosaveFailedEvent;
import model.ResourceType;
import model.UnitType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class GameControlPanel extends JPanel {
    private final GameController GC;

    private JLabel turnLabel;
    private JLabel seasonLabel;
    private JLabel unitLabel;
    private JLabel foodLabel;
    private JLabel woodLabel;
    private JLabel stoneLabel;
    private JLabel ironLabel;
    private JLabel fishLabel;
    private JLabel happinessLabel;
    private JButton nextTurnButton;

    public GameControlPanel(GameController gc) {
        this.GC = gc;

        this.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 12));
        this.setBackground(new Color(45, 45, 45));
        // FlowLayout.preferredLayoutSize() always assumes a single row, even when the actual
        // available width forces it to wrap onto a second line. Since this panel sits in
        // BorderLayout.NORTH (sized via getPreferredSize()), a wrapped second row would be
        // pushed outside the reserved area and rendered behind the map. Reserve enough
        // height up front for two rows so nothing (e.g. the Pause button) goes missing.
        this.setPreferredSize(new Dimension(0, 100));

        initializeComponents();
        updateHUD();

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "openPauseMenu");
        getActionMap().put("openPauseMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPauseMenu();
            }
        });

        EventBus.subscribe(HUDChangedEvent.class, e -> updateHUD());
        EventBus.subscribe(StarvationEvent.class, e -> showStarvationAlert());
        EventBus.subscribe(DisasterEvent.class, this::showDisasterAlert);
        EventBus.subscribe(CombatResultEvent.class, this::showCombatResult);
        EventBus.subscribe(TribeEvent.class, this::showTribeNotification);
        EventBus.subscribe(AutosaveFailedEvent.class, e -> JOptionPane.showMessageDialog(this,
                "Autosave failed this turn. Your last successful autosave is still safe - " +
                        "consider saving manually from the Pause menu.",
                "Autosave Failed", JOptionPane.ERROR_MESSAGE));
    }

    private JLabel tribeNotificationLabel;
    private Timer tribeNotificationTimer;

    private void showTribeNotification(TribeEvent event) {
        if (tribeNotificationLabel == null) return;

        tribeNotificationLabel.setText(event.getTribeName() + ": " + event.getMessage());
        tribeNotificationLabel.setVisible(true);

        if (tribeNotificationTimer != null) tribeNotificationTimer.stop();
        tribeNotificationTimer = new Timer(6000, e -> tribeNotificationLabel.setVisible(false));
        tribeNotificationTimer.setRepeats(false);
        tribeNotificationTimer.start();
    }

    private void showDisasterAlert(DisasterEvent event) {
        JOptionPane.showMessageDialog(this, event.getMessage(), "Natural Disaster: " + event.getType(),
                JOptionPane.WARNING_MESSAGE);
    }

    private static class DieFace extends JComponent {
        private int value = 1;
        private Color highlight = null;

        DieFace() {
            setPreferredSize(new Dimension(34, 34));
            setMinimumSize(new Dimension(34, 34));
            setMaximumSize(new Dimension(34, 34));
            setOpaque(false);
        }

        void setValue(int v) {
            value = v;
            repaint();
        }

        void setHighlight(Color c) {
            highlight = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
            g2.setColor(highlight != null ? highlight : new Color(90, 90, 90));
            g2.setStroke(new BasicStroke(highlight != null ? 3f : 1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, 8, 8);

            g2.setColor(new Color(30, 30, 30));
            int r = Math.max(2, w / 10);
            for (int[] pos : pipPositions(value)) {
                int cx = w * pos[0] / 4;
                int cy = h * pos[1] / 4;
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            }
            g2.dispose();
        }

        private int[][] pipPositions(int v) {
            return switch (v) {
                case 1 -> new int[][]{{2, 2}};
                case 2 -> new int[][]{{1, 1}, {3, 3}};
                case 3 -> new int[][]{{1, 1}, {2, 2}, {3, 3}};
                case 4 -> new int[][]{{1, 1}, {3, 1}, {1, 3}, {3, 3}};
                case 5 -> new int[][]{{1, 1}, {3, 1}, {2, 2}, {1, 3}, {3, 3}};
                default -> new int[][]{{1, 1}, {3, 1}, {1, 2}, {3, 2}, {1, 3}, {3, 3}};
            };
        }
    }

    private JPanel buildDiceRow(String rowLabel, java.util.List<Integer> rolls, java.util.List<Integer> rawRolls,
                                 int pairedCount, boolean isAttackerRow,
                                 boolean[] attackerWinsPair, java.util.List<DieFace> collector) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JLabel label = new JLabel(rowLabel + ":");
        label.setPreferredSize(new Dimension(140, 20));
        row.add(label);
        for (int i = 0; i < rolls.size(); i++) {
            int displayValue = rawRolls != null ? rawRolls.get(i) : rolls.get(i);
            DieFace die = new DieFace();
            die.setValue(displayValue);
            if (i < pairedCount) {
                boolean thisRowWinsThisPair = isAttackerRow == attackerWinsPair[i];
                die.setHighlight(thisRowWinsThisPair ? new Color(46, 204, 113) : new Color(231, 76, 60));
            }
            collector.add(die);

            boolean boosted = rawRolls != null && !rawRolls.get(i).equals(rolls.get(i));
            if (boosted) {
                JPanel cell = new JPanel();
                cell.setOpaque(false);
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                die.setAlignmentX(Component.CENTER_ALIGNMENT);
                cell.add(die);
                JLabel caption = new JLabel(rawRolls.get(i) + " +2 wall → " + rolls.get(i));
                caption.setAlignmentX(Component.CENTER_ALIGNMENT);
                caption.setFont(caption.getFont().deriveFont(Font.BOLD, 10f));
                caption.setForeground(new Color(52, 152, 219));
                cell.add(caption);
                row.add(cell);
            } else {
                row.add(die);
            }
        }
        return row;
    }

    private void showCombatResult(CombatResultEvent event) {
        java.util.List<Integer> atk = event.getAttackerRolls();
        java.util.List<Integer> def = event.getDefenderRolls();
        java.util.List<Integer> defRaw = event.getDefenderRawRolls();
        int pairs = Math.min(atk.size(), def.size());
        boolean[] attackerWinsPair = new boolean[pairs];
        for (int i = 0; i < pairs; i++) attackerWinsPair[i] = atk.get(i) > def.get(i);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Combat Result", true);
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        java.util.List<DieFace> allDice = new java.util.ArrayList<>();
        JPanel diceArea = new JPanel();
        diceArea.setLayout(new BoxLayout(diceArea, BoxLayout.Y_AXIS));
        String defenderLabel = event.isDefenderHadWall() ? "Defender (wall +2)" : "Defender";
        diceArea.add(buildDiceRow("Attacker", atk, null, pairs, true, attackerWinsPair, allDice));
        diceArea.add(buildDiceRow(defenderLabel, def, defRaw, pairs, false, attackerWinsPair, allDice));
        content.add(diceArea, BorderLayout.CENTER);

        String wallNote = event.isDefenderHadWall() ? " (wall added +2 to each defender die, capped at 6)" : "";
        JLabel summary = new JLabel("<html>Hits on defender: <b>" + event.getDefenderHits() + "</b> &nbsp;|&nbsp; " +
                "Hits on attacker: <b>" + event.getAttackerHits() + "</b>" + wallNote + "</html>");

        JButton ok = new JButton("Continue");
        ok.setFocusable(false);
        ok.addActionListener(e -> dialog.dispose());

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(summary, BorderLayout.NORTH);
        south.add(ok, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        Random rollAnim = new Random();
        int[] ticks = {0};
        Timer rollTimer = new Timer(55, null);
        rollTimer.addActionListener(e -> {
            ticks[0]++;
            boolean settled = ticks[0] >= 7;
            for (int i = 0; i < allDice.size(); i++) {
                DieFace die = allDice.get(i);
                if (!settled) {
                    die.setValue(1 + rollAnim.nextInt(6));
                }
            }
            if (settled) {
                rollTimer.stop();
                for (int i = 0; i < allDice.size(); i++) {
                    int displayValue = i < atk.size() ? atk.get(i) : defRaw.get(i - atk.size());
                    allDice.get(i).setValue(displayValue);
                }
            }
        });
        rollTimer.start();

        dialog.setVisible(true);
    }

    private JLabel createLabel(Font hudFont, Color textColor){
        JLabel label = new JLabel();
        label.setFont(hudFont);
        label.setForeground(textColor);
        this.add(label);
        return label;
    }

    private void initializeComponents() {
        Font hudFont = new Font("SansSerif", Font.BOLD, 14);
        Color textColor = Color.WHITE;

        turnLabel = new JLabel("Turn: 1");
        turnLabel.setFont(hudFont);
        turnLabel.setForeground(new Color(241, 196, 15));
        this.add(turnLabel);

        seasonLabel = new JLabel("Season: Spring");
        seasonLabel.setFont(hudFont);
        seasonLabel.setForeground(new Color(102, 204, 255));
        this.add(seasonLabel);

        unitLabel = createLabel(hudFont, textColor);
        foodLabel = createLabel(hudFont, textColor);
        woodLabel = createLabel(hudFont, textColor);
        stoneLabel = createLabel(hudFont, textColor);
        ironLabel = createLabel(hudFont, textColor);
        fishLabel = createLabel(hudFont, textColor);
        happinessLabel = createLabel(hudFont, textColor);

        nextTurnButton = new JButton("Next Turn");
        nextTurnButton.setFont(hudFont);
        nextTurnButton.setFocusable(false);
        nextTurnButton.setBackground(new Color(39, 174, 96));
        nextTurnButton.setForeground(Color.WHITE);

        nextTurnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleNextTurnAction();
            }
        });

        this.add(nextTurnButton);

        JButton pauseButton = new JButton("Pause");
        pauseButton.setFont(hudFont);
        pauseButton.setFocusable(false);
        pauseButton.addActionListener(e -> showPauseMenu());
        this.add(pauseButton);

        tribeNotificationLabel = new JLabel();
        tribeNotificationLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        tribeNotificationLabel.setForeground(new Color(230, 126, 34));
        tribeNotificationLabel.setVisible(false);
        this.add(tribeNotificationLabel);
    }

    private void showPauseMenu() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(topFrame, "Paused", true);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        dialog.setContentPane(content);

        content.add(buildSlotRow(dialog, 0, "Autosave", false));
        content.add(Box.createVerticalStrut(8));
        for (int slot = 1; slot <= 3; slot++) {
            content.add(buildSlotRow(dialog, slot, "Slot " + slot, true));
            content.add(Box.createVerticalStrut(8));
        }

        JButton resumeButton = new JButton("Resume");
        resumeButton.setFocusable(false);
        resumeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resumeButton.addActionListener(e -> dialog.dispose());
        content.add(resumeButton);

        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(420, 260));
        dialog.pack();
        dialog.setLocationRelativeTo(topFrame);
        dialog.setVisible(true);
    }

    private JPanel buildSlotRow(JDialog dialog, int slot, String label, boolean allowSave) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel summaryLabel = new JLabel(label + ": " + GC.peekSaveSummary(slot));
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(summaryLabel);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (allowSave) {
            JButton saveBtn = new JButton("Save");
            saveBtn.setFocusable(false);
            saveBtn.addActionListener(e -> handleSaveAction(slot, summaryLabel));
            buttonRow.add(saveBtn);
        }

        boolean corrupted = GC.isSaveSlotCorrupted(slot);
        JButton loadBtn = new JButton("Load");
        loadBtn.setFocusable(false);
        loadBtn.setEnabled(!corrupted);
        loadBtn.addActionListener(e -> {
            handleLoadAction(slot);
            dialog.dispose();
        });
        buttonRow.add(loadBtn);

        if (corrupted) {
            JLabel brokenLabel = new JLabel("(broken save)");
            brokenLabel.setForeground(new Color(231, 76, 60));
            buttonRow.add(brokenLabel);

            JButton deleteBtn = new JButton("Delete");
            deleteBtn.setFocusable(false);
            deleteBtn.addActionListener(e -> {
                GC.deleteSaveSlot(slot);
                dialog.dispose();
                showPauseMenu();
            });
            buttonRow.add(deleteBtn);
        }

        row.add(buttonRow);
        return row;
    }

    private void handleSaveAction(int slot, JLabel summaryLabel) {
        if (!GC.isSaveAllowed()) {
            JOptionPane.showMessageDialog(this,
                    "Finish or cancel your pending action (attack/build/deconstruct) before saving.",
                    "Save Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean slotOccupied = !"Empty".equals(GC.peekSaveSummary(slot));
        if (slotOccupied) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Slot " + slot + " already has a save (" + GC.peekSaveSummary(slot) + "). Overwrite it?",
                    "Overwrite Save", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        boolean success = GC.saveGame(slot);
        if (success) summaryLabel.setText("Slot " + slot + ": " + GC.peekSaveSummary(slot));
        JOptionPane.showMessageDialog(this, success ? "Game saved to slot " + slot : "Save failed",
                "Save Game", success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void handleLoadAction(int slot) {
        if ("Empty".equals(GC.peekSaveSummary(slot))) {
            JOptionPane.showMessageDialog(this, "Slot " + slot + " is empty.",
                    "Load Game", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Loading will discard your current unsaved progress. Continue?",
                "Load Game", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean success = GC.loadGame(slot);
        JOptionPane.showMessageDialog(this, success ? "Game loaded from slot " + slot : "Load failed (corrupted slot)",
                "Load Game", success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);

        if (success) refreshAfterExternalLoad();
    }

    public void refreshAfterExternalLoad() {
        turnLabel.setText("Turn: " + GC.getCurrentTurn());
        updateHUD();

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) topFrame.repaint();
    }

    private void handleNextTurnAction() {
        if (GC.hasUnitsWithRemainingAP()) {

            String warningMessage = "<html><div style='font-family: \"Segoe UI\", sans-serif;'>"
                    + "<b>Hold on, bro!</b> some of your units still have Action Points (AP) left.<br>"
                    + "Are you sure you want to end this turn and waste their moves?"
                    + "</div></html>";

            int response = JOptionPane.showConfirmDialog(
                    this,
                    warningMessage,
                    "Unused Action Points",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }
        GC.advanceTurn();

        turnLabel.setText("Turn: " + GC.getCurrentTurn());

        updateHUD();

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) {
            topFrame.repaint();
        }
    }

    public void updateHUD() {
        var economy = GC.getEconomy();
        GC.updateNetChanges();
        seasonLabel.setText("Season: " + GC.getCurrentSeason());
        unitLabel.setText("Unit: " + GC.getUnitCounts() + "/" + GC.getUnitCapacity());
        foodLabel.setText("Food: " + (economy.getNetChanges(ResourceType.CATTLE) +
                economy.getNetChanges(ResourceType.WHEAT)) + " | " +
                (economy.getResourceAmount(ResourceType.CATTLE) +
                economy.getResourceAmount(ResourceType.WHEAT)) + "/" +
                (economy.getResourceCapacityAmount(ResourceType.CATTLE) +
                        economy.getResourceCapacityAmount(ResourceType.WHEAT)));
        woodLabel.setText("Wood: " + economy.getNetChanges(ResourceType.WOOD) + " | " +
                economy.getResourceAmount(ResourceType.WOOD) + "/" +
                economy.getResourceCapacityAmount(ResourceType.WOOD));
        stoneLabel.setText("Stone: " + economy.getNetChanges(ResourceType.STONE) + " | " +
                economy.getResourceAmount(ResourceType.STONE) + "/" +
                economy.getResourceCapacityAmount(ResourceType.STONE));
        ironLabel.setText("Iron: " + economy.getNetChanges(ResourceType.IRON) + " | " +
                economy.getResourceAmount(ResourceType.IRON) + "/" +
                economy.getResourceCapacityAmount(ResourceType.IRON));
        fishLabel.setText("Fish: " + economy.getNetChanges(ResourceType.FISH) + " | " +
                economy.getResourceAmount(ResourceType.FISH) + "/" +
                economy.getResourceCapacityAmount(ResourceType.FISH));
        happinessLabel.setText("Happiness: " + GC.getHappinessManager().getHappiness() +
                " (" + GC.getHappinessManager().getLevelLabel() + ")");
        happinessLabel.setToolTipText("Golden Age (>=3): +10% production. Normal (-2..2): no effect. " +
                "Dissatisfaction (-3..-4): -1 production per worker. Riot (<=-5): also -1 AP for all units.");

        int explorerCounts = GC.getUnitCounts(UnitType.EXPLORER);
        int builderCounts = GC.getUnitCounts(UnitType.BUILDER);
        int workerCounts = GC.getUnitCounts(UnitType.WORKER);
        int expanderCounts = GC.getUnitCounts(UnitType.BORDER_EXPANDER);

// ساخت یک متن HTML شیک برای تول‌تیپ
        String tooltipText = "<html>" +
                "Explorers: " + explorerCounts + "<br>" +
                "Builders: " + builderCounts + "<br>" +
                "Workers: " + workerCounts + "<br>" +
                "Expanders: " + expanderCounts +
                "</html>";

        unitLabel.setToolTipText(tooltipText);
    }

// آره خلاصه اینجا احساس صمیمیت کردم
    public void showStarvationAlert() {
        String alertHtml = "<html><div style='width: 280px; text-align: left; font-family: \"Segoe UI\", sans-serif; padding: 5px;'>"
                + "<h2 style='color: #e74c3c; margin: 0 0 10px 0; font-size: 16px;'>CRITICAL CRISIS!</h2>"
                + "<p style='color: #333333; font-size: 13px; line-height: 1.6;'>"
                + "<b>Bro, we are in a total crisis!</b><br>"
                + "Food reserves hit zero and starvation is kicking in. "
                + "Fix the situation ASAP before your units start dropping dead!</p>"
                + "</div></html>";

        JLabel label = new JLabel(alertHtml);

        JOptionPane.showMessageDialog(
                this,
                label,
                "Emergency",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
