package org.nsider.chatgames.games;

import lombok.Getter;
import org.nsider.chatgames.games.utils.PlayerResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameController {
    @Getter private static final Map<UUID, PlayerResult> results = new ConcurrentHashMap<>();


    public static void addResult(UUID uuid, double score, int place) {
        PlayerResult playerResult = new PlayerResult(uuid, score, place);
        results.put(uuid, playerResult);
    }

    public static boolean playerExists(UUID playerUUID) {
        return results.containsKey(playerUUID);
    }

    public static PlayerResult getPlayerResult(UUID playerUUID) {
        return results.get(playerUUID);
    }

    public static void clearResults() {
        results.clear();
    }
}
