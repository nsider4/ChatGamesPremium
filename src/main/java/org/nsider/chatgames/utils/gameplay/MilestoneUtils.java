package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.managers.MilestoneManager;

import java.util.List;
import java.util.UUID;

public final class MilestoneUtils {

    private MilestoneUtils() {}

    /**
     * Runs the commands of every milestone between the old and new point totals.
     * Safe to call from any thread: the commands are dispatched on the main thread
     * (the global region on Folia), where console commands must run.
     */
    public static void checkMilestones(UUID uuid, int previousPoints, int newPoints) {
        for (int milestone : MilestoneManager.getAllMilestones()) {
            if (previousPoints < milestone && newPoints >= milestone) {
                applyMilestone(uuid, milestone);
            }
        }
    }

    private static void applyMilestone(UUID uuid, int milestone) {
        List<String> commands = MilestoneManager.getCommandsForMilestone(milestone);
        if (commands == null || commands.isEmpty()) return;

        ChatGames.getPlugin(ChatGames.class).getScheduler().global(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
        });
    }
}
