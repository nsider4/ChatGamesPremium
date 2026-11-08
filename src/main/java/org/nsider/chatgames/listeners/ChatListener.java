package org.nsider.chatgames.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.games.round.AnswerChecker;
import org.nsider.chatgames.globalgames.GlobalGameManager;

/**
 * Hands chat to the global game in play, or to the running chat game as an answer. Async chat thread.
 *
 * <p>Runs at LOWEST so it reads the message as typed: chat plugins (EssentialsX Chat, ColorChat,
 * resource-pack glyph plugins) rewrite it at later priorities. Cancelled messages still count,
 * since some chat plugins cancel every message and send their own.</p>
 */
public class ChatListener implements Listener {

    private final ChatGames plugin;

    public ChatListener(ChatGames instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        String message = AnswerChecker.plain(event.getMessage());
        GlobalGameManager globals = plugin.getGlobalGameManager();
        if (globals.isRunning()) {
            // A visible answer could be copied by everyone, and counted numbers are shown again formatted
            if (globals.onChat(event.getPlayer(), message)) event.setCancelled(true);
            return;
        }
        if (plugin.getGameManager().getAnswerHandler().onChat(event.getPlayer(), message)
                && ConfigValue.getBoolean(ConfigPaths.CANCEL_MESSAGE_ON_WIN)) {
            event.setCancelled(true);
        }
    }
}
