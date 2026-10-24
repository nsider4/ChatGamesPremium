package org.nsider.chatgames.games.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.PermissionKey;

/**
 * Counts the players online, for the rotation's and the global games' minimum player settings.
 */
public class GamePlayerManager {
    private final ChatGames plugin;

    public GamePlayerManager(ChatGames plugin) {
        this.plugin = plugin;
    }

    public int getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    /**
     * @return true if at least {@code minimum} players can play: vanished players don't count, nor
     *         (unless count-all-players is on) those without {@code chatgames.play}
     */
    public boolean checkMinimumPlayersOnline(int minimum) {
        boolean countAll = plugin.getGameConfigManager().countAllPlayers();
        int able = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Each player counts at most once, however many plugins mark them vanished
            if (isVanished(player)) continue;
            if (!countAll && !player.hasPermission(PermissionKey.PLAY.getPermission())) continue;
            able++;
        }
        return able >= minimum;
    }

    private static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
