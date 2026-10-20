package org.nsider.chatgames.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;

public class ChatListener implements Listener {

    private final ChatGames plugin;

    public ChatListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getGameManager().getAnswerHandler().onChat(event.getPlayer(), event.getMessage())
                && ConfigValue.getBoolean(ConfigPaths.CANCEL_MESSAGE_ON_WIN)) {
            event.setCancelled(true);
        }
    }
}
