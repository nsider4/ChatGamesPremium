package org.nsider.chatgames.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.ChallengeQueries;
import org.nsider.chatgames.database.queries.ManagementQueries;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatGamesPlayerManager {

    private static final Map<UUID, ChatGamesPlayer> cache = new ConcurrentHashMap<>();
    private static ChatGames plugin;

    public static void initialize(ChatGames instance) {
        plugin = instance;
    }

    public static ChatGamesPlayer get(UUID uuid) {
        return cache.get(uuid);
    }

    public static void load(OfflinePlayer player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!ManagementQueries.playerExists(player)) {
                ManagementQueries.createPlayerIfNotExists(player);
            }

            PlayerMainData mainData = ManagementQueries.getPlayerData(player);
            if(mainData == null) return;
            ChallengeData challengeData = ChallengeQueries.getChallengeData(player);
            if(challengeData == null) return;
            ChatGamesPlayer data = getChatGamesPlayer(player, mainData, challengeData);

            cache.put(player.getUniqueId(), data);
        });
    }

    private static @Nonnull ChatGamesPlayer getChatGamesPlayer(OfflinePlayer player, PlayerMainData mainData, ChallengeData challengeData) {
        ChatGamesPlayer data = new ChatGamesPlayer(player.getUniqueId());
        data.setPoints(mainData.getPoints());
        data.setToggled(mainData.isToggled());
        data.setSoundEnabled(mainData.isSoundEnabled());
        data.setFastest(mainData.getFastest());

        data.setWinsToday(challengeData.getWinsToday());
        data.setRewardClaimed(challengeData.isRewardClaimed());
        data.setLastUpdated(challengeData.getLastUpdated());
        data.setChallengeStreak(challengeData.getChallengeStreak());
        data.setDailyStreak(challengeData.getDailyStreak());
        data.setGlobalStreak(challengeData.getGlobalStreak());
        data.setWeeklyWins(challengeData.getWeeklyWins());

        data.resetDailyIfNeeded();
        return data;
    }

    public static void unload(OfflinePlayer player) {
        ChatGamesPlayer data = cache.remove(player.getUniqueId());
        if (data != null) {
            save(data);
        }
    }

    public static void save(ChatGamesPlayer data) {
        UUID uuid = data.getUuid();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ManagementQueries.updatePlayerData(uuid, data.getPoints(), data.isToggled(), data.isSoundEnabled(), data.getFastest());
            ChallengeQueries.updateChallengeProgress(uuid, data.getWinsToday(), data.isRewardClaimed(), data.getLastUpdated(),
                    data.getChallengeStreak(), data.getDailyStreak(), data.getGlobalStreak(), data.getWeeklyWins());
        });
    }

    public static void saveAll() {
        for (ChatGamesPlayer data : cache.values()) {
            save(data);
        }
    }
}