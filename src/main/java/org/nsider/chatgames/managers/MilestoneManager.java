package org.nsider.chatgames.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class MilestoneManager {

    // Read by chat threads when points change, rewritten on reload
    private static final Map<Integer, List<String>> milestones = new ConcurrentSkipListMap<>();

    public static void loadMilestones() {
        milestones.clear();

        ConfigurationSection section = ConfigManager.getMilestones().getConfigurationSection("milestones");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                int points = Integer.parseInt(key);
                List<String> commands = section.getStringList(key);
                milestones.put(points, commands);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static List<String> getCommandsForMilestone(int points) {
        return milestones.get(points);
    }

    public static Set<Integer> getAllMilestones() {
        return milestones.keySet();
    }
}