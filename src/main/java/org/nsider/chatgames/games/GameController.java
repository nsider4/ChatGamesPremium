package org.nsider.chatgames.games;

import lombok.Getter;
import org.nsider.chatgames.games.utils.PlayerResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    /**
     * @return a snapshot of the current results ordered by place (1st first)
     */
    public static List<PlayerResult> getResultsByPlace() {
        List<PlayerResult> sorted = new ArrayList<>(results.values());
        sorted.sort(Comparator.comparingInt(PlayerResult::getPlace));
        return sorted;
    }

    public static void clearResults() {
        results.clear();
    }
}
