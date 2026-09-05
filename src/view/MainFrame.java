package view;

import controller.GameController;
import view.Panels.GameControlPanel;
import view.Panels.LobbyPanel;
import view.Panels.MainMenuPanel;
import view.Panels.SettingPanel;
import view.Panels.UnitActionPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainCardContainer;

    private JLayeredPane layeredPane;
    private Ground ground;
    private GameController gc;
    private GameControlPanel GCP;
    private UnitActionPanel actionPanel;

    private MainMenuPanel mainMenuPanel;

    public MainFrame(){
        super("Civilization");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainCardContainer = new JPanel(cardLayout);

        mainMenuPanel = new MainMenuPanel(this);
        mainCardContainer.add(mainMenuPanel, "MENU");

        SettingPanel settingPanel = new SettingPanel(this);
        mainCardContainer.add(settingPanel, "SETTINGS");

        LobbyPanel lobbyPanel = new LobbyPanel(this);
        mainCardContainer.add(lobbyPanel, "LOBBY");

        JPanel gameContainer = new JPanel(new BorderLayout());

        ground = new Ground();
        gc = new GameController(ground);
        ground.setController(gc);
        GCP = new GameControlPanel(gc);
        actionPanel = new UnitActionPanel(gc);
        layeredPane = new JLayeredPane();

        layeredPane.add(ground, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(actionPanel, JLayeredPane.PALETTE_LAYER);

        gameContainer.add(GCP, BorderLayout.NORTH);
        gameContainer.add(layeredPane, BorderLayout.CENTER);

        mainCardContainer.add(gameContainer, "GAME");

        this.setLayout(new BorderLayout());
        this.add(mainCardContainer, BorderLayout.CENTER);

        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateLayeredLayoutBounds();
            }
        });

        showMenu();

        setLocationRelativeTo(null);
        setVisible(true);

        controller.AudioManager.getInstance().playBGM("assets/bgm.wav");
    }

    public void showGame() {
        cardLayout.show(mainCardContainer, "GAME");
    }

    public GameController getGameController() {
        return gc;
    }

    public boolean loadGameAndShow(int slot) {
        boolean success = gc.loadGame(slot);
        if (success) {
            GCP.refreshAfterExternalLoad();
            showGame();
        }
        return success;
    }

    public void showMenu() {
        cardLayout.show(mainCardContainer, "MENU");
    }

    public void showSettings() {
        cardLayout.show(mainCardContainer, "SETTINGS");
    }

    public void showLobby() {
        cardLayout.show(mainCardContainer, "LOBBY");
    }

    private void updateLayeredLayoutBounds() {
        int paneWidth = layeredPane.getWidth();
        int paneHeight = layeredPane.getHeight();

        ground.setBounds(0, 0, paneWidth, paneHeight);

        int actionHeight = (int) (gc.getB() * 3.4);
        int actionY = paneHeight - actionHeight;
        actionPanel.setBounds(0, actionY, paneWidth, actionHeight);

        actionPanel.updateActions();
        layeredPane.revalidate();
        layeredPane.repaint();
    }
}
