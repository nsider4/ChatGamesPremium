package org.nsider.chatgames.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.messages.MessageUtils;

/**
 * Loads a player's data when they join (and tells operators about updates); saves and drops it when they leave.
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

        if(p.isOp() && plugin.isUpdateNeeded()) {
            MessageUtils.sendMessage(p, "&8[%prefix%&8] &fThere is a new update available on Spigot!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        ChatGamesPlayerManager.unload(e.getPlayer());
    }
}
