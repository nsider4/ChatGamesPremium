package org.nsider.chatgames.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.config.ConfigManager;

import java.util.*;

public class MilestoneManager {

    private static final Map<Integer, List<String>> milestones = new TreeMap<>();

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