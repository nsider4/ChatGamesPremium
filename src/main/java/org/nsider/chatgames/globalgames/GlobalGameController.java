package org.nsider.chatgames.globalgames;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.globalgames.games.GlobalReactionGameListener;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalGameController {
    private final ChatGames plugin;
    // Chat events arrive on many threads at once
    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();
    private ChatGamesScheduler.Task listenerRemover;
    private GlobalReactionGameListener listener;
    @Getter private volatile boolean active;

    public GlobalGameController(ChatGames plugin) {
        this.plugin = plugin;
    }

    public void startGlobalWordGame(long durationSeconds, String promptWord) {
        participants.clear();
        active = true;
        broadcast("&eGlobal Game starting! Type &b" + promptWord + " &ein chat within " + durationSeconds + "s to join!");

        listener = new GlobalReactionGameListener(plugin, promptWord, participants);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        listenerRemover = plugin.getScheduler().globalLater(() -> {
            HandlerList.unregisterAll(listener);
            concludeGlobalGame();
        }, durationSeconds * 20L);
    }

    /**
     * Ends a running global game without rewards, e.g. when the plugin is disabled.
     */
    public void stop() {
        if (!active) return;
        active = false;
        ChatGamesScheduler.cancel(listenerRemover);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        participants.clear();
    }

    private void concludeGlobalGame() {
        active = false;
        //TODO CHANGE TO NORMAL REWARDS
        participants.forEach(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) plugin.getGameUtils().giveRewards("global_word", p, 1);
        });
        broadcast("&aGlobal Game ended! Participants rewarded: " + participants.size());
    }

    private void broadcast(String message) {//TODO CHANGE THIS
        plugin.getServer().broadcastMessage(MessageUtils.color(message));
    }
}

