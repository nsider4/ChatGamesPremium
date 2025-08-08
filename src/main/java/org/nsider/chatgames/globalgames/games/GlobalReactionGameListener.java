package org.nsider.chatgames.globalgames.games;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;

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
        if (!plugin.isGlobalGameActive()) return;
        if (event.getMessage().equalsIgnoreCase(promptWord) && !participants.contains(event.getPlayer().getUniqueId())) {
            participants.add(event.getPlayer().getUniqueId());
            event.setCancelled(plugin.getConfig().getBoolean("cancel-message-when-winning"));
        }
    }
}
