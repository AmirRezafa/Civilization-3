package network.server;

import controller.events.CombatResultEvent;
import controller.events.EventBus;
import controller.services.CombatService;
import model.Building;
import model.BuildingType;
import model.GlobalResourceManager;
import model.HexUtils;
import model.ResourceType;
import model.TechType;
import model.Tile;
import model.TownHallLevel;
import model.Unit;
import model.UnitType;
import network.protocol.CombatResultMessage;
import network.protocol.DiplomacyChangedMessage;
import network.protocol.ErrorMessage;
import network.protocol.GameStartedMessage;
import network.protocol.GameStateSnapshotMessage;
import network.protocol.LobbyStateMessage;
import network.protocol.Message;
import network.protocol.PlayerLeftMessage;
import network.protocol.ProductionCompletedMessage;
import network.protocol.ProductionStartedMessage;
import network.protocol.TurnChangedMessage;
import network.protocol.UnitMovedMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.HashSet;
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
    private final List<String> allPlayerIds = new ArrayList<>();
    private final Set<String> eliminatedPlayerIds = ConcurrentHashMap.newKeySet();
    private final Set<String> fogRevealedPlayerIds = ConcurrentHashMap.newKeySet();
    private final Set<Integer> combatBoostedUnitIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, TradeOffer> pendingTradeOffers = new ConcurrentHashMap<>();
    private final AtomicInteger nextTradeOfferId = new AtomicInteger(1);
    private volatile CombatResultEvent lastCombatEvent;

    public GameServer(int port) {
        this.port = port;
        EventBus.subscribe(CombatResultEvent.class, e -> lastCombatEvent = e);
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
        allPlayerIds.clear();
        eliminatedPlayerIds.clear();
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
        allPlayerIds.clear();
        allPlayerIds.addAll(orderedIds);
        eliminatedPlayerIds.clear();
        world = GameWorld.generate(orderedIds, selectedMapName);
        broadcast(new GameStartedMessage(), null);
        for (String playerId : orderedIds) {
            sendTo(playerId, buildSnapshotMessage(playerId));
        }
        broadcastTurnState();
    }

    private GameStateSnapshotMessage buildSnapshotMessage(String recipientId) {
        List<GameStateSnapshotMessage.TileEntry> tileEntries = new ArrayList<>();
        for (Tile tile : world.getTiles()) {
            boolean currentlyVisible = isVisibleToPlayer(recipientId, tile.getCol(), tile.getRow());
            if (currentlyVisible) {
                world.markExplored(recipientId, tile.getCol(), tile.getRow());
            } else if (!world.isExplored(recipientId, tile.getCol(), tile.getRow())) {
                continue;
            }
            Map<String, Integer> resources = new LinkedHashMap<>();
            for (Map.Entry<ResourceType, Integer> entry : tile.getResources().entrySet()) {
                resources.put(entry.getKey().name(), entry.getValue());
            }
            tileEntries.add(new GameStateSnapshotMessage.TileEntry(tile.getCol(), tile.getRow(), tile.getTerrain().name(),
                    resources, currentlyVisible));
        }

        List<GameStateSnapshotMessage.UnitEntry> unitEntries = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : world.getUnits()) {
            Unit unit = networkUnit.unit;
            if (!recipientId.equals(networkUnit.ownerId) && !isVisibleToPlayer(recipientId, unit.getCol(), unit.getRow())) {
                continue;
            }
            unitEntries.add(new GameStateSnapshotMessage.UnitEntry(networkUnit.id, networkUnit.ownerId,
                    unit.getType().name(), unit.getCol(), unit.getRow(), unit.getCurrentAP(), unit.getHP()));
        }

        List<GameStateSnapshotMessage.BuildingEntry> buildingEntries = new ArrayList<>();
        for (GameWorld.NetworkBuilding networkBuilding : world.getBuildings()) {
            Building building = networkBuilding.building;
            if (!recipientId.equals(networkBuilding.ownerId) && !isVisibleToPlayer(recipientId, building.getCol(), building.getRow())) {
                continue;
            }
            String townHallLevel = building.getType() == BuildingType.TOWN_HALL ? building.getTownHallLevel().name() : null;
            String producingKind = null;
            String producingTarget = null;
            if (building.getProducingUnit() != null) {
                producingKind = "UNIT";
                producingTarget = building.getProducingUnit().name();
            } else if (building.getUpgradingToLevel() != null) {
                producingKind = "UPGRADE";
                producingTarget = building.getUpgradingToLevel().name();
            } else if (building.getResearchingTech() != null) {
                producingKind = "TECH";
                producingTarget = building.getResearchingTech().name();
            }
            buildingEntries.add(new GameStateSnapshotMessage.BuildingEntry(networkBuilding.id, networkBuilding.ownerId,
                    building.getType().name(), building.getCol(), building.getRow(), building.getHP(), building.getMaxHP(),
                    townHallLevel, producingKind, producingTarget, building.getProductionTurnsLeft()));
        }

        Map<String, Integer> yourResources = new LinkedHashMap<>();
        List<String> yourTechs = new ArrayList<>();
        Map<String, Integer> yourItems = new LinkedHashMap<>();
        GameWorld.PlayerEconomy economy = world.getEconomy(recipientId);
        if (economy != null) {
            for (ResourceType type : ResourceType.values()) {
                if (type == ResourceType.NONE) continue;
                yourResources.put(type.name(), economy.resources.getResourceAmount(type));
            }
            for (Map.Entry<TechType, Boolean> entry : economy.researchedTechs.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    yourTechs.add(entry.getKey().name());
                }
            }
            for (network.protocol.ItemType type : network.protocol.ItemType.values()) {
                yourItems.put(type.name(), economy.getItemCount(type));
            }
        }

        return new GameStateSnapshotMessage(world.getRows(), world.getCols(), tileEntries, unitEntries, buildingEntries,
                yourResources, yourTechs, yourItems);
    }

    private boolean isVisibleToPlayer(String playerId, int col, int row) {
        if (fogRevealedPlayerIds.contains(playerId)) {
            return true;
        }
        for (GameWorld.NetworkUnit networkUnit : world.getUnitsOwnedBy(playerId)) {
            int radius = networkUnit.unit.getType().getVisionRadius();
            int dc = networkUnit.unit.getCol() - col;
            int dr = networkUnit.unit.getRow() - row;
            if (dc * dc + dr * dr <= radius * radius) {
                return true;
            }
        }
        for (GameWorld.NetworkBuilding networkBuilding : world.getBuildingsOwnedBy(playerId)) {
            int radius = networkBuilding.building.getType().getVisionRadius();
            int dc = networkBuilding.building.getCol() - col;
            int dr = networkBuilding.building.getRow() - row;
            if (dc * dc + dr * dr <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private void sendEconomyUpdate(String playerId) {
        GameWorld.PlayerEconomy economy = world != null ? world.getEconomy(playerId) : null;
        if (economy == null) {
            return;
        }
        Map<String, Integer> resources = new LinkedHashMap<>();
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.NONE) continue;
            resources.put(type.name(), economy.resources.getResourceAmount(type));
        }
        List<String> techs = new ArrayList<>();
        for (Map.Entry<TechType, Boolean> entry : economy.researchedTechs.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                techs.add(entry.getKey().name());
            }
        }
        Map<String, Integer> items = new LinkedHashMap<>();
        for (network.protocol.ItemType type : network.protocol.ItemType.values()) {
            items.put(type.name(), economy.getItemCount(type));
        }
        sendTo(playerId, new network.protocol.PlayerEconomyMessage(resources, techs, items));
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
        sendTo(requesterId, buildSnapshotMessage(requesterId));
    }

    synchronized void handleAttackRequest(String requesterId, int attackerUnitId, int targetCol, int targetRow) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return;
        }
        GameWorld.NetworkUnit anchor = world.getUnitById(attackerUnitId);
        if (anchor == null) {
            sendTo(requesterId, new ErrorMessage("Unit not found"));
            return;
        }
        if (!requesterId.equals(anchor.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this unit"));
            return;
        }
        if (targetCol < 0 || targetCol >= world.getCols() || targetRow < 0 || targetRow >= world.getRows()) {
            sendTo(requesterId, new ErrorMessage("Target tile is out of bounds"));
            return;
        }
        if (anchor.unit.getCurrentAP() < 1) {
            sendTo(requesterId, new ErrorMessage("Not enough action points"));
            return;
        }

        int anchorCol = anchor.unit.getCol();
        int anchorRow = anchor.unit.getRow();
        boolean isAdjacent = HexUtils.isNeighbor(anchorCol, anchorRow, targetCol, targetRow);

        List<GameWorld.NetworkUnit> attackingStack = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : world.getUnitsAt(anchorCol, anchorRow)) {
            if (requesterId.equals(networkUnit.ownerId)) {
                attackingStack.add(networkUnit);
            }
        }
        List<Unit> attackerUnits = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : attackingStack) {
            attackerUnits.add(networkUnit.unit);
        }

        boolean hasArcher = false;
        for (Unit u : attackerUnits) {
            if (u.getType() == UnitType.ARCHER) {
                hasArcher = true;
                break;
            }
        }
        boolean isRanged = !isAdjacent && hasArcher
                && HexUtils.isDistanceTwo(anchorCol, anchorRow, targetCol, targetRow, world.getTiles());

        if (!isAdjacent && !isRanged) {
            sendTo(requesterId, new ErrorMessage("Target is out of range"));
            return;
        }

        List<GameWorld.NetworkUnit> defenderStack = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : world.getUnitsAt(targetCol, targetRow)) {
            if (networkUnit.ownerId != null && !requesterId.equals(networkUnit.ownerId)) {
                defenderStack.add(networkUnit);
            }
        }

        if (defenderStack.isEmpty()) {
            attackStructure(requesterId, targetCol, targetRow, attackingStack, attackerUnits);
            return;
        }

        String defenderOwnerId = defenderStack.get(0).ownerId;
        if (!isEnemy(requesterId, defenderOwnerId)) {
            sendTo(requesterId, new ErrorMessage("You are not at war with this player"));
            return;
        }

        List<Unit> defenderUnits = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : defenderStack) {
            defenderUnits.add(networkUnit.unit);
        }

        lastCombatEvent = null;
        new CombatService().resolveCombat(attackerUnits, defenderUnits, false, isRanged);
        CombatResultEvent combatResult = lastCombatEvent;

        List<Integer> attackingUnitIds = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : attackingStack) {
            networkUnit.unit.setCurrentAP(networkUnit.unit.getCurrentAP() - 1);
            attackingUnitIds.add(networkUnit.id);
        }

        List<Integer> killedUnitIds = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : attackingStack) {
            if (networkUnit.unit.isDead()) {
                killedUnitIds.add(networkUnit.id);
            }
        }
        for (GameWorld.NetworkUnit networkUnit : defenderStack) {
            if (networkUnit.unit.isDead()) {
                killedUnitIds.add(networkUnit.id);
            }
        }
        for (int id : killedUnitIds) {
            world.removeUnit(id);
        }

        List<Integer> attackerRolls = combatResult != null ? combatResult.getAttackerRolls() : List.of();
        List<Integer> defenderRolls = combatResult != null ? combatResult.getDefenderRolls() : List.of();
        int attackerHits = combatResult != null ? combatResult.getAttackerHits() : 0;
        int defenderHits = combatResult != null ? combatResult.getDefenderHits() : 0;

        broadcast(new CombatResultMessage(targetCol, targetRow, attackingUnitIds, attackerRolls, defenderRolls,
                attackerHits, defenderHits, 0, killedUnitIds, null), null);
    }

    private void attackStructure(String requesterId, int targetCol, int targetRow,
                                  List<GameWorld.NetworkUnit> attackingStack, List<Unit> attackerUnits) {
        GameWorld.NetworkBuilding targetBuilding = world.getBuildingAt(targetCol, targetRow);
        if (targetBuilding == null || targetBuilding.ownerId == null || requesterId.equals(targetBuilding.ownerId)) {
            sendTo(requesterId, new ErrorMessage("No enemy target at that location"));
            return;
        }
        if (!isEnemy(requesterId, targetBuilding.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You are not at war with this player"));
            return;
        }

        CombatService combatService = new CombatService();
        int baseDamage = combatService.calculateStructureDamage(attackerUnits);
        int bonusDamage = 0;
        for (GameWorld.NetworkUnit networkUnit : attackingStack) {
            if (combatBoostedUnitIds.remove(networkUnit.id)) {
                bonusDamage += 5;
            }
        }
        int damage = baseDamage + bonusDamage;
        targetBuilding.building.takeDamage(damage);

        List<Integer> attackingUnitIds = new ArrayList<>();
        for (GameWorld.NetworkUnit networkUnit : attackingStack) {
            networkUnit.unit.setCurrentAP(networkUnit.unit.getCurrentAP() - 1);
            attackingUnitIds.add(networkUnit.id);
        }

        boolean destroyed = targetBuilding.building.isDestroyed();
        Integer destroyedId = destroyed ? targetBuilding.id : null;
        broadcast(new CombatResultMessage(targetCol, targetRow, attackingUnitIds, List.of(), List.of(),
                0, 0, damage, List.of(), destroyedId), null);
        if (destroyed) {
            String defeatedOwnerId = targetBuilding.ownerId;
            world.removeBuilding(targetBuilding.id);
            checkEliminationAndWinCondition(defeatedOwnerId);
        }
    }

    void handleCheatCommand(String requesterId, String rawCommand) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String[] parts = rawCommand.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        switch (command) {
            case "/greedisgood" -> applyGreedIsGoodCheat(requesterId);
            case "/redbull" -> applyRedBullCheat(requesterId);
            case "/marco" -> applyMarcoCheat(requesterId);
            case "/killhall" -> applyKillHallCheat(requesterId, parts);
            default -> sendTo(requesterId, new ErrorMessage("Unknown cheat command: " + command));
        }
    }

    private void applyGreedIsGoodCheat(String requesterId) {
        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        if (economy == null) {
            return;
        }
        GlobalResourceManager resources = economy.resources;
        int cattleCap = Math.max(resources.getResourceCapacityAmount(ResourceType.CATTLE), resources.getResourceAmount(ResourceType.CATTLE) + 500);
        int wheatCap = Math.max(resources.getResourceCapacityAmount(ResourceType.WHEAT), resources.getResourceAmount(ResourceType.WHEAT) + 500);
        int woodCap = Math.max(resources.getResourceCapacityAmount(ResourceType.WOOD), resources.getResourceAmount(ResourceType.WOOD) + 500);
        int stoneCap = Math.max(resources.getResourceCapacityAmount(ResourceType.STONE), resources.getResourceAmount(ResourceType.STONE) + 500);
        int ironCap = Math.max(resources.getResourceCapacityAmount(ResourceType.IRON), resources.getResourceAmount(ResourceType.IRON) + 500);
        int fishCap = Math.max(resources.getResourceCapacityAmount(ResourceType.FISH), resources.getResourceAmount(ResourceType.FISH) + 500);
        resources.updateStorage(cattleCap, wheatCap, woodCap, stoneCap, ironCap, fishCap);

        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.NONE) continue;
            resources.addResource(type, 500);
        }
        sendTo(requesterId, new network.protocol.CheatAppliedMessage("/greedisgood", "Added 500 of every resource"));
        sendEconomyUpdate(requesterId);
    }

    private void applyRedBullCheat(String requesterId) {
        for (GameWorld.NetworkUnit networkUnit : world.getUnitsOwnedBy(requesterId)) {
            networkUnit.unit.setCurrentAP(networkUnit.unit.getType().getMaxAP());
        }
        sendTo(requesterId, new network.protocol.CheatAppliedMessage("/redbull", "All your units' action points are full"));
    }

    private void applyMarcoCheat(String requesterId) {
        fogRevealedPlayerIds.add(requesterId);
        sendTo(requesterId, new network.protocol.CheatAppliedMessage("/marco", "Fog of war revealed for you"));
        sendTo(requesterId, buildSnapshotMessage(requesterId));
    }

    private void applyKillHallCheat(String requesterId, String[] parts) {
        if (parts.length < 3) {
            sendTo(requesterId, new ErrorMessage("Usage: /killhall <col> <row>"));
            return;
        }
        int col, row;
        try {
            col = Integer.parseInt(parts[1]);
            row = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            sendTo(requesterId, new ErrorMessage("Usage: /killhall <col> <row>"));
            return;
        }
        GameWorld.NetworkBuilding target = world.getBuildingAt(col, row);
        if (target == null || target.building.getType() != BuildingType.TOWN_HALL) {
            sendTo(requesterId, new ErrorMessage("No Town Hall at that location"));
            return;
        }
        if (target.ownerId == null || target.ownerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("That Town Hall is not an enemy's"));
            return;
        }
        if (!isEnemy(requesterId, target.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You are not at war with this player"));
            return;
        }
        target.building.takeDamage(target.building.getHP());
        String defeatedOwnerId = target.ownerId;
        world.removeBuilding(target.id);
        broadcast(new network.protocol.CheatAppliedMessage("/killhall", "A natural disaster levels the Town Hall at (" + col + ", " + row + ")"), null);
        checkEliminationAndWinCondition(defeatedOwnerId);
    }

    private void checkEliminationAndWinCondition(String playerId) {
        if (world == null || playerId == null || eliminatedPlayerIds.contains(playerId)) {
            return;
        }
        if (world.countActiveTownHalls(playerId) > 0) {
            return;
        }
        eliminatedPlayerIds.add(playerId);
        broadcast(new network.protocol.PlayerEliminatedMessage(playerId), null);

        List<String> remaining = new ArrayList<>();
        for (String id : allPlayerIds) {
            if (!eliminatedPlayerIds.contains(id)) {
                remaining.add(id);
            }
        }
        if (remaining.size() == 1) {
            String winnerId = remaining.get(0);
            ClientHandler winnerHandler = clientsById.get(winnerId);
            String winnerName = winnerHandler != null ? winnerHandler.getPlayerName() : winnerId;
            broadcast(new network.protocol.GameOverMessage(winnerId, winnerName), null);
        }
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
        resetActionPointsFor(currentId);
        tickProductionFor(currentId);
        rollForDisasterFor(currentId);
        ClientHandler handler = clientsById.get(currentId);
        String currentName = handler != null ? handler.getPlayerName() : "";
        broadcast(new TurnChangedMessage(currentId, currentName), null);
    }

    private void resetActionPointsFor(String playerId) {
        if (world == null) {
            return;
        }
        for (GameWorld.NetworkUnit networkUnit : world.getUnits()) {
            if (playerId.equals(networkUnit.ownerId)) {
                networkUnit.unit.setCurrentAP(networkUnit.unit.getType().getMaxAP());
                combatBoostedUnitIds.remove(networkUnit.id);
            }
        }
    }

    private static final double DISASTER_CHANCE = 0.05;
    private static final int DISASTER_RADIUS = 2;
    private static final int DISASTER_UNIT_DAMAGE = 15;
    private static final int DISASTER_TOWNHALL_DAMAGE = 40;
    private final java.util.Random disasterRandom = new java.util.Random();

    private void rollForDisasterFor(String playerId) {
        if (world == null || disasterRandom.nextDouble() >= DISASTER_CHANCE) {
            return;
        }
        List<GameWorld.NetworkBuilding> ownedBuildings = world.getBuildingsOwnedBy(playerId);
        if (ownedBuildings.isEmpty()) {
            return;
        }
        GameWorld.NetworkBuilding center = ownedBuildings.get(disasterRandom.nextInt(ownedBuildings.size()));
        int centerCol = center.building.getCol();
        int centerRow = center.building.getRow();

        List<Tile> area = HexUtils.hexesWithinRadius(centerCol, centerRow, DISASTER_RADIUS, world.getTiles());
        List<Integer> killedUnitIds = new ArrayList<>();
        for (Tile tile : area) {
            for (GameWorld.NetworkUnit networkUnit : world.getUnitsAt(tile.getCol(), tile.getRow())) {
                networkUnit.unit.takeDamage(DISASTER_UNIT_DAMAGE);
                if (networkUnit.unit.isDead()) {
                    killedUnitIds.add(networkUnit.id);
                }
            }
            Building building = tile.getBuilding();
            if (building != null && building.getType() == BuildingType.TOWN_HALL) {
                int damage = Math.min(DISASTER_TOWNHALL_DAMAGE, building.getHP() - 1);
                if (damage > 0) {
                    building.takeDamage(damage);
                }
            }
        }
        for (int id : killedUnitIds) {
            world.removeUnit(id);
        }
        broadcast(new network.protocol.DisasterOccurredMessage(playerId, centerCol, centerRow, DISASTER_RADIUS, killedUnitIds), null);
    }

    private void tickProductionFor(String playerId) {
        if (world == null) {
            return;
        }
        for (GameWorld.NetworkBuilding networkBuilding : world.getBuildingsOwnedBy(playerId)) {
            Building building = networkBuilding.building;
            if (!building.isProducing()) {
                continue;
            }
            building.decrementProductionTurns();
            if (building.getProductionTurnsLeft() <= 0) {
                completeProduction(networkBuilding, playerId);
            }
        }
    }

    private void completeProduction(GameWorld.NetworkBuilding networkBuilding, String playerId) {
        Building building = networkBuilding.building;
        if (building.getProducingUnit() != null) {
            UnitType newType = building.getProducingUnit();
            Unit newUnit = new Unit(newType, building.getCol(), building.getRow());
            GameWorld.NetworkUnit created = world.registerUnit(newUnit, playerId);
            broadcast(new ProductionCompletedMessage(networkBuilding.id, "UNIT", newType.name(),
                    created.id, newUnit.getCol(), newUnit.getRow(), playerId), null);
            sendEconomyUpdate(playerId);
        } else if (building.getUpgradingToLevel() != null) {
            building.applyLevelUpgrade();
            GameWorld.PlayerEconomy economy = world.getEconomy(playerId);
            if (economy != null) {
                TownHallLevel level = building.getTownHallLevel();
                economy.resources.updateStorage(level.getCattleCapacity(), level.getWheatCapacity(),
                        level.getWoodCapacity(), level.getStoneCapacity(), level.getIronCapacity(), level.getFishCapacity());
            }
            broadcast(new ProductionCompletedMessage(networkBuilding.id, "UPGRADE",
                    building.getTownHallLevel().name(), -1, building.getCol(), building.getRow(), playerId), null);
            sendEconomyUpdate(playerId);
        } else if (building.getResearchingTech() != null) {
            TechType tech = building.getResearchingTech();
            GameWorld.PlayerEconomy economy = world.getEconomy(playerId);
            if (economy != null) {
                economy.researchedTechs.put(tech, true);
            }
            broadcast(new ProductionCompletedMessage(networkBuilding.id, "TECH", tech.name(),
                    -1, building.getCol(), building.getRow(), playerId), null);
            sendEconomyUpdate(playerId);
        }
        building.clearProduction();
    }

    void handleStartUnitProductionRequest(String requesterId, int buildingId, String unitTypeName) {
        GameWorld.NetworkBuilding networkBuilding = validateProductionOwnership(requesterId, buildingId);
        if (networkBuilding == null) {
            return;
        }
        Building building = networkBuilding.building;
        if (building.getType() != BuildingType.TOWN_HALL) {
            sendTo(requesterId, new ErrorMessage("Only the Town Hall can produce units"));
            return;
        }
        UnitType unitType;
        try {
            unitType = UnitType.valueOf(unitTypeName);
        } catch (IllegalArgumentException e) {
            sendTo(requesterId, new ErrorMessage("Unknown unit type"));
            return;
        }
        if (building.isProducing()) {
            sendTo(requesterId, new ErrorMessage("This building is already producing something"));
            return;
        }
        if (unitType == UnitType.CAVALRY) {
            if (building.getTownHallLevel().getLevelNumber() < TownHallLevel.LEVEL_2.getLevelNumber()) {
                sendTo(requesterId, new ErrorMessage("Cavalry requires a level 2 Town Hall"));
                return;
            }
            if (!world.hasBuildingType(requesterId, BuildingType.STABLE)) {
                sendTo(requesterId, new ErrorMessage("Cavalry requires a Stable"));
                return;
            }
        }

        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        int woodCost = unitType == UnitType.SWORDSMAN ? 10 : 0;
        if (woodCost > 0 && !economy.resources.hasEnough(ResourceType.WOOD, woodCost)) {
            sendTo(requesterId, new ErrorMessage("Not enough wood to produce this unit"));
            return;
        }
        if (!economy.resources.spendFood(unitType.getFoodCost())) {
            sendTo(requesterId, new ErrorMessage("Not enough food to produce this unit"));
            return;
        }
        if (woodCost > 0) {
            economy.resources.spendResource(ResourceType.WOOD, woodCost);
        }

        building.startProducing(unitType);
        broadcast(new ProductionStartedMessage(networkBuilding.id, "UNIT", unitType.name(), building.getProductionTurnsLeft()), null);
        sendEconomyUpdate(requesterId);
    }

    void handleStartUpgradeRequest(String requesterId, int buildingId) {
        GameWorld.NetworkBuilding networkBuilding = validateProductionOwnership(requesterId, buildingId);
        if (networkBuilding == null) {
            return;
        }
        Building building = networkBuilding.building;
        if (building.getType() != BuildingType.TOWN_HALL) {
            sendTo(requesterId, new ErrorMessage("Only the Town Hall can be upgraded"));
            return;
        }
        if (building.isProducing()) {
            sendTo(requesterId, new ErrorMessage("This building is already producing something"));
            return;
        }
        TownHallLevel nextLevel = building.getTownHallLevel().getNextLevel();
        if (nextLevel == null) {
            sendTo(requesterId, new ErrorMessage("Town Hall is already at its maximum level"));
            return;
        }
        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        if (!(economy.resources.hasEnough(ResourceType.WOOD, nextLevel.getUpgradeWoodCost())
                && economy.resources.hasEnough(ResourceType.STONE, nextLevel.getUpgradeStoneCost())
                && economy.resources.hasEnough(ResourceType.IRON, nextLevel.getUpgradeIronCost()))) {
            sendTo(requesterId, new ErrorMessage("Not enough resources for this upgrade"));
            return;
        }
        economy.resources.spendResource(ResourceType.WOOD, nextLevel.getUpgradeWoodCost());
        economy.resources.spendResource(ResourceType.STONE, nextLevel.getUpgradeStoneCost());
        economy.resources.spendResource(ResourceType.IRON, nextLevel.getUpgradeIronCost());

        building.startUpgrading(nextLevel);
        broadcast(new ProductionStartedMessage(networkBuilding.id, "UPGRADE", nextLevel.name(), building.getProductionTurnsLeft()), null);
        sendEconomyUpdate(requesterId);
    }

    void handleStartTechResearchRequest(String requesterId, int buildingId, String techTypeName) {
        GameWorld.NetworkBuilding networkBuilding = validateProductionOwnership(requesterId, buildingId);
        if (networkBuilding == null) {
            return;
        }
        Building building = networkBuilding.building;
        if (building.getType() != BuildingType.TOWN_HALL) {
            sendTo(requesterId, new ErrorMessage("Only the Town Hall can research technology"));
            return;
        }
        TechType tech;
        try {
            tech = TechType.valueOf(techTypeName);
        } catch (IllegalArgumentException e) {
            sendTo(requesterId, new ErrorMessage("Unknown technology"));
            return;
        }
        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        if (economy.hasTech(tech)) {
            sendTo(requesterId, new ErrorMessage("Already researched"));
            return;
        }
        if (building.getTownHallLevel().getLevelNumber() < tech.getRequiredLevel().getLevelNumber()) {
            sendTo(requesterId, new ErrorMessage("Town Hall level too low for this technology"));
            return;
        }
        if (!economy.resources.hasEnough(tech.getCostResource(), tech.getCostAmount())) {
            sendTo(requesterId, new ErrorMessage("Not enough resources for this technology"));
            return;
        }

        if (tech.isInstant()) {
            if (tech.getCostAmount() > 0) {
                economy.resources.spendResource(tech.getCostResource(), tech.getCostAmount());
            }
            economy.researchedTechs.put(tech, true);
            broadcast(new ProductionCompletedMessage(networkBuilding.id, "TECH", tech.name(),
                    -1, building.getCol(), building.getRow(), requesterId), null);
            sendEconomyUpdate(requesterId);
            return;
        }

        if (building.isProducing()) {
            sendTo(requesterId, new ErrorMessage("This building is already producing something"));
            return;
        }
        if (tech.getCostAmount() > 0) {
            economy.resources.spendResource(tech.getCostResource(), tech.getCostAmount());
        }
        building.startResearching(tech);
        broadcast(new ProductionStartedMessage(networkBuilding.id, "TECH", tech.name(), building.getProductionTurnsLeft()), null);
        sendEconomyUpdate(requesterId);
    }

    void handleConstructBuildingRequest(String requesterId, int builderUnitId, String buildingTypeName) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return;
        }
        GameWorld.NetworkUnit builder = world.getUnitById(builderUnitId);
        if (builder == null) {
            sendTo(requesterId, new ErrorMessage("Unit not found"));
            return;
        }
        if (!requesterId.equals(builder.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this unit"));
            return;
        }
        if (builder.unit.getType() != UnitType.BUILDER) {
            sendTo(requesterId, new ErrorMessage("Only a Builder can construct buildings"));
            return;
        }
        BuildingType buildingType;
        try {
            buildingType = BuildingType.valueOf(buildingTypeName);
        } catch (IllegalArgumentException e) {
            sendTo(requesterId, new ErrorMessage("Unknown building type"));
            return;
        }
        if (!buildingType.isPlayerBuildable()) {
            sendTo(requesterId, new ErrorMessage("This building cannot be constructed"));
            return;
        }
        Tile tile = world.getTile(builder.unit.getCol(), builder.unit.getRow());
        if (tile.getBuilding() != null) {
            sendTo(requesterId, new ErrorMessage("This tile already has a building"));
            return;
        }
        if (!buildingType.isBuildableAt(tile, world.getTiles())) {
            sendTo(requesterId, new ErrorMessage("This building cannot be built on this terrain"));
            return;
        }

        int townHallLevel = 0;
        for (GameWorld.NetworkBuilding networkBuilding : world.getBuildingsOwnedBy(requesterId)) {
            if (networkBuilding.building.getType() == BuildingType.TOWN_HALL) {
                townHallLevel = Math.max(townHallLevel, networkBuilding.building.getTownHallLevel().getLevelNumber());
            }
        }
        if (townHallLevel < buildingType.getRequiredTownHallLevel()) {
            sendTo(requesterId, new ErrorMessage("Town Hall level too low for this building"));
            return;
        }

        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        boolean stoneTech = economy.hasTech(TechType.STONE_MINING);
        boolean ironTech = economy.hasTech(TechType.IRON_MINING);
        boolean settlementTech = economy.hasTech(TechType.SETTLEMENT_TECH);
        if (!buildingType.isUnlocked(stoneTech, ironTech, settlementTech)) {
            sendTo(requesterId, new ErrorMessage("This building requires more research"));
            return;
        }

        if (builder.unit.getCurrentAP() < buildingType.getApCost()) {
            sendTo(requesterId, new ErrorMessage("Not enough action points"));
            return;
        }
        if (!(economy.resources.hasEnough(ResourceType.WOOD, buildingType.getWoodCost())
                && economy.resources.hasEnough(ResourceType.STONE, buildingType.getStoneCost())
                && economy.resources.hasEnough(ResourceType.IRON, buildingType.getIronCost()))) {
            sendTo(requesterId, new ErrorMessage("Not enough resources for this building"));
            return;
        }

        builder.unit.setCurrentAP(builder.unit.getCurrentAP() - buildingType.getApCost());
        economy.resources.spendResource(ResourceType.WOOD, buildingType.getWoodCost());
        economy.resources.spendResource(ResourceType.STONE, buildingType.getStoneCost());
        economy.resources.spendResource(ResourceType.IRON, buildingType.getIronCost());

        Building building = new Building(buildingType, builder.unit.getCol(), builder.unit.getRow());
        tile.setBuilding(building);
        GameWorld.NetworkBuilding registered = world.registerBuilding(building, requesterId);

        builder.unit.useCharge();
        boolean builderConsumed = builder.unit.getCharge() <= 0;
        if (builderConsumed) {
            world.removeUnit(builder.id);
        }

        broadcast(new network.protocol.BuildingConstructedMessage(registered.id, requesterId, buildingType.name(),
                building.getCol(), building.getRow()), null);
        if (builderConsumed) {
            broadcast(new network.protocol.UnitRemovedMessage(builder.id, "Builder consumed"), null);
        }
        sendEconomyUpdate(requesterId);
    }

    private static final int EXPANSION_WOOD_COST = 300;
    private static final int EXPANSION_STONE_COST = 300;
    private static final int EXPANSION_IRON_COST = 200;

    void handleConstructTownHallRequest(String requesterId, int builderUnitId) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return;
        }
        GameWorld.NetworkUnit builder = world.getUnitById(builderUnitId);
        if (builder == null || !requesterId.equals(builder.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this unit"));
            return;
        }
        if (builder.unit.getType() != UnitType.BUILDER) {
            sendTo(requesterId, new ErrorMessage("Only a Builder can found a new Town Hall"));
            return;
        }
        Tile tile = world.getTile(builder.unit.getCol(), builder.unit.getRow());
        if (tile.getBuilding() != null) {
            sendTo(requesterId, new ErrorMessage("This tile already has a building"));
            return;
        }
        if (!tile.getTerrain().isPassable() || tile.getTerrain() == model.TerrainType.SEA
                || tile.getTerrain() == model.TerrainType.MOUNTAIN_RANGE) {
            sendTo(requesterId, new ErrorMessage("A Town Hall cannot be founded on this terrain"));
            return;
        }
        if (builder.unit.getCurrentAP() < BuildingType.TOWN_HALL.getApCost()) {
            sendTo(requesterId, new ErrorMessage("Not enough action points"));
            return;
        }

        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        if (!(economy.resources.hasEnough(ResourceType.WOOD, EXPANSION_WOOD_COST)
                && economy.resources.hasEnough(ResourceType.STONE, EXPANSION_STONE_COST)
                && economy.resources.hasEnough(ResourceType.IRON, EXPANSION_IRON_COST))) {
            sendTo(requesterId, new ErrorMessage("Founding a new Town Hall requires " + EXPANSION_WOOD_COST
                    + " wood, " + EXPANSION_STONE_COST + " stone and " + EXPANSION_IRON_COST + " iron"));
            return;
        }

        builder.unit.setCurrentAP(builder.unit.getCurrentAP() - BuildingType.TOWN_HALL.getApCost());
        economy.resources.spendResource(ResourceType.WOOD, EXPANSION_WOOD_COST);
        economy.resources.spendResource(ResourceType.STONE, EXPANSION_STONE_COST);
        economy.resources.spendResource(ResourceType.IRON, EXPANSION_IRON_COST);

        Building newHall = new Building(BuildingType.TOWN_HALL, builder.unit.getCol(), builder.unit.getRow());
        tile.setBuilding(newHall);
        GameWorld.NetworkBuilding registered = world.registerBuilding(newHall, requesterId);

        builder.unit.useCharge();
        boolean builderConsumed = builder.unit.getCharge() <= 0;
        if (builderConsumed) {
            world.removeUnit(builder.id);
        }

        broadcast(new network.protocol.BuildingConstructedMessage(registered.id, requesterId, BuildingType.TOWN_HALL.name(),
                newHall.getCol(), newHall.getRow()), null);
        if (builderConsumed) {
            broadcast(new network.protocol.UnitRemovedMessage(builder.id, "Builder consumed"), null);
        }
        sendEconomyUpdate(requesterId);
    }

    void handleProduceItemRequest(String requesterId, int apothecaryBuildingId, String itemTypeName) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return;
        }
        GameWorld.NetworkBuilding networkBuilding = world.getBuildingById(apothecaryBuildingId);
        if (networkBuilding == null || !requesterId.equals(networkBuilding.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this building"));
            return;
        }
        if (networkBuilding.building.getType() != BuildingType.APOTHECARY) {
            sendTo(requesterId, new ErrorMessage("Only an Apothecary can produce items"));
            return;
        }
        network.protocol.ItemType itemType;
        try {
            itemType = network.protocol.ItemType.valueOf(itemTypeName);
        } catch (IllegalArgumentException e) {
            sendTo(requesterId, new ErrorMessage("Unknown item type"));
            return;
        }
        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        if (!(economy.resources.hasEnough(ResourceType.WOOD, 15) && economy.resources.hasEnough(ResourceType.STONE, 10))) {
            sendTo(requesterId, new ErrorMessage("Not enough resources to produce this item (needs 15 wood, 10 stone)"));
            return;
        }
        economy.resources.spendResource(ResourceType.WOOD, 15);
        economy.resources.spendResource(ResourceType.STONE, 10);
        economy.addItem(itemType, 1);

        sendTo(requesterId, new network.protocol.CheatAppliedMessage("produce-item",
                "Produced 1 " + itemType.getDisplayName()));
        sendEconomyUpdate(requesterId);
    }

    void handleUseTeleportItemRequest(String requesterId, int unitId, int targetCol, int targetRow) {
        GameWorld.NetworkUnit networkUnit = validateItemUsage(requesterId, unitId, network.protocol.ItemType.TELEPORT);
        if (networkUnit == null) {
            return;
        }
        if (targetCol < 0 || targetCol >= world.getCols() || targetRow < 0 || targetRow >= world.getRows()) {
            sendTo(requesterId, new ErrorMessage("Target tile is out of bounds"));
            return;
        }
        if (!world.getUnitsAt(targetCol, targetRow).isEmpty()) {
            sendTo(requesterId, new ErrorMessage("Target tile is occupied"));
            return;
        }
        Tile targetTile = world.getTile(targetCol, targetRow);
        if (!targetTile.getTerrain().isPassable()) {
            sendTo(requesterId, new ErrorMessage("Target terrain is not passable"));
            return;
        }

        world.getEconomy(requesterId).consumeItem(network.protocol.ItemType.TELEPORT);
        networkUnit.unit.placeAt(targetCol, targetRow);
        broadcast(new UnitMovedMessage(unitId, targetCol, targetRow, networkUnit.unit.getCurrentAP()), null);
        broadcast(new network.protocol.ItemUsedMessage("TELEPORT", unitId, "Teleported to (" + targetCol + ", " + targetRow + ")"), null);
        sendEconomyUpdate(requesterId);
        sendTo(requesterId, buildSnapshotMessage(requesterId));
    }

    void handleUseMobilityItemRequest(String requesterId, int unitId) {
        GameWorld.NetworkUnit networkUnit = validateItemUsage(requesterId, unitId, network.protocol.ItemType.MOBILITY);
        if (networkUnit == null) {
            return;
        }
        world.getEconomy(requesterId).consumeItem(network.protocol.ItemType.MOBILITY);
        networkUnit.unit.setCurrentAP(networkUnit.unit.getCurrentAP() + 2);
        broadcast(new UnitMovedMessage(unitId, networkUnit.unit.getCol(), networkUnit.unit.getRow(), networkUnit.unit.getCurrentAP()), null);
        broadcast(new network.protocol.ItemUsedMessage("MOBILITY", unitId, "+2 action points"), null);
        sendEconomyUpdate(requesterId);
    }

    void handleUseCombatBoostItemRequest(String requesterId, int unitId) {
        GameWorld.NetworkUnit networkUnit = validateItemUsage(requesterId, unitId, network.protocol.ItemType.COMBAT_BOOST);
        if (networkUnit == null) {
            return;
        }
        world.getEconomy(requesterId).consumeItem(network.protocol.ItemType.COMBAT_BOOST);
        combatBoostedUnitIds.add(unitId);
        broadcast(new network.protocol.ItemUsedMessage("COMBAT_BOOST", unitId, "+5 structure damage on its next attack this turn"), null);
        sendEconomyUpdate(requesterId);
    }

    private GameWorld.NetworkUnit validateItemUsage(String requesterId, int unitId, network.protocol.ItemType requiredItem) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return null;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return null;
        }
        GameWorld.NetworkUnit networkUnit = world.getUnitById(unitId);
        if (networkUnit == null || !requesterId.equals(networkUnit.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this unit"));
            return null;
        }
        GameWorld.PlayerEconomy economy = world.getEconomy(requesterId);
        if (economy == null || economy.getItemCount(requiredItem) <= 0) {
            sendTo(requesterId, new ErrorMessage("You have none of this item"));
            return null;
        }
        return networkUnit;
    }

    private GameWorld.NetworkBuilding validateProductionOwnership(String requesterId, int buildingId) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return null;
        }
        String currentTurnPlayerId = getCurrentTurnPlayerId();
        if (currentTurnPlayerId == null || !currentTurnPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("It's not your turn"));
            return null;
        }
        GameWorld.NetworkBuilding networkBuilding = world.getBuildingById(buildingId);
        if (networkBuilding == null) {
            sendTo(requesterId, new ErrorMessage("Building not found"));
            return null;
        }
        if (!requesterId.equals(networkBuilding.ownerId)) {
            sendTo(requesterId, new ErrorMessage("You do not own this building"));
            return null;
        }
        return networkBuilding;
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

    void handleTradeOfferRequest(String requesterId, String targetPlayerId, Map<String, Integer> offering, Map<String, Integer> requesting) {
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        if (targetPlayerId == null || !clientsById.containsKey(targetPlayerId)) {
            sendTo(requesterId, new ErrorMessage("Target player not found"));
            return;
        }
        if (targetPlayerId.equals(requesterId)) {
            sendTo(requesterId, new ErrorMessage("You cannot trade with yourself"));
            return;
        }
        Map<ResourceType, Integer> offeringParsed;
        Map<ResourceType, Integer> requestingParsed;
        try {
            offeringParsed = parseResourceMap(offering);
            requestingParsed = parseResourceMap(requesting);
        } catch (IllegalArgumentException e) {
            sendTo(requesterId, new ErrorMessage("Invalid resource in trade offer"));
            return;
        }
        if (offeringParsed.isEmpty() && requestingParsed.isEmpty()) {
            sendTo(requesterId, new ErrorMessage("A trade offer must include at least one resource"));
            return;
        }

        GameWorld.PlayerEconomy senderEconomy = world.getEconomy(requesterId);
        for (Map.Entry<ResourceType, Integer> entry : offeringParsed.entrySet()) {
            if (!senderEconomy.resources.hasEnough(entry.getKey(), entry.getValue())) {
                sendTo(requesterId, new ErrorMessage("Not enough " + entry.getKey().getDisplayName() + " to offer"));
                return;
            }
        }
        for (Map.Entry<ResourceType, Integer> entry : offeringParsed.entrySet()) {
            senderEconomy.resources.spendResource(entry.getKey(), entry.getValue());
        }

        int offerId = nextTradeOfferId.getAndIncrement();
        pendingTradeOffers.put(offerId, new TradeOffer(requesterId, targetPlayerId, offeringParsed, requestingParsed));
        sendEconomyUpdate(requesterId);

        ClientHandler senderHandler = clientsById.get(requesterId);
        String senderName = senderHandler != null ? senderHandler.getPlayerName() : requesterId;
        sendTo(targetPlayerId, new network.protocol.TradeOfferReceivedMessage(offerId, requesterId, senderName, offering, requesting));
        sendTo(requesterId, new network.protocol.CheatAppliedMessage("trade-offer", "Trade offer sent, resources locked"));
    }

    void handleTradeRespondRequest(String requesterId, int offerId, boolean accept) {
        TradeOffer offer = pendingTradeOffers.get(offerId);
        if (offer == null) {
            sendTo(requesterId, new ErrorMessage("Trade offer not found"));
            return;
        }
        if (!requesterId.equals(offer.toPlayerId)) {
            sendTo(requesterId, new ErrorMessage("Only the recipient can respond to this trade"));
            return;
        }

        if (!accept) {
            pendingTradeOffers.remove(offerId);
            refundOffer(offer);
            broadcastTradeResolution(offer, offerId, false);
            return;
        }

        GameWorld.PlayerEconomy responderEconomy = world.getEconomy(requesterId);
        for (Map.Entry<ResourceType, Integer> entry : offer.requesting.entrySet()) {
            if (!responderEconomy.resources.hasEnough(entry.getKey(), entry.getValue())) {
                sendTo(requesterId, new ErrorMessage("Not enough " + entry.getKey().getDisplayName() + " to accept this trade"));
                return;
            }
        }

        for (Map.Entry<ResourceType, Integer> entry : offer.requesting.entrySet()) {
            responderEconomy.resources.spendResource(entry.getKey(), entry.getValue());
        }
        GameWorld.PlayerEconomy senderEconomy = world.getEconomy(offer.fromPlayerId);
        for (Map.Entry<ResourceType, Integer> entry : offer.requesting.entrySet()) {
            senderEconomy.resources.addResource(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<ResourceType, Integer> entry : offer.offering.entrySet()) {
            responderEconomy.resources.addResource(entry.getKey(), entry.getValue());
        }

        pendingTradeOffers.remove(offerId);
        sendEconomyUpdate(offer.fromPlayerId);
        sendEconomyUpdate(requesterId);
        broadcastTradeResolution(offer, offerId, true);
    }

    private void refundOffer(TradeOffer offer) {
        GameWorld.PlayerEconomy senderEconomy = world.getEconomy(offer.fromPlayerId);
        if (senderEconomy == null) {
            return;
        }
        for (Map.Entry<ResourceType, Integer> entry : offer.offering.entrySet()) {
            senderEconomy.resources.addResource(entry.getKey(), entry.getValue());
        }
        sendEconomyUpdate(offer.fromPlayerId);
    }

    private void broadcastTradeResolution(TradeOffer offer, int offerId, boolean accepted) {
        network.protocol.TradeOfferResolvedMessage message = new network.protocol.TradeOfferResolvedMessage(offerId, accepted);
        sendTo(offer.fromPlayerId, message);
        sendTo(offer.toPlayerId, message);
    }

    private Map<ResourceType, Integer> parseResourceMap(Map<String, Integer> raw) {
        Map<ResourceType, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            ResourceType type = ResourceType.valueOf(entry.getKey());
            if (type == ResourceType.NONE || entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Invalid resource entry");
            }
            if (entry.getValue() > 0) {
                result.put(type, entry.getValue());
            }
        }
        return result;
    }

    private static final String SAVE_DIRECTORY = "network_saves";
    private static final String SAVE_FILE_NAME = "slot0.sav";

    void handleSaveGameRequest(String requesterId) {
        if (!requesterId.equals(hostPlayerId)) {
            sendTo(requesterId, new ErrorMessage("Only the host can save the game"));
            return;
        }
        if (!gameStarted || world == null) {
            sendTo(requesterId, new ErrorMessage("Game hasn't started yet"));
            return;
        }
        try {
            File dir = new File(SAVE_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            GameSaveData data = new GameSaveData(world, new ArrayList<>(turnOrder), currentTurnIndex, hostPlayerId,
                    selectedMapName, copyEnemyMap(), new ArrayList<>(allPlayerIds), new HashSet<>(eliminatedPlayerIds));
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(dir, SAVE_FILE_NAME)))) {
                out.writeObject(data);
            }
            broadcast(new network.protocol.GameSavedMessage(true, "Game saved"), null);
        } catch (IOException e) {
            sendTo(requesterId, new network.protocol.GameSavedMessage(false, "Save failed: " + e.getMessage()));
        }
    }

    void handleLoadGameRequest(String requesterId) {
        if (!requesterId.equals(hostPlayerId)) {
            sendTo(requesterId, new ErrorMessage("Only the host can load a game"));
            return;
        }
        if (gameStarted) {
            sendTo(requesterId, new ErrorMessage("Cannot load while a game is already in progress"));
            return;
        }
        File file = new File(SAVE_DIRECTORY, SAVE_FILE_NAME);
        if (!file.exists()) {
            sendTo(requesterId, new ErrorMessage("No saved game found"));
            return;
        }

        GameSaveData data;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            data = (GameSaveData) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            sendTo(requesterId, new ErrorMessage("Failed to load save: " + e.getMessage()));
            return;
        }

        Set<String> connectedIds = new HashSet<>(clientsById.keySet());
        Set<String> savedIds = new HashSet<>(data.allPlayerIds);
        if (!connectedIds.equals(savedIds)) {
            sendTo(requesterId, new ErrorMessage("Connected players do not match the save (need exactly: " + savedIds + ")"));
            return;
        }

        world = data.world;
        turnOrder.clear();
        turnOrder.addAll(data.turnOrder);
        currentTurnIndex = data.currentTurnIndex;
        selectedMapName = data.selectedMapName;
        enemyMap.clear();
        for (Map.Entry<String, Set<String>> entry : data.enemyMap.entrySet()) {
            Set<String> restored = ConcurrentHashMap.newKeySet();
            restored.addAll(entry.getValue());
            enemyMap.put(entry.getKey(), restored);
        }
        allPlayerIds.clear();
        allPlayerIds.addAll(data.allPlayerIds);
        eliminatedPlayerIds.clear();
        eliminatedPlayerIds.addAll(data.eliminatedPlayerIds);
        gameStarted = true;

        broadcast(new GameStartedMessage(), null);
        for (String playerId : allPlayerIds) {
            sendTo(playerId, buildSnapshotMessage(playerId));
        }
        String currentId = getCurrentTurnPlayerId();
        if (currentId != null) {
            ClientHandler handler = clientsById.get(currentId);
            String currentName = handler != null ? handler.getPlayerName() : currentId;
            broadcast(new TurnChangedMessage(currentId, currentName), null);
        }
        broadcast(new network.protocol.GameSavedMessage(true, "Game loaded, resuming"), null);
    }

    private Map<String, Set<String>> copyEnemyMap() {
        Map<String, Set<String>> copy = new java.util.HashMap<>();
        for (Map.Entry<String, Set<String>> entry : enemyMap.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    private static class TradeOffer {
        final String fromPlayerId;
        final String toPlayerId;
        final Map<ResourceType, Integer> offering;
        final Map<ResourceType, Integer> requesting;

        TradeOffer(String fromPlayerId, String toPlayerId, Map<ResourceType, Integer> offering, Map<ResourceType, Integer> requesting) {
            this.fromPlayerId = fromPlayerId;
            this.toPlayerId = toPlayerId;
            this.offering = offering;
            this.requesting = requesting;
        }
    }
}
