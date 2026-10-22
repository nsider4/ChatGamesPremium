package org.nsider.chatgames.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.api.event.ChatGamesPointsChangeEvent;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.gameplay.MilestoneUtils;

import java.util.UUID;

/**
 * Points and fastest times. Works on cached data for online players; changes to
 * offline players are loaded, applied and saved in the background.
 */
public final class TopManager {

    private TopManager() {}

    /**
     * @return the player's points, or 0 if their data isn't loaded
     */
    public static int getPoints(OfflinePlayer player) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        return data != null ? data.getPoints() : 0;
    }

    /**
     * Adds points (negative to remove) and runs any milestones crossed on the way up,
     * straight away or, for an offline player, when they next join.
     */
    public static void addPoints(OfflinePlayer player, int amount) {
        UUID uuid = player.getUniqueId();
        ChatGamesPlayerManager.modify(uuid, data -> {
            int before = data.getPoints();
            int after = data.addPoints(amount);
            MilestoneUtils.checkMilestones(data, before, after);
            if (after != before) Bukkit.getPluginManager().callEvent(new ChatGamesPointsChangeEvent(uuid, before, after));
        });
    }

    /**
     * Sets points to an exact value. Milestones are not run: this is an admin correction.
     */
    public static void setPoints(OfflinePlayer player, int points) {
        ChatGamesPlayerManager.modify(player.getUniqueId(), data -> {
            int before = data.getPoints();
            data.setPoints(points);
            if (points != before) Bukkit.getPluginManager().callEvent(new ChatGamesPointsChangeEvent(player.getUniqueId(), before, points));
        });
    }

    /**
     * Records a winning time for an online player.
     *
     * @return true if it was a new personal best for that game
     */
    public static boolean recordTime(OfflinePlayer player, GameType game, double seconds) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        return data != null && data.recordTime(game, seconds);
    }
}
