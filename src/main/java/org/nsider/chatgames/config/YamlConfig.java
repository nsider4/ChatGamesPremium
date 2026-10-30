package org.nsider.chatgames.config;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.nsider.chatgames.ChatGames;

/**
 * Represents a YAML configuration file for the ChatGames plugin.
 * It provides methods to load, save, and create the configuration file
 * as well as manage the default configuration resource.
 */
public class YamlConfig extends YamlConfiguration {

    @Getter private final File file;
    private final String path;
    private final ChatGames plugin;
    /** Why the last {@link #reload()} failed, or null if it worked. */
    @Getter private ConfigLoadError lastError;

    /**
     * Constructs a new YamlConfig instance.
     *
     * @param plugin the ChatGames plugin instance
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
     * Reloads the configuration file from disk. Always read as UTF-8: Bukkit 1.8-1.11
     * used the system charset, which on Windows turned symbols like ✓ into "âœ“".
     *
     * @return true if the file was successfully loaded, false otherwise
     */
    public boolean reload() {
        try (Reader reader = new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8)) {
            super.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            // The message says where the YAML is broken (line and column)
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load configuration file " + this.path + "! " + e.getMessage());
            this.lastError = ConfigLoadError.of(this.path, e);
            return false;
        }
        this.lastError = null;
        return true;
    }

    /**
     * Saves the current configuration to disk.
     *
     * @return true if the file was successfully saved, false otherwise
     */
    public boolean save() {
        try {
            Files.write(this.file.toPath(), saveToString().getBytes(StandardCharsets.UTF_8));
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
                boolean bundled;
                // getResource opens a stream; only whether there is one matters here
                try (InputStream resource = this.plugin.getResource(this.path)) {
                    bundled = resource != null;
                }
                if (bundled) {
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
