package org.nsider.chatgames.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.top.FastestData;

public class TopManager {

    public static void setPoints(OfflinePlayer player, int points) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        if (cgPlayer != null) {
            cgPlayer.setPoints(points);
        }
    }

    public static int getPoints(OfflinePlayer player) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        return cgPlayer != null ? cgPlayer.getPoints() : 0;
    }

    public static void addPoint(OfflinePlayer player, int amount) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        if (cgPlayer != null) {
            cgPlayer.setPoints(cgPlayer.getPoints() + amount);
        }
    }

    public static void removePoints(OfflinePlayer player, int amount) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        if (cgPlayer != null) {
            cgPlayer.setPoints(Math.max(cgPlayer.getPoints() - amount, 0));
        }
    }

    public static void setFastestTime(Player player, GameType game, double time) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        if (cgPlayer != null) {
            FastestData updated = cgPlayer.getFastestData().withGameTime(game, time);
            cgPlayer.setFastestData(updated);
        }
    }

    public static void setOverallFastest(Player player, double time) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        if (cgPlayer != null) {
            FastestData updated = cgPlayer.getFastestData().withOverallFastest(time);
            cgPlayer.setFastestData(updated);
        }
    }

    public static FastestData getFastestData(Player player) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        return cgPlayer != null ? cgPlayer.getFastestData() : null;
    }

    public static boolean isFasterThan(Player player, GameType game, double newTime) {
        FastestData fd = getFastestData(player);
        if (fd == null) return false;
        double current = fd.getTime(game);
        return current == 0.0 || newTime < current;
    }

    public static boolean isFasterThanOverall(Player player, double newTime) {
        FastestData fd = getFastestData(player);
        if (fd == null) return false;
        double current = fd.getBestOverall();
        return current == 0.0 || newTime < current;
    }

}