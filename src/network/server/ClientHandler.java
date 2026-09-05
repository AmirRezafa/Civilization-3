package network.server;

import network.protocol.ConnectRequest;
import network.protocol.ConnectResponse;
import network.protocol.EndTurnRequest;
import network.protocol.ErrorMessage;
import network.protocol.Message;
import network.protocol.MessageCodec;
import network.protocol.PlayerJoinedMessage;
import network.protocol.SetReadyRequest;
import network.protocol.StartGameRequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

class ClientHandler implements Runnable {
    private final GameServer server;
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final Object writeLock = new Object();
    private volatile String playerId;
    private volatile String playerName = "";
    private volatile boolean closed;
    private volatile boolean ready;

    ClientHandler(GameServer server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException ignored) {
        } finally {
            close();
            if (playerId != null) {
                server.unregisterClient(playerId);
            }
        }
    }

    private void handleLine(String line) {
        Message message;
        try {
            message = MessageCodec.decode(line);
        } catch (RuntimeException e) {
            send(new ErrorMessage("Malformed message"));
            return;
        }

        if (message instanceof ConnectRequest request) {
            handleConnectRequest(request);
        } else if (message instanceof SetReadyRequest request) {
            handleSetReadyRequest(request);
        } else if (message instanceof StartGameRequest) {
            handleStartGameRequest();
        } else if (message instanceof EndTurnRequest) {
            handleEndTurnRequest();
        } else {
            send(new ErrorMessage("Unsupported message type: " + message.getType()));
        }
    }

    private void handleEndTurnRequest() {
        if (playerId == null) {
            send(new ErrorMessage("Connect before ending a turn"));
            return;
        }
        server.handleEndTurnRequest(playerId);
    }

    private void handleSetReadyRequest(SetReadyRequest request) {
        if (playerId == null) {
            send(new ErrorMessage("Connect before setting ready state"));
            return;
        }
        server.setReady(playerId, request.isReady());
    }

    private void handleStartGameRequest() {
        if (playerId == null) {
            send(new ErrorMessage("Connect before starting the game"));
            return;
        }
        server.handleStartGameRequest(playerId);
    }

    private void handleConnectRequest(ConnectRequest request) {
        if (playerId != null) {
            send(new ErrorMessage("Already connected"));
            return;
        }
        String trimmedName = request.getPlayerName() == null ? "" : request.getPlayerName().trim();
        if (trimmedName.isEmpty()) {
            send(new ConnectResponse(false, null, "Player name cannot be empty", List.of()));
            return;
        }
        List<String> existingNames = server.getConnectedPlayerNames();
        String assignedId = server.registerClient(trimmedName, this);
        if (assignedId == null) {
            send(new ConnectResponse(false, null, "Name already taken", List.of()));
            return;
        }
        this.playerName = trimmedName;
        this.playerId = assignedId;
        send(new ConnectResponse(true, assignedId, null, existingNames));
        server.broadcast(new PlayerJoinedMessage(assignedId, trimmedName), assignedId);
        server.broadcastLobbyState();
    }

    void send(Message message) {
        String json = MessageCodec.encode(message);
        synchronized (writeLock) {
            try {
                out.write(json);
                out.newLine();
                out.flush();
            } catch (IOException e) {
                close();
            }
        }
    }

    void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    String getPlayerName() {
        return playerName;
    }

    String getPlayerId() {
        return playerId;
    }

    boolean isReady() {
        return ready;
    }

    void setReady(boolean ready) {
        this.ready = ready;
    }
}
