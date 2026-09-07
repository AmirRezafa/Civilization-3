package network.server;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GameSaveData implements Serializable {
    final GameWorld world;
    final List<String> turnOrder;
    final int currentTurnIndex;
    final String hostPlayerId;
    final String selectedMapName;
    final Map<String, Set<String>> enemyMap;
    final List<String> allPlayerIds;
    final Set<String> eliminatedPlayerIds;

    GameSaveData(GameWorld world, List<String> turnOrder, int currentTurnIndex, String hostPlayerId,
                 String selectedMapName, Map<String, Set<String>> enemyMap, List<String> allPlayerIds,
                 Set<String> eliminatedPlayerIds) {
        this.world = world;
        this.turnOrder = turnOrder;
        this.currentTurnIndex = currentTurnIndex;
        this.hostPlayerId = hostPlayerId;
        this.selectedMapName = selectedMapName;
        this.enemyMap = enemyMap;
        this.allPlayerIds = allPlayerIds;
        this.eliminatedPlayerIds = eliminatedPlayerIds;
    }
}
