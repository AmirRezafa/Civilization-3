package view.Panels;

import view.MainFrame;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {
    private static final Color backGroundColor = new Color(25, 25, 25);
    private static final Dimension ButtonSize = new Dimension(220, 50);

    private final JLabel label = new JLabel("Civilization");

    private static MainFrame MF;

    private enum Button {
        Start("Start Game") {
            @Override
            public void clicked() {
                MF.showGame();
            }
        },
        Load("Load Game") {
            @Override
            public void clicked() {
                showLoadDialog();
            }
        },
        Multiplayer("Multiplayer") {
            @Override
            public void clicked() {
                MF.showLobby();
            }
        },
        Setting("Settings") {
            @Override
            public void clicked() {
                MF.showSettings();
            }
        },
        Exit("Exit") {
            @Override
            public void clicked() {
                System.out.println("Game Closed Safely.");
                System.exit(0);
            }
        };

        private final String name;
        Button(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract void clicked();
    }

    public MainMenuPanel(MainFrame MF) {
        MainMenuPanel.MF = MF;
        setLayout(new GridBagLayout());
        setBackground(backGroundColor);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBackground(backGroundColor);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 45));
        buttonsPanel.add(label);
        buttonsPanel.add(Box.createVerticalStrut(25));

        for (Button button : Button.values()) {
            JButton temp = new JButton(button.getName());
            temp.setAlignmentX(Component.CENTER_ALIGNMENT);
            temp.setPreferredSize(ButtonSize);
            temp.setMinimumSize(ButtonSize);
            temp.setMaximumSize(ButtonSize);
            temp.setFocusPainted(false);

            temp.setFont(new Font("SansSerif", Font.BOLD, 14));
            temp.setBackground(new Color(52, 73, 94));
            temp.setForeground(Color.WHITE);

            temp.addActionListener(e -> button.clicked());

            buttonsPanel.add(temp);
            buttonsPanel.add(Box.createVerticalStrut(15));
        }
        add(buttonsPanel);
    }

    private static void showLoadDialog() {
        controller.GameController gc = MF.getGameController();
        JDialog dialog = new JDialog(MF, "Load Game", true);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] labels = {"Autosave", "Slot 1", "Slot 2", "Slot 3"};
        for (int slot = 0; slot < labels.length; slot++) {
            String summary = gc.peekSaveSummary(slot);
            boolean corrupted = gc.isSaveSlotCorrupted(slot);
            boolean empty = "Empty".equals(summary);

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            row.add(new JLabel(labels[slot] + ": " + summary), BorderLayout.CENTER);

            JButton loadBtn = new JButton("Load");
            loadBtn.setFocusable(false);
            loadBtn.setEnabled(!empty && !corrupted);
            int finalSlot = slot;
            loadBtn.addActionListener(e -> {
                boolean success = MF.loadGameAndShow(finalSlot);
                if (!success) {
                    JOptionPane.showMessageDialog(dialog, "Load failed (corrupted slot).",
                            "Load Game", JOptionPane.ERROR_MESSAGE);
                }
                dialog.dispose();
            });
            row.add(loadBtn, BorderLayout.EAST);

            content.add(row);
            content.add(Box.createVerticalStrut(6));
        }

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFocusable(false);
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> dialog.dispose());
        content.add(cancelBtn);

        dialog.setContentPane(content);
        dialog.setMinimumSize(new Dimension(360, 260));
        dialog.pack();
        dialog.setLocationRelativeTo(MF);
        dialog.setVisible(true);
    }
}