package org.nsider.chatgames.config;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.nsider.chatgames.ChatGames;

/**
 * Represents a YAML configuration file for the ElytraTweaks plugin.
 * It provides methods to load, save, and create the configuration file
 * as well as manage the default configuration resource.
 */
public class YamlConfig extends YamlConfiguration {

    @Getter private final File file;
    private final String path;
    private final ChatGames plugin;

    /**
     * Constructs a new YamlConfig instance.
     *
     * @param plugin the ElytraTweaks plugin instance
     * @param path the path to the configuration file (without the extension)
     */
    public YamlConfig(ChatGames plugin, String path) {
        this.plugin = plugin;
        this.path = path + ".yml";
        this.file = new File(plugin.getDataFolder(), this.path);
        this.saveDefault();
        this.reload();
    }

    /**
     * Reloads the configuration file from disk.
     *
     * @return true if the file was successfully loaded, false otherwise
     */
    public boolean reload() {
        try {
            super.load(this.file);
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load configuration file {0}!", this.path);
            return false;
        }
        return true;
    }

    /**
     * Saves the current configuration to disk.
     *
     * @return true if the file was successfully saved, false otherwise
     */
    public boolean save() {
        try {
            super.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save configuration file {0}!", this.path);
            return false;
        }
        return true;
    }

    /**
     * Saves the default configuration file if it does not already exist.
     * If the file does not exist, it will either copy the resource from the plugin's jar
     * or create a new file if no resource is available.
     */
    public void saveDefault() {
        try {
            if (!this.file.exists()) {
                if (this.plugin.getResource(this.path) != null) {
                    this.plugin.saveResource(this.path, false);
                } else {
                    this.file.createNewFile();
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to create configuration file {0}!", this.path);
        }
    }
}
