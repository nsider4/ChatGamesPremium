package org.nsider.chatgames.globalgames.games;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;

import java.util.Set;
import java.util.UUID;

public class GlobalReactionGameListener implements Listener { //TODO REGISTER LISTENER
    private final String promptWord;
    private final Set<UUID> participants;
    private final ChatGames plugin;

    public GlobalReactionGameListener(ChatGames plugin, String promptWord, Set<UUID> participants) {
        this.plugin = plugin;
        this.promptWord = promptWord;
        this.participants = participants;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getGlobalGameController().isActive()) return;
        if (event.getMessage().equalsIgnoreCase(promptWord) && !participants.contains(event.getPlayer().getUniqueId())) {
            participants.add(event.getPlayer().getUniqueId());
            event.setCancelled(ConfigValue.getBoolean(ConfigPaths.CANCEL_MESSAGE_ON_WIN));
        }
    }
}
