package org.nsider.chatgames.model.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.ChallengeQueries;
import org.nsider.chatgames.database.queries.FastestTimeQueries;
import org.nsider.chatgames.database.queries.ManagementQueries;
import org.nsider.chatgames.model.top.FastestData;
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
        plugin.getScheduler().async(() -> {
            if (!ManagementQueries.playerExists(player)) {
                ManagementQueries.createPlayerIfNotExists(player);
            }

            PlayerMainData mainData = ManagementQueries.getPlayerData(player);
            if(mainData == null) return;
            ChallengeData challengeData = ChallengeQueries.getChallengeData(player);
            if(challengeData == null) return;
            FastestData fastestData = FastestTimeQueries.getFastestData(player);
            ChatGamesPlayer data = getChatGamesPlayer(player, mainData, challengeData, fastestData);

            cache.put(player.getUniqueId(), data);
        });
    }

    private static @Nonnull ChatGamesPlayer getChatGamesPlayer(OfflinePlayer player, PlayerMainData mainData, ChallengeData challengeData, FastestData fastestData) {
        ChatGamesPlayer data = new ChatGamesPlayer(player.getUniqueId());
        data.setPoints(mainData.getPoints());
        data.setToggled(mainData.isToggled());
        data.setSoundEnabled(mainData.isSoundEnabled());

        data.setWinsToday(challengeData.getWinsToday());
        data.setRewardClaimed(challengeData.isRewardClaimed());
        data.setLastUpdated(challengeData.getLastUpdated());
        data.setChallengeStreak(challengeData.getChallengeStreak());
        data.setDailyStreak(challengeData.getDailyStreak());
        data.setGlobalStreak(challengeData.getGlobalStreak());
        data.setWeeklyWins(challengeData.getWeeklyWins());

        data.setFastestData(fastestData);

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
        plugin.getScheduler().async(() -> write(data));
    }

    private static void write(ChatGamesPlayer data) {
        UUID uuid = data.getUuid();
        ManagementQueries.updatePlayerData(uuid, data.getPoints(), data.isToggled(), data.isSoundEnabled());
        ChallengeQueries.updateChallengeProgress(uuid, data.getWinsToday(), data.isRewardClaimed(), data.getLastUpdated(),
                data.getChallengeStreak(), data.getDailyStreak(), data.getGlobalStreak(), data.getWeeklyWins());
    }

    /**
     * Saves every cached player in the background. Used by the periodic autosave.
     */
    public static void saveAll() {
        for (ChatGamesPlayer data : cache.values()) {
            save(data);
        }
    }

    /**
     * Saves every cached player on the calling thread and empties the cache.
     * Only for shutdown: the scheduler no longer runs tasks once the plugin is disabling.
     */
    public static void saveAllNow() {
        for (ChatGamesPlayer data : cache.values()) {
            write(data);
        }
        cache.clear();
    }

    /**
     * Loads everyone already online, for when the plugin is enabled or reloaded mid-game.
     */
    public static void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            load(player);
        }
    }
}