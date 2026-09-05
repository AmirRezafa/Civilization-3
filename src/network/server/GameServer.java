package network.server;

import network.protocol.ErrorMessage;
import network.protocol.GameStartedMessage;
import network.protocol.LobbyStateMessage;
import network.protocol.Message;
import network.protocol.PlayerLeftMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private final int port;
    private final ConcurrentHashMap<String, ClientHandler> clientsById = new ConcurrentHashMap<>();
    private final AtomicInteger playerIdSequence = new AtomicInteger(1);
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;
    private volatile String hostPlayerId;

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
        broadcast(new GameStartedMessage(), null);
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
