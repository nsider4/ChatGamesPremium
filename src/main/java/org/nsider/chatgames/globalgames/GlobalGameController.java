package org.nsider.chatgames.globalgames;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.globalgames.games.GlobalReactionGameListener;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlobalGameController {
    private final ChatGames plugin;
    private final Set<UUID> participants = new HashSet<>();
    private BukkitRunnable listenerRemover;

    public GlobalGameController(ChatGames plugin) {
        this.plugin = plugin;
    }

    public void startGlobalWordGame(long durationSeconds, String promptWord) {
        participants.clear();
        broadcast("&eGlobal Game starting! Type &b" + promptWord + " &ein chat within " + durationSeconds + "s to join!");

        GlobalReactionGameListener listener = new GlobalReactionGameListener(plugin, promptWord, participants);
        Bukkit.getPluginManager().registerEvents(listener, plugin);

        listenerRemover = new BukkitRunnable() {
            @Override
            public void run() {
                HandlerList.unregisterAll(listener);
                concludeGlobalGame();
            }
        };
        listenerRemover.runTaskLater(plugin, durationSeconds * 20L);
    }

    private void concludeGlobalGame() {
        //TODO CHANGE TO NORMAL REWARDS
        participants.forEach(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) plugin.getGameUtils().giveRewards("global_word", p);
        });
        broadcast("&aGlobal Game ended! Participants rewarded: " + participants.size());
    }

    private void broadcast(String message) {//TODO CHANGE THIS
        plugin.getServer().broadcastMessage(MessageUtils.color(message));
    }
}

