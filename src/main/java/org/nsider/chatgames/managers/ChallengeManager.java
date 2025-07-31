package org.nsider.chatgames.managers;

import org.bukkit.Bukkit;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.player.ChatGamesPlayer;
import org.nsider.chatgames.player.ChatGamesPlayerManager;

import java.util.UUID;

public class ChallengeManager {

    private static ChatGames plugin;

    public static void initialize(ChatGames instance) {
        plugin = instance;
    }

    public static void incrementWinsIfApplicable(UUID uuid) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(uuid);
        if (data == null) return;

        boolean completed = data.isDailyComplete();
        boolean claimed = data.isRewardClaimed();

        if (!completed && !claimed) {
            data.setWinsToday(data.getWinsToday() + 1);
            if (data.getWinsToday() >= 3) {
                data.setChallengeStreak(data.getChallengeStreak() + 1);
                plugin.getLogger().info("Player " + Bukkit.getOfflinePlayer(uuid).getName() + " completed today's daily challenge!");
            }
        }

        data.setDailyStreak(data.getDailyStreak() + 1);
        data.setWeeklyWins(data.getWeeklyWins() + 1);
    }

    public static void incrementGlobalStreak(UUID uuid) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(uuid);
        if (data != null) {
            data.setGlobalStreak(data.getGlobalStreak() + 1);
        }
    }

    public static boolean isCompleted(UUID uuid) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(uuid);
        return data != null && data.isDailyComplete();
    }

    public static boolean canClaimReward(UUID uuid) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(uuid);
        return data != null && data.canClaimReward();
    }

    public static void markRewardClaimed(UUID uuid) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(uuid);
        if (data != null && data.canClaimReward()) {
            data.markClaimed();
            plugin.getLogger().info("Player " + Bukkit.getOfflinePlayer(uuid).getName() + " claimed their daily challenge reward.");
        }
    }
}