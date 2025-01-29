package org.nsider.chatgames.games.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.ValueQueries;

import java.util.List;

public class GamePlayerManager {
    private final ChatGames plugin;

    public GamePlayerManager(ChatGames plugin) {
        this.plugin = plugin;
    }

    public boolean canPlay(Player player) {
        if (plugin.getGameConfigManager().isPermissionRequiredToPlay() && !player.hasPermission("chatgames.play")) {
            return false;
        }
        return ValueQueries.getPlayerToggle(player);
    }

    public boolean isWorldEnabled(Player player) {
        List<String> disabledWorlds = plugin.getGameConfigManager().getDisabledWorlds();
        return !disabledWorlds.contains(player.getWorld().getName());
    }

    public boolean checkMinimumPlayersOnline(int minimum) {
        int unableToPlay = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (MetadataValue meta : player.getMetadata("vanished")) {
                if (meta.asBoolean()) {
                    unableToPlay++;
                }
            }

            if (!plugin.getGameConfigManager().countAllPlayers() && !player.hasPermission("chatgames.play")) {
                unableToPlay++;
            }
        }

        return Bukkit.getOnlinePlayers().size() - unableToPlay >= minimum;
    }
}
