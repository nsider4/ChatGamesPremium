package org.nsider.chatgames.races;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.nsider.chatgames.games.utils.PlayerResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class RaceHandler {
    private static final Map<UUID, Integer> scores = new HashMap<>();
    private static final Map<UUID, PlayerResult> results = new HashMap<>();

    public static void addScore(UUID uuid, int amount) {
        if(!scores.containsKey(uuid)) {
            scores.put(uuid, 1);
            return;
        }
        scores.put(uuid, scores.get(uuid) + amount);
    }

    public static void addPlayersOnline() {
        Bukkit.getOnlinePlayers().forEach(p -> scores.put(p.getUniqueId(), 0));
    }

    public static void removePlayersOnline() {
        scores.clear();
    }

    public static boolean playerWon(UUID uuid, int winningScore, double time) {
        if (scores.get(uuid) >= winningScore) {
            PlayerResult result = new PlayerResult(uuid, time, 1);
            results.put(uuid, result);
            return true;
        }
        return false;
    }

    public static PlayerResult getPlayerResult(UUID uuid) {
        return results.get(uuid);
    }
}

