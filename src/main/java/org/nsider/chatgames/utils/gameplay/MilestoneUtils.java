package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.managers.MilestoneManager;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.Stat;

import java.util.List;
import java.util.UUID;

/**
 * Runs milestone commands. A milestone is first stored on the player as pending and
 * then paid out while they are online, so points given to an offline player (by an
 * admin command) still pay their milestones when that player next joins.
 */
public final class MilestoneUtils {

    private MilestoneUtils() {}

    /**
     * Marks every milestone between the old and new point totals as pending, then pays
     * them out if the player is online. Safe to call from any thread.
     */
    public static void checkMilestones(ChatGamesPlayer data, int previousPoints, int newPoints) {
        boolean crossed = false;
        for (int milestone : MilestoneManager.getAllMilestones()) {
            if (previousPoints < milestone && newPoints >= milestone) {
                data.setStat(Stat.milestonePending(milestone), 1);
                crossed = true;
            }
        }
        if (crossed) payPendingLater(data.getUuid());
    }

    /**
     * Pays the player's pending milestones on the main thread (the global region on
     * Folia, where console commands must run). Does nothing if they are offline.
     */
    public static void payPendingLater(UUID uuid) {
        ChatGames.getPlugin(ChatGames.class).getScheduler().global(() -> payPending(uuid));
    }

    private static void payPending(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        ChatGamesPlayer data = ChatGamesPlayerManager.get(uuid);
        if (player == null || data == null) return;

        for (String stat : data.getStatNames(Stat.MILESTONE_PENDING_PREFIX)) {
            if (data.getStat(stat) <= 0) continue;
            data.setStat(stat, 0);
            List<String> commands = MilestoneManager.getCommandsForMilestone(Stat.milestoneOf(stat));
            if (commands == null) continue;
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
        }
    }
}
