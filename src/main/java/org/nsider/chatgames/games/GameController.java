package org.nsider.chatgames.games;

import lombok.Getter;
import org.nsider.chatgames.games.utils.PlayerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameController {
    @Getter private static final List<PlayerResult> results = new ArrayList<>();

    public synchronized static void addResult(UUID uuid, double score, int place) {
        PlayerResult playerResult = new PlayerResult(uuid, score, place);
        results.add(playerResult);
    }

    public synchronized static boolean playerExists(UUID playerUUID) {
        for (PlayerResult result : results) {
            if (result.getUuid().equals(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public synchronized static PlayerResult getPlayerResult(UUID playerUUID) {
        for (PlayerResult result : results) {
            if (result.getUuid().equals(playerUUID)) {
                return result;
            }
        }
        return null;
    }

    public synchronized static void clearResults() {
        results.clear();
    }
}
