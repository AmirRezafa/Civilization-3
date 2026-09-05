package network.server;

import network.protocol.ErrorMessage;
import network.protocol.GameStartedMessage;
import network.protocol.LobbyStateMessage;
import network.protocol.Message;
import network.protocol.PlayerLeftMessage;
import network.protocol.TurnChangedMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private final int port;
    private final ConcurrentHashMap<String, ClientHandler> clientsById = new ConcurrentHashMap<>();
    private final AtomicInteger playerIdSequence = new AtomicInteger(1);
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;
    private volatile String hostPlayerId;
    private final List<String> turnOrder = new CopyOnWriteArrayList<>();
    private volatile int currentTurnIndex = -1;
    private volatile boolean gameStarted;

    public GameServer(int port) {
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "game-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(this, socket);
                Thread thread = new Thread(handler, "client-handler-" + socket.getRemoteSocketAddress());
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                running = false;
            }
        }
    }

    public synchronized void stop() {
        running = false;
        for (ClientHandler handler : clientsById.values()) {
            handler.close();
        }
        clientsById.clear();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    synchronized String registerClient(String desiredName, ClientHandler handler) {
        for (ClientHandler existing : clientsById.values()) {
            if (existing.getPlayerName().equalsIgnoreCase(desiredName)) {
                return null;
            }
        }
        String playerId = "P" + playerIdSequence.getAndIncrement();
        clientsById.put(playerId, handler);
        if (hostPlayerId == null) {
            hostPlayerId = playerId;
        }
        return playerId;
    }

    synchronized void unregisterClient(String playerId) {
        ClientHandler removed = clientsById.remove(playerId);
        if (removed == null) {
            return;
        }
        if (playerId.equals(hostPlayerId)) {
            hostPlayerId = clientsById.isEmpty() ? null : clientsById.keySet().iterator().next();
        }
        broadcast(new PlayerLeftMessage(playerId, removed.getPlayerName()), playerId);
        handleDisconnectTurnImpact(playerId);
        broadcastLobbyState();
    }

    void broadcast(Message message, String excludePlayerId) {
        for (Map.Entry<String, ClientHandler> entry : clientsById.entrySet()) {
            if (excludePlayerId != null && excludePlayerId.equals(entry.getKey())) {
                continue;
            }
            entry.getValue().send(message);
        }
    }

    void sendTo(String playerId, Message message) {
        if (playerId == null) {
            return;
        }
        ClientHandler handler = clientsById.get(playerId);
        if (handler != null) {
            handler.send(message);
        }
    }

    void broadcastLobbyState() {
        List<LobbyStateMessage.PlayerEntry> entries = new ArrayList<>();
        for (Map.Entry<String, ClientHandler> entry : clientsById.entrySet()) {
            ClientHandler handler = entry.getValue();
            entries.add(new LobbyStateMessage.PlayerEntry(entry.getKey(), handler.getPlayerName(), handler.isReady()));
        }
        broadcast(new LobbyStateMessage(entries, hostPlayerId), null);
    }

    void setReady(String playerId, boolean ready) {
        ClientHandler handler = clientsById.get(playerId);
        if (handler == null) {
            return;
        }
        handler.setReady(ready);
        broadcastLobbyState();
    }

    void handleStartGameRequest(String requesterId) {
        if (!requesterId.equals(hostPlayerId)) {
            sendTo(requesterId, new ErrorMessage("Only the host can start the game"));
            return;
        }
        for (ClientHandler handler : clientsById.values()) {
            if (!handler.isReady()) {
                sendTo(requesterId, new ErrorMessage("All players must be ready"));
                return;
            }
        }
        gameStarted = true;
        List<String> orderedIds = new ArrayList<>(clientsById.keySet());
        orderedIds.sort(Comparator.comparingInt(id -> Integer.parseInt(id.substring(1))));
        turnOrder.clear();
        turnOrder.addAll(orderedIds);
        currentTurnIndex = turnOrder.isEmpty() ? -1 : 0;
        broadcast(new GameStartedMessage(), null);
        broadcastTurnState();
    }

    void handleEndTurnRequest(String requesterId) {
        if (!gameStarted) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String currentId = getCurrentTurnPlayerId();
        if (currentId == null || !currentId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return;
        }
        advanceTurn();
    }

    private void handleDisconnectTurnImpact(String playerId) {
        if (!gameStarted) {
            return;
        }
        int index = turnOrder.indexOf(playerId);
        if (index == -1) {
            return;
        }
        boolean wasCurrentTurn = index == currentTurnIndex;
        turnOrder.remove(index);
        if (turnOrder.isEmpty()) {
            currentTurnIndex = -1;
            return;
        }
        if (index < currentTurnIndex) {
            currentTurnIndex--;
        } else if (wasCurrentTurn && currentTurnIndex >= turnOrder.size()) {
            currentTurnIndex = 0;
        }
        if (wasCurrentTurn) {
            broadcastTurnState();
        }
    }

    private void advanceTurn() {
        if (turnOrder.isEmpty()) {
            return;
        }
        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
        broadcastTurnState();
    }

    private String getCurrentTurnPlayerId() {
        if (turnOrder.isEmpty() || currentTurnIndex < 0 || currentTurnIndex >= turnOrder.size()) {
            return null;
        }
        return turnOrder.get(currentTurnIndex);
    }

    private void broadcastTurnState() {
        String currentId = getCurrentTurnPlayerId();
        if (currentId == null) {
            return;
        }
        ClientHandler handler = clientsById.get(currentId);
        String currentName = handler != null ? handler.getPlayerName() : "";
        broadcast(new TurnChangedMessage(currentId, currentName), null);
    }

    List<String> getConnectedPlayerNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler handler : clientsById.values()) {
            names.add(handler.getPlayerName());
        }
        return names;
    }

    public Collection<String> getConnectedPlayerIds() {
        return clientsById.keySet();
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }
}
