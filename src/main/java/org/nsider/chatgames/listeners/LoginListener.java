package org.nsider.chatgames.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.messages.MessageUtils;

/**
 * Loads a player's data when they join (and tells admins about updates); saves and drops it when they leave.
 */
public class LoginListener implements Listener {

    private final ChatGames plugin;

    public LoginListener(ChatGames instance) {
        plugin = instance;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Loads (or creates) the player's data off the main thread
        ChatGamesPlayerManager.load(p);

        // chatgames.admin defaults to op, and lets servers show it to staff without op too
        if (plugin.isUpdateNeeded() && p.hasPermission(PermissionKey.ADMIN.getPermission())) {
            MessageUtils.sendMessage(p, "&8[%prefix%&8] &fChatGames-Premium &e" + plugin.getLatestVersion()
                    + " &fis available on Spigot &7(you have " + plugin.getDescription().getVersion() + ")&f: &e"
                    + plugin.getUpdateChecker().getResourceUrl());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        ChatGamesPlayerManager.unload(e.getPlayer());
    }
}
