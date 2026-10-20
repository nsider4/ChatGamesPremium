package org.nsider.chatgames.config.settings;

import org.bukkit.configuration.file.FileConfiguration;
import org.nsider.chatgames.config.ConfigManager;

import java.util.function.Supplier;

/**
 * The plugin's configuration files.
 *
 * <p>Each constant resolves its file when asked rather than when the enum loads,
 * so {@link ConfigPaths} keeps working after {@code /chatgames reload} replaces
 * the underlying {@link FileConfiguration}.</p>
 */
public enum ConfigFile {

    CONFIG(ConfigManager::getConfig),
    MESSAGES(ConfigManager::getMessagesConfig),
    WORDS(ConfigManager::getWords),
    REWARDS(ConfigManager::getRewards),
    RACES(ConfigManager::getRaces),
    GAMES(ConfigManager::getGames),
    ADDONS(ConfigManager::getAddons),
    MILESTONES(ConfigManager::getMilestones),
    GLOBAL_GAMES(ConfigManager::getGlobalGames);

    private final Supplier<FileConfiguration> supplier;

    ConfigFile(Supplier<FileConfiguration> supplier) {
        this.supplier = supplier;
    }

    /**
     * @return the currently loaded file
     * @throws IllegalStateException if called before {@link ConfigManager#initialize}
     */
    public FileConfiguration get() {
        FileConfiguration file = supplier.get();
        if (file == null) {
            throw new IllegalStateException("Config file " + name() + " was read before ConfigManager.initialize()");
        }
        return file;
    }
}
