package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.chatgames.ChatGames;

/**
 * Utility class for handling version-related operations.
 * This class provides methods to retrieve the current Minecraft server version and perform
 * version-dependent logic.
 */
public class VersionUtils {

    private static ChatGames plugin;

    /**
     * Initializes the TaskUtils with the given plugin instance.
     *
     * @param plugin The plugin instance.
     */
    public static void initialize(ChatGames plugin) {
        VersionUtils.plugin = plugin;
    }

    /**
     * Retrieves the current Minecraft server version number (major version).
     * This method extracts the version number from the Bukkit version string.
     *
     * @return The major version number of the current Minecraft server (e.g., 1 for Minecraft 1.16).
     */
    public static int getMCVersion() {
        String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        String verNum = split[1];

        return Integer.parseInt(verNum);
    }

    /**
     * Corrects configuration settings based on the Minecraft version.
     * This method adjusts the configuration options for older Minecraft versions (e.g., version 8).
     *
     * @param config The main configuration file.
     * @param raceConfig The race-specific configuration file.
     */
    public void correctConfigs(FileConfiguration config, YamlConfiguration raceConfig) {
        if(getMCVersion() <= 8) {
            config.set("GameSounds.win", "ORB_PICKUP");
            config.set("GameSounds.game-start", "LEVEL_UP");
            config.set("GameSounds.time-expired", "VILLAGER_NO");
            Object codData = raceConfig.get("fish.data.COD");
            raceConfig.set("fish.data", null);
            raceConfig.set("fish.data.RAW_FISH", codData);
            plugin.saveConfig();
            plugin.getConfigManager().getChatRaces().save();
        }
    }
}
