package org.nsider.chatgames.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.ManagementQueries;
import org.nsider.chatgames.utils.messages.MessageUtils;

public class LoginListener implements Listener {

    private final ChatGames plugin;

    public LoginListener(ChatGames instance) {
        plugin = instance;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if(!ManagementQueries.playerExists(p)) {
            ManagementQueries.createPlayer(p, 0, success -> {
                if(!success) {
                    plugin.getLogger().warning("Error while trying to create player: " + p.getName());
                }
            });
        }

        if(p.isOp() && plugin.isUpdateNeeded()) {
            MessageUtils.sendMessage(p, "&8[%prefix%&8] &fThere is a new update available on Spigot!");
        }
    }
}
