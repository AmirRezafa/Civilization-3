package view.Panels;

import controller.GameController;
import controller.events.EventBus;
import controller.events.UnitActionsChangedEvent;
import model.*;

import javax.swing.*;
import java.awt.*;

public class UnitActionPanel extends JPanel {
    private final GameController GC;
    private final JPanel buttonContainer;

    private enum SubMenu {
        MAIN, TRAIN, STORAGE, TECH
    }

    private SubMenu currentSubMenu = SubMenu.MAIN;

    private static final ResourceType[] TRADABLE_RESOURCES = {
            ResourceType.WOOD, ResourceType.STONE, ResourceType.IRON, ResourceType.WHEAT
    };

    private ResourceType bazaarSellResource = ResourceType.WOOD;
    private ResourceType bazaarRewardResource = ResourceType.STONE;
    private ResourceType tradingPostSellResource = ResourceType.WOOD;
    private ResourceType tradingPostRewardResource = ResourceType.STONE;
    private int tradingPostAmount = 50;
    private ResourceType tribeSellResource = ResourceType.WOOD;
    private ResourceType tribeRewardResource = null;

    private int a;

    public UnitActionPanel(GameController gc) {
        this.GC = gc;
        setVisible(false);

        this.setLayout(new BorderLayout());
        this.setBackground(new Color(40, 40, 40));
        this.setPreferredSize(new Dimension(0, 80));

        buttonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 25));
        buttonContainer.setOpaque(false);
        this.add(buttonContainer, BorderLayout.CENTER);
        setOpaque(false);

        EventBus.subscribe(UnitActionsChangedEvent.class, e -> updateActions());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.dispose();

        super.paintComponent(g);
    }

    public void showProducingMSG(Building building){
        String msg;
        if (building.getProducingUnit() != null) {
            msg = "Producing: " + building.getProducingUnit().getDisplayName() +
                    " (" + building.getProductionTurnsLeft() + " Turns Left)";
        } else if (building.getUpgradingToLevel() != null) {
            msg = "Upgrading to: " + building.getUpgradingToLevel().getDisplayName() +
                    " (" + building.getProductionTurnsLeft() + " Turns Left)";
        } else {
            msg = "Researching: " + building.getResearchingTech().getDisplayName() +
                    " (" + building.getProductionTurnsLeft() + " Turns Left)";
        }
        JLabel producingLabel = new JLabel(msg);
        producingLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        producingLabel.setForeground(new Color(241, 196, 15));

        buttonContainer.add(producingLabel);

        JButton cancelBtn = new JButton("Cancel (no refund)");
        cancelBtn.setFocusable(false);
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        cancelBtn.setBackground(new Color(149, 165, 166));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.addActionListener(e -> {
            GC.cancelTownHallProduction();
            updateActions();
        });
        buttonContainer.add(cancelBtn);
    }

    public void showProduceButtons(){
        for (UnitType uType : UnitType.values()) {
            int woodCost = uType == UnitType.SWORDSMAN ? 10 : 0;
            String costText = uType.getFoodCost() + " Food" + (woodCost > 0 ? ", " + woodCost + " Wood" : "");
            String btnText = "Train " + uType.getDisplayName() +
                    " (" + costText + ", " + uType.getBuildTurns() + " Turns)";

            JButton trainBtn = new JButton(btnText);
            trainBtn.setFocusable(false);
            trainBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

            boolean hasFood = GC.hasEnoughFood(uType.getFoodCost());
            boolean hasWood = woodCost <= 0 || GC.hasEnoughWood(woodCost);
            boolean unitCapOk = GC.checkUnitCap();
            boolean militaryCapOk = !GC.isMilitaryUnit(uType) || GC.checkMilitaryUnitCap();
            boolean stableOk = uType != UnitType.CAVALRY || GC.hasStable();
            boolean archerLevelOk = uType != UnitType.ARCHER || GC.getTownHallLevel().getLevelNumber() >= 2;
            boolean cavalryLevelOk = uType != UnitType.CAVALRY || GC.getTownHallLevel().getLevelNumber() >= 2;
            boolean enabled = hasFood && hasWood && unitCapOk && militaryCapOk && stableOk && archerLevelOk && cavalryLevelOk;
            trainBtn.setEnabled(enabled);

            if (!enabled) {
                String reason;
                if (!hasFood) reason = "Not enough food.";
                else if (!hasWood) reason = "Not enough wood.";
                else if (!unitCapOk) reason = "Total unit cap reached.";
                else if (!militaryCapOk) reason = "Military unit cap reached for this Town Hall level.";
                else if (!archerLevelOk || !cavalryLevelOk) reason = "Requires Town Hall level 2 (Settlement).";
                else reason = "Requires an active Stable on the map.";
                trainBtn.setToolTipText(reason);
            }

            trainBtn.addActionListener(e -> {
                GC.startProducingUnitInTownHall(uType);
                updateActions();
            });

            buttonContainer.add(trainBtn);
        }
    }

    private void showUnassignButton(int workerCount){
        JButton unassignBtn = new JButton("Unassign Worker (" + workerCount + ")");
        unassignBtn.setFocusable(false);
        unassignBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        unassignBtn.setBackground(new Color(231, 76, 60));
        unassignBtn.setForeground(Color.WHITE);

        unassignBtn.addActionListener(e -> {
            GC.removeWorker();
            updateActions();
        });

        buttonContainer.add(unassignBtn);
    }

    private void showBuildButtons(Tile currentTile){
        for (BuildingType bType : BuildingType.values()) {
            if (!bType.isPlayerBuildable()) {
                continue;
            }

            JButton buildBtn = new JButton("Build " + bType.getDisplayName()
                    + " (" + bType.getCostString() + ")");
            buildBtn.setFocusable(false);
            buildBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

            boolean isValidTerrain = bType.isBuildableAt(currentTile, GC.getTiles())
                    && bType.isUnlocked(GC.hasStoneTech(), GC.hasIronTech(), GC.hasSettlementTech());
            boolean isTileEmpty = (currentTile.getBuilding() == null);
            boolean inTerritory = currentTile.isOwned();
            boolean levelMet = GC.getTownHallLevel().getLevelNumber() >= bType.getRequiredTownHallLevel();
            boolean notInTribeZone = !GC.isInTribeForbiddenZone(currentTile.getCol(), currentTile.getRow());

            boolean enabled = isValidTerrain && isTileEmpty && inTerritory && levelMet && notInTribeZone;
            buildBtn.setEnabled(enabled);
            if (!enabled && !notInTribeZone) {
                buildBtn.setToolTipText("Too close to an unfriendly tribe's camp.");
            }

            buildBtn.addActionListener(e -> {
                GC.constructBuilding(bType);
                updateActions();
            });

            buttonContainer.add(buildBtn);
        }
    }

    private void showAttackButton() {
        String hint = GC.hasArcherAvailable()
                ? "Attack (whole stack) - then right-click an adjacent or 2-hex-away target"
                : "Attack (whole stack) - then right-click an adjacent target";
        JButton attackBtn = new JButton(hint);
        attackBtn.setFocusable(false);
        attackBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        attackBtn.setBackground(new Color(192, 57, 43));
        attackBtn.setForeground(Color.WHITE);
        attackBtn.addActionListener(e -> {
            GC.startAttacking();
            updateActions();
        });
        buttonContainer.add(attackBtn);
    }

    private void showEdgeBuildButtons() {
        JButton roadBtn = new JButton("Build Road (10 Wood) - then right-click a neighbor");
        roadBtn.setFocusable(false);
        roadBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        roadBtn.setEnabled(GC.hasEnoughWood(10));
        roadBtn.addActionListener(e -> {
            GC.startBuildingEdge(EdgeFeature.ROAD);
            updateActions();
        });
        buttonContainer.add(roadBtn);

        JButton wallBtn = new JButton("Build Wall (30 Stone) - then right-click a neighbor");
        wallBtn.setFocusable(false);
        wallBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        wallBtn.setEnabled(GC.hasEnoughStone(30));
        wallBtn.addActionListener(e -> {
            GC.startBuildingEdge(EdgeFeature.WALL);
            updateActions();
        });
        buttonContainer.add(wallBtn);
    }

    private JComboBox<ResourceType> makeResourceCombo(ResourceType selected, java.util.function.Consumer<ResourceType> onChange) {
        JComboBox<ResourceType> combo = new JComboBox<>(TRADABLE_RESOURCES);
        combo.setSelectedItem(selected);
        combo.setFocusable(false);
        combo.addActionListener(e -> {
            onChange.accept((ResourceType) combo.getSelectedItem());
            updateActions();
        });
        return combo;
    }

    private void showBazaarButtons() {
        buttonContainer.add(new JLabel("Sell:"));
        buttonContainer.add(makeResourceCombo(bazaarSellResource, r -> bazaarSellResource = r));
        buttonContainer.add(new JLabel("For:"));
        buttonContainer.add(makeResourceCombo(bazaarRewardResource, r -> bazaarRewardResource = r));

        int[] tiers = {10, 100, 500};
        for (int tier : tiers) {
            double rate = GC.bazaarRateForTier(tier);
            JButton tradeBtn = new JButton("Trade " + tier + " " + bazaarSellResource.name() +
                    " -> " + (int) (tier * rate) + " " + bazaarRewardResource.name() + " (" + (int) (rate * 100) + "%)");
            tradeBtn.setFocusable(false);
            tradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            boolean sameResource = bazaarSellResource == bazaarRewardResource;
            boolean enabled = !sameResource && GC.canUseBazaar() && GC.hasEnoughResource(bazaarSellResource, tier);
            tradeBtn.setEnabled(enabled);
            if (!enabled) {
                tradeBtn.setToolTipText(sameResource ? "Pick two different resources." :
                        !GC.canUseBazaar() ? "Only one Bazaar trade per turn." : "Not enough " + bazaarSellResource.name() + ".");
            }
            tradeBtn.addActionListener(e -> {
                GC.tradeAtBazaar(bazaarSellResource, bazaarRewardResource, tier);
                updateActions();
            });
            buttonContainer.add(tradeBtn);
        }
    }

    private void showTradingPostButtons(Tile currentTile) {
        if (!currentTile.isOwned()) {
            JLabel label = new JLabel("Trading Post is outside your territory");
            label.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            label.setForeground(Color.WHITE);
            buttonContainer.add(label);
            return;
        }

        buttonContainer.add(new JLabel("Sell:"));
        buttonContainer.add(makeResourceCombo(tradingPostSellResource, r -> tradingPostSellResource = r));
        buttonContainer.add(new JLabel("For:"));
        buttonContainer.add(makeResourceCombo(tradingPostRewardResource, r -> tradingPostRewardResource = r));

        JTextField amountField = new JTextField(String.valueOf(tradingPostAmount), 4);
        amountField.addActionListener(e -> {
            try {
                tradingPostAmount = Math.max(1, Integer.parseInt(amountField.getText().trim()));
            } catch (NumberFormatException ignored) { /* keep previous amount */ }
            updateActions();
        });
        buttonContainer.add(amountField);

        JButton tradeBtn = new JButton("Trade " + tradingPostAmount + " " + tradingPostSellResource.name() +
                " -> " + (int) (tradingPostAmount * 0.80) + " " + tradingPostRewardResource.name() + " (80%)");
        tradeBtn.setFocusable(false);
        tradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        boolean sameResource = tradingPostSellResource == tradingPostRewardResource;
        boolean enabled = !sameResource && GC.canUseTradingPost() && GC.hasEnoughResource(tradingPostSellResource, tradingPostAmount);
        tradeBtn.setEnabled(enabled);
        if (!enabled) {
            tradeBtn.setToolTipText(sameResource ? "Pick two different resources." :
                    !GC.canUseTradingPost() ? "Only one Trading Post trade per turn." : "Not enough " + tradingPostSellResource.name() + ".");
        }
        tradeBtn.addActionListener(e -> {
            GC.tradeAtTradingPost(tradingPostSellResource, tradingPostRewardResource, tradingPostAmount);
            updateActions();
        });
        buttonContainer.add(tradeBtn);
    }

    private void showTribeCampButtons(Tile currentTile) {
        Tribe tribe = null;
        for (Tribe t : GC.getTribes()) {
            if (t.getCol() == currentTile.getCol() && t.getRow() == currentTile.getRow()) {
                tribe = t;
                break;
            }
        }
        if (tribe == null) return;

        JLabel infoLabel = new JLabel(tribe.getName() + " (" + tribe.getType().getDisplayName() +
                ") | Camp: [" + tribe.getCol() + ", " + tribe.getRow() + "]" +
                " | Relationship: " + tribe.getRelationship() +
                " (" + tribe.getRelationshipValue() + ") | Camp HP: " + currentTile.getBuilding().getHP() +
                "/" + currentTile.getBuilding().getMaxHP() +
                " | Guards: " + GC.countGuardUnits(tribe));
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        infoLabel.setForeground(Color.WHITE);
        buttonContainer.add(infoLabel);

        Tribe finalTribe = tribe;

        if (currentTile.getBuilding().isDestroyed()) {
            JButton captureBtn = new JButton("Capture Camp (turns into Outpost)");
            captureBtn.setFocusable(false);
            captureBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            captureBtn.setBackground(new Color(46, 204, 113));
            captureBtn.setForeground(Color.WHITE);
            captureBtn.addActionListener(e -> {
                GC.captureTribeCamp(finalTribe);
                updateActions();
            });
            buttonContainer.add(captureBtn);
            return;
        }

        if (tribe.getType().canTrade()) {
            java.util.List<ResourceType> rewardOptions = tribe.getType().getTradeRewardOptions();
            if (tribeRewardResource == null || !rewardOptions.contains(tribeRewardResource)) {
                tribeRewardResource = rewardOptions.get(0);
            }

            int sellAmount = 20;
            boolean canTrade = GC.canTradeWithTribe(tribe);

            buttonContainer.add(new JLabel("Sell:"));
            buttonContainer.add(makeResourceCombo(tribeSellResource, r -> tribeSellResource = r));
            if (rewardOptions.size() > 1) {
                buttonContainer.add(new JLabel("For:"));
                JComboBox<ResourceType> rewardCombo = new JComboBox<>(rewardOptions.toArray(new ResourceType[0]));
                rewardCombo.setSelectedItem(tribeRewardResource);
                rewardCombo.setFocusable(false);
                rewardCombo.addActionListener(e -> {
                    tribeRewardResource = (ResourceType) rewardCombo.getSelectedItem();
                    updateActions();
                });
                buttonContainer.add(rewardCombo);
            }

            JButton tradeBtn = new JButton("Trade " + sellAmount + " " + tribeSellResource.name() + " -> " +
                    (int) (sellAmount * tribe.getType().getTradeRate()) + " " + tribeRewardResource.name());
            tradeBtn.setFocusable(false);
            tradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            boolean sameResource = tribeSellResource == tribeRewardResource;
            boolean enabled = canTrade && !sameResource && GC.hasEnoughResource(tribeSellResource, sellAmount);
            tradeBtn.setEnabled(enabled);
            tradeBtn.setToolTipText(!canTrade ? "Requires Friendly/Allied relationship (>= 20) and one trade per turn" :
                    sameResource ? "Pick two different resources." :
                    !enabled ? "Not enough " + tribeSellResource.name() + "." : null);
            tradeBtn.addActionListener(e -> {
                GC.tradeWithTribe(finalTribe, tribeSellResource, sellAmount, tribeRewardResource);
                updateActions();
            });
            buttonContainer.add(tradeBtn);
        }

        addGiftButton(finalTribe, ResourceType.WOOD, 50);
        addGiftButton(finalTribe, ResourceType.WHEAT, 50);
        addGiftButton(finalTribe, ResourceType.STONE, 30);
        addGiftButton(finalTribe, ResourceType.IRON, 30);

        if (tribe.getActiveQuest() == null) {
            boolean canOffer = GC.canOfferQuestToTribe(tribe);
            JButton questBtn = new JButton("Ask for a Quest");
            questBtn.setFocusable(false);
            questBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            questBtn.setEnabled(canOffer);
            questBtn.setToolTipText(canOffer ? null :
                    "Requires relationship >= 20, not at war, and this tribe's quest cooldown to have passed");
            questBtn.addActionListener(e -> {
                GC.issueQuestToTribe(finalTribe);
                updateActions();
            });
            buttonContainer.add(questBtn);
        } else {
            Quest quest = tribe.getActiveQuest();
            String status = quest.isCompleted() ? "Completed" : quest.isReadyToDeliver() ? "Ready to deliver" : "In Progress";
            JLabel questLabel = new JLabel("Quest: " + quest.getDescription() +
                    " | Deadline: Turn " + quest.getDeadlineTurn() + " | " + status);
            questLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            questLabel.setForeground(Color.WHITE);
            buttonContainer.add(questLabel);

            if (!quest.isCompleted()) {
                boolean canDeliver = GC.canDeliverQuest(finalTribe);
                JButton deliverBtn = new JButton("Deliver Quest");
                deliverBtn.setFocusable(false);
                deliverBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
                deliverBtn.setEnabled(canDeliver);
                deliverBtn.setToolTipText(canDeliver ? null :
                        !quest.isReadyToDeliver() ? "Quest condition not met yet." : "Not enough storage room for the reward.");
                deliverBtn.addActionListener(e -> {
                    GC.deliverQuest(finalTribe);
                    updateActions();
                });
                buttonContainer.add(deliverBtn);

                JButton cancelQuestBtn = new JButton("Cancel Quest (-5 relationship)");
                cancelQuestBtn.setFocusable(false);
                cancelQuestBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
                cancelQuestBtn.addActionListener(e -> {
                    GC.cancelTribeQuest(finalTribe);
                    updateActions();
                });
                buttonContainer.add(cancelQuestBtn);
            }
        }

        JButton rewardsBtn = new JButton("View Tribe Rewards");
        rewardsBtn.setFocusable(false);
        rewardsBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        rewardsBtn.addActionListener(e -> {
            StringBuilder info = new StringBuilder();
            if (finalTribe.getType().canTrade()) {
                info.append("Trade rate: ").append((int) (finalTribe.getType().getTradeRate() * 100))
                        .append("% into ");
                java.util.List<ResourceType> options = finalTribe.getType().getTradeRewardOptions();
                for (int i = 0; i < options.size(); i++) {
                    info.append(options.get(i).name());
                    if (i < options.size() - 1) info.append(" / ");
                }
                info.append(".\n");
            } else {
                info.append("This tribe type does not offer trade.\n");
            }
            info.append("Allied bonus: ").append(GC.describeAllianceBonus(finalTribe.getType()));
            JOptionPane.showMessageDialog(this, info.toString(), "Tribe Rewards", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonContainer.add(rewardsBtn);

        boolean canDeclareWar = GC.canDeclareWarOnTribe(tribe);
        JButton declareWarBtn = new JButton("Declare War");
        declareWarBtn.setFocusable(false);
        declareWarBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        declareWarBtn.setBackground(new Color(192, 57, 43));
        declareWarBtn.setForeground(Color.WHITE);
        declareWarBtn.setEnabled(canDeclareWar);
        declareWarBtn.setToolTipText(canDeclareWar ? "Friendly costs -5 happiness, Allied costs -15 happiness." :
                "Already at war with this tribe.");
        declareWarBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Declaring war cannot be undone and will drop relationship to -100. Continue?",
                    "Declare War", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                GC.declareWarOnTribe(finalTribe);
                updateActions();
            }
        });
        buttonContainer.add(declareWarBtn);

        boolean canPeace = GC.canRequestPeaceWithTribe(tribe);
        JButton peaceBtn = new JButton("Request Peace (30 Food, 30 Wood, 30 Iron)");
        peaceBtn.setFocusable(false);
        peaceBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        peaceBtn.setEnabled(canPeace);
        peaceBtn.setToolTipText(canPeace ? null : "Only available while at war with this tribe");
        peaceBtn.addActionListener(e -> {
            GC.requestPeaceWithTribe(finalTribe);
            updateActions();
        });
        buttonContainer.add(peaceBtn);

        if (tribe.isAllianceActive()) {
            JLabel allianceLabel = new JLabel("Alliance Active: " + GC.describeAllianceBonus(tribe.getType()));
            allianceLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            allianceLabel.setForeground(new Color(46, 204, 113));
            buttonContainer.add(allianceLabel);
        } else {
            boolean canAlliance = GC.canRequestAllianceWithTribe(tribe);
            JButton allianceBtn = new JButton("Request Alliance");
            allianceBtn.setFocusable(false);
            allianceBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            allianceBtn.setEnabled(canAlliance);
            if (!canAlliance) {
                String exclusivityReason = GC.allianceExclusivityReason(tribe);
                allianceBtn.setToolTipText(exclusivityReason != null ? exclusivityReason :
                        "Requires relationship >= 70, no active war, and no failed quest from this tribe in the last 5 turns.");
            }
            allianceBtn.addActionListener(e -> {
                GC.requestAllianceWithTribe(finalTribe);
                updateActions();
            });
            buttonContainer.add(allianceBtn);
        }
    }

    private void addGiftButton(Tribe tribe, ResourceType resource, int amount) {
        JButton giftBtn = new JButton("Send Gift (" + amount + " " + resource.name() + ")");
        giftBtn.setFocusable(false);
        giftBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        boolean canSend = GC.canSendGiftToTribe(tribe);
        boolean canAfford = GC.hasEnoughResource(resource, amount);
        giftBtn.setEnabled(canSend && canAfford);
        if (!canSend) {
            giftBtn.setToolTipText("An enemy tribe won't accept gifts.");
        } else if (!canAfford) {
            giftBtn.setToolTipText("Not enough " + resource.name() + ".");
        }

        giftBtn.addActionListener(e -> {
            GC.sendGiftToTribe(tribe, resource, amount);
            updateActions();
        });
        buttonContainer.add(giftBtn);
    }

    private static final java.util.Set<BuildingType> NON_DEMOLISHABLE_TYPES = java.util.Set.of(
            BuildingType.TOWN_HALL, BuildingType.TRIBE_CAMP, BuildingType.TRADING_POST);

    private void showDeconstructButtons(Tile currentTile) {
        if (currentTile.getBuilding() != null && !NON_DEMOLISHABLE_TYPES.contains(currentTile.getBuilding().getType())) {
            JButton deconstructBtn = new JButton("Deconstruct " + currentTile.getBuilding().getType().getDisplayName() + " (1 AP)");
            deconstructBtn.setFocusable(false);
            deconstructBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
            deconstructBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Deconstruct " + currentTile.getBuilding().getType().getDisplayName() +
                                "? This cannot be undone.",
                        "Confirm Deconstruction", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    GC.deconstructBuilding();
                    updateActions();
                }
            });
            buttonContainer.add(deconstructBtn);
        }

        JButton deconstructEdgeBtn = new JButton("Deconstruct Road/Wall (1 AP) - then right-click a neighbor");
        deconstructEdgeBtn.setFocusable(false);
        deconstructEdgeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        deconstructEdgeBtn.addActionListener(e -> {
            GC.startDeconstructingEdge();
            updateActions();
        });
        buttonContainer.add(deconstructEdgeBtn);

        JButton deconstructNeighborBtn = new JButton("Deconstruct Building on Neighbor Hex (1 AP) - then right-click it");
        deconstructNeighborBtn.setFocusable(false);
        deconstructNeighborBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        deconstructNeighborBtn.addActionListener(e -> {
            GC.startDeconstructingBuilding();
            updateActions();
        });
        buttonContainer.add(deconstructNeighborBtn);
    }

    private void showWorkHereButton(){
        JButton workBtn = new JButton("Work Here");
        workBtn.setFocusable(false);
        workBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        workBtn.setBackground(new Color(46, 204, 113));
        workBtn.setForeground(Color.WHITE);

        workBtn.addActionListener(e -> {
            GC.assignWorkerToBuilding();
            updateActions();
        });

        buttonContainer.add(workBtn);
    }

    private void showExpandBorderHereButton(){
        JButton expandBtn = new JButton("Expand Borders Here");
        expandBtn.setFocusable(false);
        expandBtn.setBackground(new Color(155, 89, 182));
        expandBtn.setForeground(Color.WHITE);
        expandBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        expandBtn.addActionListener(e -> {
            GC.expandTerritory();
            updateActions();
        });

        buttonContainer.add(expandBtn);
    }

    private void showTownHallUpgradeButton() {
        TownHallLevel currentLevel = GC.getTownHallLevel();
        TownHallLevel nextLevel = currentLevel.getNextLevel();

        JButton upgradeBtn = new JButton();
        upgradeBtn.setFocusable(false);
        upgradeBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

        if (nextLevel == null) {
            upgradeBtn.setText("Town Hall Maxed Out (" + currentLevel.getDisplayName() + ")");
            upgradeBtn.setEnabled(false);
            buttonContainer.add(upgradeBtn);
            return;
        }

        upgradeBtn.setText("Upgrade to " + nextLevel.getDisplayName()
                + " (" + nextLevel.getUpgradeWoodCost() + " Wood, "
                + nextLevel.getUpgradeStoneCost() + " Stone, "
                + nextLevel.getUpgradeIronCost() + " Iron)");
        upgradeBtn.setEnabled(GC.hasEnoughWood(nextLevel.getUpgradeWoodCost()) &&
                GC.hasEnoughStone(nextLevel.getUpgradeStoneCost()) &&
                GC.hasEnoughIron(nextLevel.getUpgradeIronCost()));

        upgradeBtn.addActionListener(e -> {
            GC.upgradeTownHall();
            updateActions();
        });

        buttonContainer.add(upgradeBtn);
    }

    private void showResearchButtons() {
        for (TechType tech : TechType.values()) {
            JButton techBtn = new JButton(tech.getDisplayName() + " (" + tech.getCostString() + ")");
            techBtn.setFocusable(false);
            techBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));

            if (GC.hasTech(tech)) {
                techBtn.setText(tech.getDisplayName() + " ✅");
                techBtn.setEnabled(false);
            } else {
                boolean levelMet = GC.getTownHallLevel().getLevelNumber() >= tech.getRequiredLevel().getLevelNumber();
                boolean canAfford = GC.hasEnoughResource(tech.getCostResource(), tech.getCostAmount());
                techBtn.setEnabled(levelMet && canAfford);
                if (!levelMet) {
                    techBtn.setToolTipText("Requires Town Hall level " + tech.getRequiredLevel().getLevelNumber() + ".");
                } else if (!canAfford) {
                    techBtn.setToolTipText("Not enough " + tech.getCostResource() + ".");
                }

                techBtn.addActionListener(e -> {
                    GC.researchTech(tech);
                    updateActions();
                });
            }

            buttonContainer.add(techBtn);
        }
    }

    private void showTownHallMainMenu() {
        JButton trainMenuBtn = new JButton("Train Units");
        trainMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        trainMenuBtn.setFocusable(false);
        trainMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.TRAIN;
            updateActions();
        });

        JButton storageMenuBtn = new JButton("Upgrade Town Hall");
        storageMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        storageMenuBtn.setFocusable(false);
        storageMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.STORAGE;
            updateActions();
        });

        JButton techMenuBtn = new JButton("Research Tech");
        techMenuBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        techMenuBtn.setFocusable(false);
        techMenuBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.TECH;
            updateActions();
        });

        buttonContainer.add(trainMenuBtn);
        buttonContainer.add(storageMenuBtn);
        buttonContainer.add(techMenuBtn);
    }

    private void showBackButton() {
        JButton backBtn = new JButton("Back");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, (int) (a * 0.4)));
        backBtn.setFocusable(false);
        backBtn.setBackground(new Color(149, 165, 166));
        backBtn.setForeground(Color.WHITE);
        backBtn.addActionListener(e -> {
            currentSubMenu = SubMenu.MAIN;
            updateActions();
        });
        buttonContainer.add(backBtn);
    }

    public void updateActions() {
        buttonContainer.removeAll();

        a = GC.getB();
        int hGap = (int) (a * 0.6);
        int vGap = (int) (a * 0.8);
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT, hGap, vGap));

        Unit selectedUnit = GC.getSelectedUnit();
        Tile currentTile = GC.getTileUnderUnit();

        setVisible(false);
        if (selectedUnit == null) {
            if(currentTile == null) return;
            Building building = currentTile.getBuilding();
            if(building == null) return;
            int workerCount = building.getStationedWorkers().size();

            if (building.getType() == BuildingType.BAZAAR) {
                showBazaarButtons();
                setVisible(true);
                return;
            }

            if (building.getType() == BuildingType.TRADING_POST) {
                showTradingPostButtons(currentTile);
                setVisible(true);
                return;
            }

            if (building.getType() == BuildingType.TRIBE_CAMP) {
                showTribeCampButtons(currentTile);
                setVisible(true);
                return;
            }

            if(workerCount == 0 && building.getType() == BuildingType.TOWN_HALL){
                if (building.isProducing()) {
                    showProducingMSG(building);
                } else {
                    switch (currentSubMenu) {
                        case MAIN -> showTownHallMainMenu();
                        default -> {
                            showBackButton();
                            switch (currentSubMenu) {
                                case TRAIN -> showProduceButtons();
                                case STORAGE -> showTownHallUpgradeButton();
                                case TECH -> showResearchButtons();
                            }
                        }
                    }
                }
                setVisible(true);
            return;
            }

            if(workerCount != 0){
                showUnassignButton(workerCount);
                setVisible(true);
            }
            refreshUI();
            return;
        }

        if (selectedUnit.getType() == UnitType.BUILDER) {
            showBuildButtons(currentTile);
            showEdgeBuildButtons();
            showDeconstructButtons(currentTile);
            setVisible(true);
        }else if(selectedUnit.getType() == UnitType.WORKER){
            Building build = GC.getTileUnderUnit().getBuilding();
            if(build == null) return;

            if(!build.needWorker()) return;

            showWorkHereButton();

            setVisible(true);
        } else if (selectedUnit.getType() == UnitType.BORDER_EXPANDER) {
            showExpandBorderHereButton();
            setVisible(true);
        } else if (GC.isMilitaryUnit(selectedUnit.getType())) {
            showAttackButton();
            setVisible(true);
        }

        refreshUI();
    }

    private void refreshUI() {
        buttonContainer.revalidate();
        buttonContainer.repaint();
    }
}