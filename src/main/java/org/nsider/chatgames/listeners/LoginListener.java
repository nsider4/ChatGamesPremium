package org.nsider.chatgames.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.PermissionKey;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.other.UpdateChecker;
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
            notifyUpdateOnce(p, plugin.getLatestVersion());
        }
    }

    /**
     * Tells an admin about a new version the first time they join after it came out, not on every join.
     * Queued after their data load on the same thread, so the stored mark is there to check.
     */
    private void notifyUpdateOnce(Player p, String version) {
        long key = UpdateChecker.versionKey(version);
        ChatGamesPlayerManager.runAfterSaves(() -> {
            ChatGamesPlayer data = ChatGamesPlayerManager.get(p.getUniqueId());
            if (data == null || data.getStat(Stat.UPDATE_NOTICE) == key) return;
            data.setStat(Stat.UPDATE_NOTICE, key);
            plugin.getScheduler().onPlayer(p, () -> MessageUtils.sendMessage(p, "&8[%prefix%&8] &fChatGames-Premium &e"
                    + version + " &fis available on Spigot &7(you have " + plugin.getDescription().getVersion()
                    + ")&f: &e" + plugin.getUpdateChecker().getResourceUrl()));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        ChatGamesPlayerManager.unload(e.getPlayer());
    }
}
