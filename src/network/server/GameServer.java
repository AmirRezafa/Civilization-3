package network.server;

import model.Building;
import model.HexUtils;
import model.ResourceType;
import model.Tile;
import model.Unit;
import network.protocol.DiplomacyChangedMessage;
import network.protocol.ErrorMessage;
import network.protocol.GameStartedMessage;
import network.protocol.GameStateSnapshotMessage;
import network.protocol.LobbyStateMessage;
import network.protocol.Message;
import network.protocol.PlayerLeftMessage;
import network.protocol.TurnChangedMessage;
import network.protocol.UnitMovedMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameServer {
    private final int port;
    private final ConcurrentHashMap<String, ClientHandler> clientsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastHeartbeatAt = new ConcurrentHashMap<>();
    private final AtomicInteger playerIdSequence = new AtomicInteger(1);
    private ServerSocket serverSocket;
    private DatagramSocket udpSocket;
    private Thread acceptThread;
    private Thread udpListenThread;
    private volatile boolean running;
    private volatile String hostPlayerId;
    private final List<String> turnOrder = new CopyOnWriteArrayList<>();
    private volatile int currentTurnIndex = -1;
    private volatile boolean gameStarted;
    private final Map<String, Set<String>> enemyMap = new ConcurrentHashMap<>();
    private static final List<String> MAP_PRESETS = List.of("Grasslands Valley", "Highland Frontier", "Coastal Reach");
    private volatile String selectedMapName = MAP_PRESETS.get(0);
    private volatile GameWorld world;

    public GameServer(int port) {
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        udpSocket = new DatagramSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "game-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        udpListenThread = new Thread(this::udpListenLoop, "game-server-udp-heartbeat");
        udpListenThread.setDaemon(true);
        udpListenThread.start();
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

    private void udpListenLoop() {
        byte[] buffer = new byte[256];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String playerId = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                lastHeartbeatAt.put(playerId, System.currentTimeMillis());
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
        lastHeartbeatAt.clear();
        gameStarted = false;
        turnOrder.clear();
        currentTurnIndex = -1;
        world = null;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (udpSocket != null) {
            udpSocket.close();
        }
    }

    public Long getLastHeartbeat(String playerId) {
        return lastHeartbeatAt.get(playerId);
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
        lastHeartbeatAt.remove(playerId);
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
        broadcast(new LobbyStateMessage(entries, hostPlayerId, selectedMapName), null);
    }

    void handleSelectMapRequest(String requesterId, String mapName) {
        if (!requesterId.equals(hostPlayerId)) {
            sendTo(requesterId, new ErrorMessage("Only the host can select the map"));
            return;
        }
        if (mapName == null || !MAP_PRESETS.contains(mapName)) {
            sendTo(requesterId, new ErrorMessage("Unknown map"));
            return;
        }
        selectedMapName = mapName;
        broadcastLobbyState();
    }

    public List<String> getMapPresets() {
        return MAP_PRESETS;
    }

    void setReady(String playerId, boolean ready) {
        ClientHandler handler = clientsById.get(playerId);
        if (handler == null) {
            return;
        }
        handler.setReady(ready);
        broadcastLobbyState();
    }

    synchronized void handleStartGameRequest(String requesterId) {
        if (gameStarted) {
            sendTo(requesterId, new ErrorMessage("Game has already started"));
            return;
        }
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
        world = GameWorld.generate(orderedIds);
        broadcast(new GameStartedMessage(), null);
        GameStateSnapshotMessage snapshot = buildSnapshotMessage();
        for (String playerId : orderedIds) {
            sendTo(playerId, snapshot);
        }
        broadcastTurnState();
    }

    private GameStateSnapshotMessage buildSnapshotMessage() {
        List<GameStateSnapshotMessage.TileEntry> tileEntries = new ArrayList<>();
        for (Tile tile : world.getTiles()) {
            Map<String, Integer> resources = new LinkedHashMap<>();
            for (Map.Entry<ResourceType, Integer> entry : tile.getResources().entrySet()) {
                resources.put(entry.getKey().name(), entry.getValue());
            }
            tileEntries.add(new GameStateSnapshotMessage.TileEntry(tile.getCol(), tile.getRow(), tile.getTerrain().name(), resources));
        }

        List<GameStateSnapshotMessage.UnitEntry> unitEntries = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : world.getUnits()) {
            Unit unit = networkUnit.unit;
            unitEntries.add(new GameStateSnapshotMessage.UnitEntry(networkUnit.id, networkUnit.ownerId,
                    unit.getType().name(), unit.getCol(), unit.getRow(), unit.getCurrentAP()));
        }

        List<GameStateSnapshotMessage.BuildingEntry> buildingEntries = new ArrayList<>();
        for (GameWorld.NetworkBuilding networkBuilding : world.getBuildings()) {
            Building building = networkBuilding.building;
            buildingEntries.add(new GameStateSnapshotMessage.BuildingEntry(networkBuilding.id, networkBuilding.ownerId,
                    building.getType().name(), building.getCol(), building.getRow()));
        }

        return new GameStateSnapshotMessage(world.getRows(), world.getCols(), tileEntries, unitEntries, buildingEntries);
    }

    void handleMoveUnitRequest(String requesterId, int unitId, int targetCol, int targetRow) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return;
        }
        GameWorld.NetworkUnit networkUnit = world.getUnitById(unitId);
        if (networkUnit == null) {
            sendTo(requesterId, new ErrorMessage("Unit not found"));
            return;
        }
        if (!requesterId.equals(networkUnit.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this unit"));
            return;
        }
        if (targetCol < 0 || targetCol >= world.getCols() || targetRow < 0 || targetRow >= world.getRows()) {
            sendTo(requesterId, new ErrorMessage("Target tile is out of bounds"));
            return;
        }
        Unit unit = networkUnit.unit;
        if (!HexUtils.isNeighbor(unit.getCol(), unit.getRow(), targetCol, targetRow)) {
            sendTo(requesterId, new ErrorMessage("Target tile is not adjacent"));
            return;
        }
        Tile targetTile = world.getTile(targetCol, targetRow);
        if (!targetTile.getTerrain().isPassable()) {
            sendTo(requesterId, new ErrorMessage("Target terrain is not passable"));
            return;
        }
        int movementCost = targetTile.getTerrain().getMovementCost();
        if (unit.getCurrentAP() < movementCost) {
            sendTo(requesterId, new ErrorMessage("Not enough action points"));
            return;
        }
        boolean moved = unit.move(targetCol, targetRow, movementCost);
        if (!moved) {
            sendTo(requesterId, new ErrorMessage("Move failed"));
            return;
        }
        broadcast(new UnitMovedMessage(unitId, targetCol, targetRow, unit.getCurrentAP()), null);
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

    void handleDeclareWarRequest(String requesterId, String targetId) {
        if (targetId == null || !clientsById.containsKey(targetId)) {
            sendTo(requesterId, new ErrorMessage("Target player not found"));
            return;
        }
        if (targetId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("You cannot declare war on yourself"));
            return;
        }
        if (isEnemy(requesterId, targetId)) {
            sendTo(requesterId, new ErrorMessage("Already at war with this player"));
            return;
        }
        enemyMap.computeIfAbsent(requesterId, k -> ConcurrentHashMap.newKeySet()).add(targetId);
        enemyMap.computeIfAbsent(targetId, k -> ConcurrentHashMap.newKeySet()).add(requesterId);
        ClientHandler a = clientsById.get(requesterId);
        ClientHandler b = clientsById.get(targetId);
        broadcast(new DiplomacyChangedMessage(requesterId, a.getPlayerName(), targetId, b.getPlayerName(), "ENEMY"), null);
    }

    private boolean isEnemy(String a, String b) {
        Set<String> set = enemyMap.get(a);
        return set != null && set.contains(b);
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
