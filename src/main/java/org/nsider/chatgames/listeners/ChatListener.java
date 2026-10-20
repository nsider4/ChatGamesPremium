package org.nsider.chatgames.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.Game;

public class ChatListener implements Listener {

    private final ChatGames plugin;

    public ChatListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Game game = plugin.getGameManager().getGame();
        // In a global game a visible answer could be copied by everyone, so it is always hidden
        boolean hide = (game != null && game.isGlobal()) || ConfigValue.getBoolean(ConfigPaths.CANCEL_MESSAGE_ON_WIN);
        if (plugin.getGameManager().getAnswerHandler().onChat(event.getPlayer(), event.getMessage()) && hide) {
            event.setCancelled(true);
        }
    }
}
