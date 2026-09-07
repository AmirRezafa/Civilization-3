package view.Panels;

import model.ResourceType;
import network.client.NetworkClient;
import network.client.NetworkListener;
import network.protocol.BuildingConstructedMessage;
import network.protocol.CheatAppliedMessage;
import network.protocol.ChatMessage;
import network.protocol.CombatResultMessage;
import network.protocol.ConnectRequest;
import network.protocol.ConnectResponse;
import network.protocol.DeclareWarRequest;
import network.protocol.DiplomacyChangedMessage;
import network.protocol.DisasterOccurredMessage;
import network.protocol.EndTurnRequest;
import network.protocol.ErrorMessage;
import network.protocol.GameOverMessage;
import network.protocol.GameSavedMessage;
import network.protocol.GameStartedMessage;
import network.protocol.GameStateSnapshotMessage;
import network.protocol.ItemUsedMessage;
import network.protocol.LoadGameRequest;
import network.protocol.LobbyStateMessage;
import network.protocol.Message;
import network.protocol.PlayerEconomyMessage;
import network.protocol.PlayerEliminatedMessage;
import network.protocol.ProductionCompletedMessage;
import network.protocol.ProductionStartedMessage;
import network.protocol.SaveGameRequest;
import network.protocol.SelectMapRequest;
import network.protocol.SetReadyRequest;
import network.protocol.StartGameRequest;
import network.protocol.TradeOfferReceivedMessage;
import network.protocol.TradeOfferRequest;
import network.protocol.TradeOfferResolvedMessage;
import network.protocol.TradeRespondRequest;
import network.protocol.TurnChangedMessage;
import network.protocol.UnitMovedMessage;
import network.protocol.UnitRemovedMessage;
import view.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LobbyPanel extends JPanel {
    private static final Color BACKGROUND = new Color(25, 25, 25);
    private static final Color FIELD_PANEL_BACKGROUND = new Color(35, 35, 35);
    private static final Color BUTTON_COLOR = new Color(52, 73, 94);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 28);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font STATUS_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Dimension BUTTON_SIZE = new Dimension(170, 42);
    private static final Dimension FIELD_SIZE = new Dimension(200, 32);

    private final MainFrame mainFrame;
    private final CardLayout innerLayout = new CardLayout();
    private final JPanel innerContainer = new JPanel(innerLayout);

    private final JTextField hostField = new JTextField("localhost");
    private final JTextField portField = new JTextField("5000");
    private final JTextField nameField = new JTextField();
    private final JLabel connectStatusLabel = styledStatusLabel();

    private final DefaultListModel<LobbyStateMessage.PlayerEntry> rosterModel = new DefaultListModel<>();
    private final JList<LobbyStateMessage.PlayerEntry> rosterList = new JList<>(rosterModel);
    private final JButton readyButton = styledButton("Ready Up");
    private final JButton startButton = styledButton("Start Game");
    private final JButton loadButton = styledButton("Load Saved Game");
    private final JButton declareWarButton = styledButton("Declare War");
    private final JLabel lobbyStatusLabel = styledStatusLabel();
    private final Set<String> myEnemyIds = new HashSet<>();

    private final JComboBox<String> mapComboBox = new JComboBox<>(new String[]{"Grasslands Valley", "Highland Frontier", "Coastal Reach"});
    private boolean applyingRemoteMapState;

    private final JLabel turnLabel = styledStatusLabel();
    private final JButton endTurnButton = styledButton("End Turn");
    private final JButton saveButton = styledButton("Save Game");
    private final JButton tradeButton = styledButton("Propose Trade");
    private final JLabel startedLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel resourcesLabel = styledStatusLabel();
    private final JLabel combatLogLabel = styledStatusLabel();
    private final NetworkBoardPanel boardPanel = new NetworkBoardPanel();
    private final ActionsPanel actionsPanel = new ActionsPanel(boardPanel);
    private String currentMapName;
    private static final ResourceType[] TRADABLE_RESOURCES = {
            ResourceType.WOOD, ResourceType.STONE, ResourceType.IRON,
            ResourceType.WHEAT, ResourceType.CATTLE, ResourceType.FISH
    };

    private final JTextArea chatLog = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JButton chatSendButton = styledButton("Send");

    private NetworkClient client;
    private String myPlayerId;
    private String hostPlayerId;
    private String currentTurnPlayerId;
    private boolean ready;

    public LobbyPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        JLabel title = new JLabel("MULTIPLAYER LOBBY", SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        innerContainer.setBackground(BACKGROUND);
        innerContainer.add(buildConnectCard(), "CONNECT");
        innerContainer.add(buildRosterCard(), "ROSTER");
        innerContainer.add(buildStartedCard(), "STARTED");
        add(innerContainer, BorderLayout.CENTER);
        add(buildChatPanel(), BorderLayout.EAST);

        JButton backButton = styledButton("Back to Menu");
        backButton.addActionListener(e -> {
            disconnectQuietly();
            mainFrame.showMenu();
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(BACKGROUND);
        bottom.add(backButton);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel buildConnectCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        hostField.setPreferredSize(FIELD_SIZE);
        portField.setPreferredSize(FIELD_SIZE);
        nameField.setPreferredSize(FIELD_SIZE);

        c.gridx = 0;
        c.gridy = 0;
        panel.add(styledLabel("Server address:"), c);
        c.gridx = 1;
        panel.add(hostField, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(styledLabel("Port:"), c);
        c.gridx = 1;
        panel.add(portField, c);

        c.gridx = 0;
        c.gridy = 2;
        panel.add(styledLabel("Your name:"), c);
        c.gridx = 1;
        panel.add(nameField, c);

        JButton connectButton = styledButton("Connect");
        connectButton.addActionListener(e -> attemptConnect());
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(16, 8, 8, 8);
        panel.add(connectButton, c);

        c.gridy = 4;
        c.insets = new Insets(4, 8, 8, 8);
        panel.add(connectStatusLabel, c);
        return panel;
    }

    private JPanel buildRosterCard() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        rosterList.setBackground(FIELD_PANEL_BACKGROUND);
        rosterList.setForeground(Color.WHITE);
        rosterList.setSelectionBackground(BUTTON_COLOR);
        rosterList.setFont(LABEL_FONT);
        rosterList.setFixedCellHeight(30);
        rosterList.setCellRenderer(new RosterCellRenderer());

        JScrollPane scrollPane = new JScrollPane(rosterList);
        scrollPane.getViewport().setBackground(FIELD_PANEL_BACKGROUND);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel mapRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        mapRow.setBackground(BACKGROUND);
        mapRow.add(styledLabel("Map:"));
        mapComboBox.addActionListener(e -> {
            if (applyingRemoteMapState) {
                return;
            }
            String selected = (String) mapComboBox.getSelectedItem();
            if (selected != null) {
                sendQuietly(new SelectMapRequest(selected));
            }
        });
        mapRow.add(mapComboBox);
        panel.add(mapRow, BorderLayout.NORTH);

        readyButton.addActionListener(e -> toggleReady());
        startButton.addActionListener(e -> requestStart());
        startButton.setEnabled(false);
        loadButton.addActionListener(e -> sendQuietly(new LoadGameRequest()));
        loadButton.setEnabled(false);

        declareWarButton.setEnabled(false);
        declareWarButton.addActionListener(e -> declareWarOnSelected());
        rosterList.addListSelectionListener(e -> updateDeclareWarButtonState());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        controls.setBackground(BACKGROUND);
        controls.add(readyButton);
        controls.add(startButton);
        controls.add(loadButton);
        controls.add(declareWarButton);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BACKGROUND);
        south.add(controls, BorderLayout.CENTER);
        south.add(lobbyStatusLabel, BorderLayout.SOUTH);

        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStartedCard() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND);
        startedLabel.setFont(LABEL_FONT);
        startedLabel.setForeground(Color.WHITE);
        topPanel.add(startedLabel, BorderLayout.NORTH);
        topPanel.add(resourcesLabel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        boardPanel.setBackground(FIELD_PANEL_BACKGROUND);
        JScrollPane boardScroll = new JScrollPane(boardPanel);
        boardScroll.getViewport().setBackground(FIELD_PANEL_BACKGROUND);
        panel.add(boardScroll, BorderLayout.CENTER);

        actionsPanel.setPreferredSize(new Dimension(300, 0));
        panel.add(actionsPanel, BorderLayout.EAST);

        endTurnButton.setEnabled(false);
        endTurnButton.addActionListener(e -> sendQuietly(new EndTurnRequest()));
        boardPanel.setCombatLogListener(combatLogLabel::setText);

        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> sendQuietly(new SaveGameRequest()));
        tradeButton.addActionListener(e -> openTradeDialog());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        controls.setBackground(BACKGROUND);
        controls.add(turnLabel);
        controls.add(endTurnButton);
        controls.add(tradeButton);
        controls.add(saveButton);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BACKGROUND);
        south.add(controls, BorderLayout.CENTER);
        south.add(combatLogLabel, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
        panel.setPreferredSize(new Dimension(260, 0));

        JLabel chatTitle = styledLabel("Chat");
        panel.add(chatTitle, BorderLayout.NORTH);

        chatLog.setEditable(false);
        chatLog.setLineWrap(true);
        chatLog.setWrapStyleWord(true);
        chatLog.setBackground(FIELD_PANEL_BACKGROUND);
        chatLog.setForeground(Color.WHITE);
        chatLog.setFont(STATUS_FONT);
        JScrollPane chatScroll = new JScrollPane(chatLog);
        panel.add(chatScroll, BorderLayout.CENTER);

        chatInput.setEnabled(false);
        chatSendButton.setEnabled(false);
        chatSendButton.setPreferredSize(new Dimension(70, 30));
        chatInput.addActionListener(e -> sendChat());
        chatSendButton.addActionListener(e -> sendChat());

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(BACKGROUND);
        inputRow.add(chatInput, BorderLayout.CENTER);
        inputRow.add(chatSendButton, BorderLayout.EAST);
        panel.add(inputRow, BorderLayout.SOUTH);

        return panel;
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        sendQuietly(new ChatMessage(null, text));
        chatInput.setText("");
    }

    private void appendChatLine(ChatMessage chat) {
        chatLog.append(chat.getSenderName() + ": " + chat.getText() + "\n");
        chatLog.setCaretPosition(chatLog.getDocument().getLength());
    }

    private void setChatEnabled(boolean enabled) {
        chatInput.setEnabled(enabled);
        chatSendButton.setEnabled(enabled);
    }

    private static JLabel styledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(Color.WHITE);
        return label;
    }

    private static JLabel styledStatusLabel() {
        JLabel label = new JLabel(" ", SwingConstants.CENTER);
        label.setFont(STATUS_FONT);
        label.setForeground(Color.LIGHT_GRAY);
        return label;
    }

    private static JButton styledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(LABEL_FONT);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusable(false);
        button.setPreferredSize(BUTTON_SIZE);
        return button;
    }

    private void attemptConnect() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            connectStatusLabel.setText("Enter a name first.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            connectStatusLabel.setText("Port must be a number.");
            return;
        }

        client = new NetworkClient(new NetworkListener() {
            @Override
            public void onMessageReceived(Message message) {
                handleMessage(message);
            }

            @Override
            public void onDisconnected(String reason) {
                connectStatusLabel.setText("Disconnected: " + reason);
                setChatEnabled(false);
                innerLayout.show(innerContainer, "CONNECT");
            }
        });

        try {
            client.connect(host, port);
            client.send(new ConnectRequest(name));
            connectStatusLabel.setText("Connecting...");
        } catch (IOException e) {
            connectStatusLabel.setText("Connection failed: " + e.getMessage());
        }
    }

    private void toggleReady() {
        ready = !ready;
        readyButton.setText(ready ? "Not Ready" : "Ready Up");
        sendQuietly(new SetReadyRequest(ready));
    }

    private void requestStart() {
        sendQuietly(new StartGameRequest());
    }

    private void sendQuietly(Message message) {
        if (client == null) {
            return;
        }
        try {
            client.send(message);
        } catch (IOException e) {
            lobbyStatusLabel.setText("Send failed: " + e.getMessage());
        }
    }

    private void handleMessage(Message message) {
        if (message instanceof ConnectResponse response) {
            handleConnectResponse(response);
        } else if (message instanceof LobbyStateMessage state) {
            handleLobbyState(state);
        } else if (message instanceof GameStartedMessage) {
            startedLabel.setText("Game started on \"" + currentMapName + "\"!");
            saveButton.setEnabled(myPlayerId != null && myPlayerId.equals(hostPlayerId));
            innerLayout.show(innerContainer, "STARTED");
        } else if (message instanceof TurnChangedMessage turn) {
            handleTurnChanged(turn);
        } else if (message instanceof ChatMessage chat) {
            appendChatLine(chat);
        } else if (message instanceof DiplomacyChangedMessage diplomacy) {
            handleDiplomacyChanged(diplomacy);
        } else if (message instanceof GameStateSnapshotMessage snapshot) {
            boardPanel.setClient(client);
            boardPanel.loadSnapshot(snapshot, myPlayerId);
            applyEconomy(snapshot.getYourResources(), snapshot.getYourTechs(), snapshot.getYourItems());
        } else if (message instanceof UnitMovedMessage moved) {
            boardPanel.applyUnitMoved(moved);
        } else if (message instanceof CombatResultMessage combatResult) {
            boardPanel.applyCombatResult(combatResult);
        } else if (message instanceof ProductionStartedMessage production) {
            boardPanel.applyProductionStarted(production);
        } else if (message instanceof ProductionCompletedMessage production) {
            boardPanel.applyProductionCompleted(production);
        } else if (message instanceof BuildingConstructedMessage constructed) {
            boardPanel.applyBuildingConstructed(constructed);
        } else if (message instanceof UnitRemovedMessage removed) {
            boardPanel.applyUnitRemoved(removed);
        } else if (message instanceof DisasterOccurredMessage disaster) {
            boardPanel.applyDisaster(disaster);
        } else if (message instanceof PlayerEconomyMessage economy) {
            applyEconomy(economy.getResources(), economy.getTechs(), economy.getItems());
        } else if (message instanceof PlayerEliminatedMessage eliminated) {
            combatLogLabel.setText("Player " + eliminated.getPlayerId() + " has been eliminated.");
        } else if (message instanceof GameOverMessage gameOver) {
            endTurnButton.setEnabled(false);
            JOptionPane.showMessageDialog(this, gameOver.getWinnerName() + " has won the game!",
                    "Game Over", JOptionPane.INFORMATION_MESSAGE);
        } else if (message instanceof CheatAppliedMessage cheat) {
            combatLogLabel.setText(cheat.getCommand() + ": " + cheat.getDetail());
        } else if (message instanceof ItemUsedMessage itemUsed) {
            combatLogLabel.setText("Item used (" + itemUsed.getItemType() + "): " + itemUsed.getDetail());
        } else if (message instanceof TradeOfferReceivedMessage tradeOffer) {
            handleIncomingTradeOffer(tradeOffer);
        } else if (message instanceof TradeOfferResolvedMessage tradeResolved) {
            lobbyStatusLabel.setText("Trade offer " + (tradeResolved.isAccepted() ? "accepted." : "rejected."));
        } else if (message instanceof GameSavedMessage saved) {
            lobbyStatusLabel.setText(saved.getDetail());
        } else if (message instanceof ErrorMessage error) {
            lobbyStatusLabel.setText("Error: " + error.getErrorText());
        }
    }

    private void applyEconomy(Map<String, Integer> resources, List<String> techs, Map<String, Integer> items) {
        actionsPanel.setEconomy(resources, techs, items);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        resourcesLabel.setText(sb.toString());
    }

    private void openTradeDialog() {
        List<LobbyStateMessage.PlayerEntry> candidates = new ArrayList<>();
        for (int i = 0; i < rosterModel.size(); i++) {
            LobbyStateMessage.PlayerEntry entry = rosterModel.get(i);
            if (!entry.playerId().equals(myPlayerId)) {
                candidates.add(entry);
            }
        }
        if (candidates.isEmpty()) {
            lobbyStatusLabel.setText("No other players to trade with.");
            return;
        }

        JComboBox<LobbyStateMessage.PlayerEntry> targetCombo = new JComboBox<>(candidates.toArray(new LobbyStateMessage.PlayerEntry[0]));
        targetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LobbyStateMessage.PlayerEntry entry) {
                    label.setText(entry.playerName());
                }
                return label;
            }
        });

        JComboBox<ResourceType> offerResourceCombo = new JComboBox<>(TRADABLE_RESOURCES);
        JTextField offerAmountField = new JTextField("50", 5);
        JComboBox<ResourceType> requestResourceCombo = new JComboBox<>(TRADABLE_RESOURCES);
        requestResourceCombo.setSelectedIndex(1);
        JTextField requestAmountField = new JTextField("50", 5);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Trade with:"));
        form.add(targetCombo);
        form.add(new JLabel("You give:"));
        form.add(offerResourceCombo);
        form.add(new JLabel("Amount:"));
        form.add(offerAmountField);
        form.add(new JLabel("You get:"));
        form.add(requestResourceCombo);
        form.add(new JLabel("Amount:"));
        form.add(requestAmountField);

        int result = JOptionPane.showConfirmDialog(this, form, "Propose Trade", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        LobbyStateMessage.PlayerEntry target = (LobbyStateMessage.PlayerEntry) targetCombo.getSelectedItem();
        ResourceType offerResource = (ResourceType) offerResourceCombo.getSelectedItem();
        ResourceType requestResource = (ResourceType) requestResourceCombo.getSelectedItem();
        int offerAmount;
        int requestAmount;
        try {
            offerAmount = Integer.parseInt(offerAmountField.getText().trim());
            requestAmount = Integer.parseInt(requestAmountField.getText().trim());
        } catch (NumberFormatException e) {
            lobbyStatusLabel.setText("Amounts must be numbers.");
            return;
        }
        if (target == null || offerResource == null || requestResource == null || offerAmount <= 0 || requestAmount <= 0) {
            return;
        }

        Map<String, Integer> offering = new HashMap<>();
        offering.put(offerResource.name(), offerAmount);
        Map<String, Integer> requesting = new HashMap<>();
        requesting.put(requestResource.name(), requestAmount);
        sendQuietly(new TradeOfferRequest(target.playerId(), offering, requesting));
    }

    private void handleIncomingTradeOffer(TradeOfferReceivedMessage offer) {
        String text = offer.getFromPlayerName() + " offers you " + describeResourceMap(offer.getOffering())
                + " for " + describeResourceMap(offer.getRequesting()) + ". Accept?";
        int result = JOptionPane.showConfirmDialog(this, text, "Trade Offer", JOptionPane.YES_NO_OPTION);
        sendQuietly(new TradeRespondRequest(offer.getOfferId(), result == JOptionPane.YES_OPTION));
    }

    private String describeResourceMap(Map<String, Integer> resources) {
        if (resources.isEmpty()) {
            return "nothing";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append(" ").append(entry.getKey());
        }
        return sb.toString();
    }

    private void handleConnectResponse(ConnectResponse response) {
        if (!response.isAccepted()) {
            connectStatusLabel.setText("Rejected: " + response.getErrorMessage());
            client.disconnect();
            client = null;
            return;
        }
        myPlayerId = response.getPlayerId();
        ready = false;
        readyButton.setText("Ready Up");
        setChatEnabled(true);
        client.startHeartbeat(myPlayerId);
        innerLayout.show(innerContainer, "ROSTER");
    }

    private void handleLobbyState(LobbyStateMessage state) {
        hostPlayerId = state.getHostPlayerId();
        rosterModel.clear();
        boolean allReady = !state.getPlayers().isEmpty();
        for (LobbyStateMessage.PlayerEntry entry : state.getPlayers()) {
            rosterModel.addElement(entry);
            if (!entry.ready()) {
                allReady = false;
            }
        }
        boolean isHost = myPlayerId != null && myPlayerId.equals(hostPlayerId);
        startButton.setEnabled(isHost && allReady);
        loadButton.setEnabled(isHost);
        lobbyStatusLabel.setText(isHost ? "You are the host." : "Waiting for host to start.");
        updateDeclareWarButtonState();

        currentMapName = state.getSelectedMapName();
        applyingRemoteMapState = true;
        mapComboBox.setSelectedItem(currentMapName);
        applyingRemoteMapState = false;
        mapComboBox.setEnabled(isHost);
    }

    private void declareWarOnSelected() {
        LobbyStateMessage.PlayerEntry selected = rosterList.getSelectedValue();
        if (selected != null) {
            sendQuietly(new DeclareWarRequest(selected.playerId()));
        }
    }

    private void updateDeclareWarButtonState() {
        LobbyStateMessage.PlayerEntry selected = rosterList.getSelectedValue();
        boolean canDeclare = selected != null && myPlayerId != null
                && !selected.playerId().equals(myPlayerId)
                && !myEnemyIds.contains(selected.playerId());
        declareWarButton.setEnabled(canDeclare);
    }

    private void handleDiplomacyChanged(DiplomacyChangedMessage diplomacy) {
        if (myPlayerId != null) {
            if (myPlayerId.equals(diplomacy.getPlayerAId())) {
                myEnemyIds.add(diplomacy.getPlayerBId());
            } else if (myPlayerId.equals(diplomacy.getPlayerBId())) {
                myEnemyIds.add(diplomacy.getPlayerAId());
            }
        }
        lobbyStatusLabel.setText(diplomacy.getPlayerAName() + " declared war on " + diplomacy.getPlayerBName() + "!");
        rosterList.repaint();
        updateDeclareWarButtonState();
    }

    private void handleTurnChanged(TurnChangedMessage turn) {
        currentTurnPlayerId = turn.getCurrentPlayerId();
        boolean myTurn = myPlayerId != null && myPlayerId.equals(currentTurnPlayerId);
        turnLabel.setText(myTurn ? "Your turn!" : "Current turn: " + turn.getCurrentPlayerName());
        endTurnButton.setEnabled(myTurn);
        boardPanel.setCurrentTurnPlayerId(currentTurnPlayerId);
    }

    private void disconnectQuietly() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
        myPlayerId = null;
        hostPlayerId = null;
        currentTurnPlayerId = null;
        ready = false;
        rosterModel.clear();
        myEnemyIds.clear();
        declareWarButton.setEnabled(false);
        mapComboBox.setEnabled(false);
        loadButton.setEnabled(false);
        saveButton.setEnabled(false);
        currentMapName = null;
        startedLabel.setText("");
        resourcesLabel.setText(" ");
        connectStatusLabel.setText(" ");
        turnLabel.setText(" ");
        combatLogLabel.setText(" ");
        endTurnButton.setEnabled(false);
        setChatEnabled(false);
        chatLog.setText("");
        boardPanel.reset();
        actionsPanel.setEconomy(new HashMap<>(), List.of(), new HashMap<>());
        innerLayout.show(innerContainer, "CONNECT");
    }

    private class RosterCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof LobbyStateMessage.PlayerEntry entry) {
                boolean isHost = entry.playerId().equals(hostPlayerId);
                String hostTag = isHost ? " <font color='#F1C40F'>&#9733; HOST</font>" : "";
                String enemyTag = myEnemyIds.contains(entry.playerId()) ? " <font color='#E74C3C'>(ENEMY)</font>" : "";
                String readyColor = entry.ready() ? "#2ECC71" : "#95A5A6";
                String readyText = entry.ready() ? "Ready" : "Not Ready";
                label.setText("<html><b>" + entry.playerName() + "</b>" + hostTag + enemyTag
                        + " &mdash; <font color='" + readyColor + "'>" + readyText + "</font></html>");
            }
            label.setOpaque(true);
            if (!isSelected) {
                label.setBackground(FIELD_PANEL_BACKGROUND);
            }
            return label;
        }
    }
}
