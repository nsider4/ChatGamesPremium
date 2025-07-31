package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.managers.MilestoneManager;

import java.util.List;
import java.util.UUID;

public class MilestoneUtils {

    public static void checkMilestones(UUID uuid, int previousPoints, int newPoints) {
        for (int milestone : MilestoneManager.getAllMilestones()) {
            if (previousPoints < milestone && newPoints >= milestone) {
                applyMilestone(uuid, milestone);
            }
        }
    }

    private static void applyMilestone(UUID uuid, int milestone) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        List<String> commands = MilestoneManager.getCommandsForMilestone(milestone);
        if (commands != null) {
            for (String cmd : commands) {
                String parsed = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }//TODO CHANGE THIS FOR THE COMMAND UTILS
    }
}
