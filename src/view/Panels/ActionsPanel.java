package view.Panels;

import model.Building;
import model.BuildingType;
import model.TechType;
import model.TownHallLevel;
import model.Unit;
import model.UnitType;
import network.protocol.ConstructBuildingRequest;
import network.protocol.ConstructTownHallRequest;
import network.protocol.ItemType;
import network.protocol.ProduceItemRequest;
import network.protocol.StartTechResearchRequest;
import network.protocol.StartUnitProductionRequest;
import network.protocol.StartUpgradeRequest;
import network.protocol.UseCombatBoostItemRequest;
import network.protocol.UseMobilityItemRequest;
import network.protocol.UseTeleportItemRequest;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ActionsPanel extends JPanel {
    private static final Color BACKGROUND = new Color(35, 35, 35);
    private static final Color BUTTON_COLOR = new Color(52, 73, 94);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);

    private final NetworkBoardPanel boardPanel;
    private final JPanel buttonContainer = new JPanel();

    private Map<String, Integer> myItems = new HashMap<>();
    private List<String> myTechs = new ArrayList<>();

    public ActionsPanel(NetworkBoardPanel boardPanel) {
        this.boardPanel = boardPanel;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        JLabel title = new JLabel("Actions", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(title, BorderLayout.NORTH);

        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setBackground(BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(buttonContainer);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        boardPanel.setSelectionListener(this::refresh);
        refresh();
    }

    public void setEconomy(Map<String, Integer> resources, List<String> techs, Map<String, Integer> items) {
        this.myItems = items != null ? items : Collections.emptyMap();
        this.myTechs = techs != null ? techs : Collections.emptyList();
        refresh();
    }

    private int itemAmount(ItemType type) {
        Integer value = myItems.get(type.name());
        return value == null ? 0 : value;
    }

    private boolean hasTech(TechType tech) {
        return myTechs.contains(tech.name());
    }

    public void refresh() {
        buttonContainer.removeAll();
        boolean myTurn = boardPanel.isMyTurn();

        Unit selectedUnit = boardPanel.getSelectedUnit();
        Building selectedBuilding = boardPanel.getSelectedBuilding();
        boolean myBuilding = selectedBuilding != null
                && boardPanel.getMyPlayerId() != null
                && boardPanel.getMyPlayerId().equals(boardPanel.getSelectedBuildingOwnerId());

        boolean showedAnything = false;
        if (selectedUnit != null) {
            showUnitActions(selectedUnit, myTurn);
            showedAnything = true;
        }
        if (myBuilding) {
            showBuildingActions(selectedBuilding, myTurn);
            showedAnything = true;
        }
        if (!showedAnything) {
            addInfoLabel("Select one of your units or buildings on the board to see actions here.");
        }

        buttonContainer.revalidate();
        buttonContainer.repaint();
    }

    private void showUnitActions(Unit unit, boolean myTurn) {
        int unitId = boardPanel.getSelectedUnitId();
        addSectionLabel(unit.getType().getDisplayName() + " (AP: " + unit.getCurrentAP() + ")");

        if (unit.getType() == UnitType.BUILDER) {
            for (BuildingType buildingType : BuildingType.values()) {
                if (!buildingType.isPlayerBuildable()) {
                    continue;
                }
                JButton buildButton = styledButton("Build " + buildingType.getDisplayName()
                        + " (" + buildingType.getCostString() + ", " + buildingType.getApCost() + " AP)");
                buildButton.setEnabled(myTurn && unit.getCurrentAP() >= buildingType.getApCost());
                buildButton.addActionListener(e -> boardPanel.sendQuiet(new ConstructBuildingRequest(unitId, buildingType.name())));
                buttonContainer.add(buildButton);
            }

            JButton foundHallButton = styledButton("Found New Town Hall (300 Wood, 300 Stone, 200 Iron)");
            foundHallButton.setEnabled(myTurn);
            foundHallButton.addActionListener(e -> boardPanel.sendQuiet(new ConstructTownHallRequest(unitId)));
            buttonContainer.add(foundHallButton);
        }

        addItemButtons(unitId, myTurn);
    }

    private void addItemButtons(int unitId, boolean myTurn) {
        addSectionLabel("Items");

        int teleportCount = itemAmount(ItemType.TELEPORT);
        JButton teleportButton = styledButton("Use Teleport (" + teleportCount + ")");
        teleportButton.setEnabled(myTurn && teleportCount > 0);
        teleportButton.addActionListener(e -> {
            String colText = JOptionPane.showInputDialog(this, "Target column:");
            if (colText == null) {
                return;
            }
            String rowText = JOptionPane.showInputDialog(this, "Target row:");
            if (rowText == null) {
                return;
            }
            try {
                int col = Integer.parseInt(colText.trim());
                int row = Integer.parseInt(rowText.trim());
                boardPanel.sendQuiet(new UseTeleportItemRequest(unitId, col, row));
            } catch (NumberFormatException ignored) {
            }
        });
        buttonContainer.add(teleportButton);

        int mobilityCount = itemAmount(ItemType.MOBILITY);
        JButton mobilityButton = styledButton("Use Mobility, +2 AP (" + mobilityCount + ")");
        mobilityButton.setEnabled(myTurn && mobilityCount > 0);
        mobilityButton.addActionListener(e -> boardPanel.sendQuiet(new UseMobilityItemRequest(unitId)));
        buttonContainer.add(mobilityButton);

        int boostCount = itemAmount(ItemType.COMBAT_BOOST);
        JButton boostButton = styledButton("Use Combat Boost, +5 dmg (" + boostCount + ")");
        boostButton.setEnabled(myTurn && boostCount > 0);
        boostButton.addActionListener(e -> boardPanel.sendQuiet(new UseCombatBoostItemRequest(unitId)));
        buttonContainer.add(boostButton);
    }

    private void showBuildingActions(Building building, boolean myTurn) {
        int buildingId = boardPanel.getSelectedBuildingId();
        addSectionLabel(building.getType().getDisplayName());

        if (building.getType() == BuildingType.TOWN_HALL) {
            if (building.isProducing()) {
                String message;
                if (building.getProducingUnit() != null) {
                    message = "Producing: " + building.getProducingUnit().getDisplayName()
                            + " (" + building.getProductionTurnsLeft() + " turns left)";
                } else if (building.getUpgradingToLevel() != null) {
                    message = "Upgrading to: " + building.getUpgradingToLevel().getDisplayName()
                            + " (" + building.getProductionTurnsLeft() + " turns left)";
                } else {
                    message = "Researching: " + building.getResearchingTech().getDisplayName()
                            + " (" + building.getProductionTurnsLeft() + " turns left)";
                }
                addInfoLabel(message);
                return;
            }

            JComboBox<UnitType> unitCombo = new JComboBox<>(UnitType.values());
            setDisplayNameRenderer(unitCombo, UnitType::getDisplayName);
            buttonContainer.add(unitCombo);
            JButton produceButton = styledButton("Produce Unit");
            produceButton.setEnabled(myTurn);
            produceButton.addActionListener(e -> {
                UnitType selected = (UnitType) unitCombo.getSelectedItem();
                if (selected != null) {
                    boardPanel.sendQuiet(new StartUnitProductionRequest(buildingId, selected.name()));
                }
            });
            buttonContainer.add(produceButton);

            TownHallLevel nextLevel = building.getTownHallLevel().getNextLevel();
            if (nextLevel != null) {
                JButton upgradeButton = styledButton("Upgrade to " + nextLevel.getDisplayName() + " ("
                        + nextLevel.getUpgradeWoodCost() + "W/" + nextLevel.getUpgradeStoneCost() + "S/"
                        + nextLevel.getUpgradeIronCost() + "I)");
                upgradeButton.setEnabled(myTurn);
                upgradeButton.addActionListener(e -> boardPanel.sendQuiet(new StartUpgradeRequest(buildingId)));
                buttonContainer.add(upgradeButton);
            } else {
                addInfoLabel("Town Hall is at max level (" + building.getTownHallLevel().getDisplayName() + ")");
            }

            List<TechType> remainingTechs = new ArrayList<>();
            for (TechType tech : TechType.values()) {
                if (!hasTech(tech)) {
                    remainingTechs.add(tech);
                }
            }
            if (!remainingTechs.isEmpty()) {
                JComboBox<TechType> techCombo = new JComboBox<>(remainingTechs.toArray(new TechType[0]));
                setDisplayNameRenderer(techCombo, TechType::getDisplayName);
                buttonContainer.add(techCombo);
                JButton researchButton = styledButton("Research");
                researchButton.setEnabled(myTurn);
                researchButton.addActionListener(e -> {
                    TechType selected = (TechType) techCombo.getSelectedItem();
                    if (selected != null) {
                        boardPanel.sendQuiet(new StartTechResearchRequest(buildingId, selected.name()));
                    }
                });
                buttonContainer.add(researchButton);
            } else {
                addInfoLabel("All technologies researched");
            }
        } else if (building.getType() == BuildingType.APOTHECARY) {
            JComboBox<ItemType> itemCombo = new JComboBox<>(ItemType.values());
            setDisplayNameRenderer(itemCombo, ItemType::getDisplayName);
            buttonContainer.add(itemCombo);
            JButton produceItemButton = styledButton("Produce Item (15 Wood, 10 Stone)");
            produceItemButton.setEnabled(myTurn);
            produceItemButton.addActionListener(e -> {
                ItemType selected = (ItemType) itemCombo.getSelectedItem();
                if (selected != null) {
                    boardPanel.sendQuiet(new ProduceItemRequest(buildingId, selected.name()));
                }
            });
            buttonContainer.add(produceItemButton);
        } else {
            addInfoLabel("No special actions for this building.");
        }
    }

    private <T> void setDisplayNameRenderer(JComboBox<T> combo, Function<T, String> displayFn) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    label.setText(displayFn.apply((T) value));
                }
                return label;
            }
        });
    }

    private void addSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(new Color(241, 196, 15));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 6, 4, 6));
        buttonContainer.add(label);
    }

    private void addInfoLabel(String text) {
        JLabel label = new JLabel("<html><body style='width: 240px'>" + text + "</body></html>");
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(Color.LIGHT_GRAY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        buttonContainer.add(label);
    }

    private JButton styledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setBackground(BUTTON_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusable(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return button;
    }
}
