package org.nsider.chatgames.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.config.ConfigManager;

import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * milestones.yml: the commands run when a player's points reach a milestone.
 */
public final class MilestoneManager {

    // Read by chat threads when points change; a reload builds a new map and swaps it in, so a
    // reader never sees it half filled
    private static volatile NavigableMap<Integer, List<String>> milestones = new ConcurrentSkipListMap<>();

    private MilestoneManager() {}

    public static void loadMilestones() {
        NavigableMap<Integer, List<String>> loaded = new ConcurrentSkipListMap<>();
        ConfigurationSection section = ConfigManager.getMilestones().getConfigurationSection("milestones");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    loaded.put(Integer.parseInt(key), section.getStringList(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        milestones = loaded;
    }

    public static List<String> getCommandsForMilestone(int points) {
        return milestones.get(points);
    }

    /** @return the milestone point values, lowest first */
    public static Set<Integer> getAllMilestones() {
        return milestones.keySet();
    }
}
