package org.nsider.chatgames.config;

import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nsider.chatgames.ChatGames;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all configuration files and messages for the ChatGames plugin.
 * This class is responsible for initializing and reloading configuration files,
 * as well as providing access to various configuration data used throughout the plugin.
 */
public class ConfigManager {

    private static ChatGames plugin;

    @Getter private static Messages messages;
    @Getter private static FileConfiguration config;
    @Getter private static MessagesConfig messagesConfig;
    @Getter private static Words words;
    @Getter private static Rewards rewards;
    @Getter private static Races races;
    @Getter private static Games games;
    @Getter private static Addons addons;
    @Getter private static Milestones milestones;
    @Getter private static GlobalGames globalGames;
    @Getter private static Presets presets;
    @Getter private static Challenges challenges;
    @Getter private static Streaks streaks;
    @Getter private static Events events;
    @Getter private static Menus menus;
    @Getter private static Discord discord;

    private ConfigManager() {}

    /**
     * Loads every configuration file, then the messages built from them.
     * Must run before anything reads {@link org.nsider.chatgames.config.settings.ConfigPaths}.
     *
     * @param plugin The instance of the ChatGames plugin to be used for loading configurations.
     */
    public static void initialize(ChatGames plugin) {
        ConfigManager.plugin = plugin;
        migrateFrom199(plugin);
        addNewSections(plugin);
        moveDiscordSettings(plugin);
        plugin.saveDefaultConfig();

        config = plugin.getConfig();
        messagesConfig = new MessagesConfig();
        words = new Words();
        rewards = new Rewards();
        races = new Races();
        games = new Games();
        addons = new Addons();
        milestones = new Milestones();
        globalGames = new GlobalGames();
        presets = new Presets();
        challenges = new Challenges();
        streaks = new Streaks();
        events = new Events();
        menus = new Menus();
        discord = new Discord();

        // Messages read messages.yml, so they are built last
        messages = new Messages();
    }

    private static void migrateFrom199(ChatGames plugin) {
        if (!LegacyMigration.isNeeded(plugin.getDataFolder())) return;
        try {
            LegacyMigration.Result result = LegacyMigration.migrate(plugin.getDataFolder(), plugin::getResource);
            StringBuilder carried = new StringBuilder();
            for (Map.Entry<String, Integer> entry : result.carried.entrySet()) {
                if (entry.getValue() > 0) carried.append(carried.length() == 0 ? "" : ", ").append(entry.getKey()).append(" (").append(entry.getValue()).append(')');
            }
            if (result.rewardsKept) carried.append(carried.length() == 0 ? "" : ", ").append("rewards.yml (kept)");
            plugin.getLogger().log(Level.INFO, "Upgraded the 1.9.9 configuration. Your changes went to: {0}. The old files are in {1}.",
                    new Object[]{carried.length() == 0 ? "nothing (all defaults)" : carried, result.backup.getPath()});
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not upgrade the 1.9.9 configuration; the old files are in backups/.", e);
        }
    }

    private static void moveDiscordSettings(ChatGames plugin) {
        try {
            List<String> moved = DiscordUpgrade.run(plugin.getDataFolder(), plugin::getResource);
            if (!moved.isEmpty()) plugin.getLogger().log(Level.INFO, "Moved the Discord settings to discord.yml: {0}", String.join(", ", moved));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not move the Discord settings to discord.yml; set the webhook there again.", e);
        }
    }

    private static void addNewSections(ChatGames plugin) {
        try {
            List<String> added = BuiltInSections.addMissing(plugin.getDataFolder(), plugin::getResource);
            if (!added.isEmpty()) plugin.getLogger().log(Level.INFO, "Added the new sections to your config files: {0}", String.join(", ", added));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not add the new races to your config files; they stay off until their sections are added.", e);
        }
    }

    /**
     * Reloads all configuration files and the messages built from them.
     *
     * @return the files that failed to load (they keep their previous values); empty if all loaded
     */
    public static List<ConfigLoadError> reload() {
        List<ConfigLoadError> errors = new ArrayList<>();
        ConfigLoadError configError = checkConfigYml();
        if (configError == null) {
            plugin.reloadConfig();
            // reloadConfig() replaces the FileConfiguration instance
            config = plugin.getConfig();
        } else {
            errors.add(configError);
        }

        // Every file is tried, so one broken file doesn't stop the others reloading
        for (YamlConfig file : new YamlConfig[]{messagesConfig, games, addons, words, rewards, races, milestones,
                globalGames, presets, challenges, streaks, events, menus, discord}) {
            if (!file.reload()) errors.add(file.getLastError());
        }

        messages = new Messages();
        return errors;
    }

    /**
     * Bukkit's reloadConfig() swallows a broken config.yml and carries on with every setting at its
     * default, so it is parsed here first and only reloaded if it is valid.
     *
     * @return why config.yml can't be loaded, or null if it can
     */
    private static ConfigLoadError checkConfigYml() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            new YamlConfiguration().load(reader);
            return null;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration file config.yml! " + e.getMessage());
            return ConfigLoadError.of("config.yml", e);
        }
    }


    //Nested static classes for each configuration file

    /**
     * Configuration file for words used in each word game
     */
    public static class Words extends YamlConfig {
        public Words() {
            super(plugin, "words");
        }
    }

    /**
     * Configuration file for game settings
     */
    public static class Games extends YamlConfig {
        public Games() {
            super(plugin, "games");
        }
    }

    /**
     * Configuration file for additional features
     */
    public static class Addons extends YamlConfig {
        public Addons() {
            super(plugin, "add-ons");
        }
    }

    /**
     * Configuration file for rewards
     */
    public static class Rewards extends YamlConfig {
        public Rewards() {
            super(plugin, "rewards");
        }
    }

    /**
     * Configuration file for plugin messages
     */
    public static class MessagesConfig extends YamlConfig {
        public MessagesConfig() {
            super(plugin, "messages");
        }
    }

    /**
     * Configuration file for race settings
     */
    public static class Races extends YamlConfig {
        public Races() {
            super(plugin, "races");
        }
    }

    /**
     * Configuration file for milestones
     */
    public static class Milestones extends YamlConfig {
        public Milestones() {
            super(plugin, "milestones");
        }
    }

    /**
     * Configuration file for global games
     */
    public static class GlobalGames extends YamlConfig {
        public GlobalGames() {
            super(plugin, "global_games");
        }
    }

    /**
     * Game presets and the active one. The plugin writes this file itself
     * ({@code /chatgames preset}), which is why it is apart from config.yml.
     */
    public static class Presets extends YamlConfig {
        public Presets() {
            super(plugin, "presets");
            options().copyHeader(true);
        }
    }

    /**
     * Daily and weekly challenges
     */
    public static class Challenges extends YamlConfig {
        public Challenges() {
            super(plugin, "challenges");
        }
    }

    /**
     * The streak reward ladders
     */
    public static class Streaks extends YamlConfig {
        public Streaks() {
            super(plugin, "streaks");
        }
    }

    /**
     * Game events: series of games with their own scoring
     */
    public static class Events extends YamlConfig {
        public Events() {
            super(plugin, "events");
        }
    }

    /**
     * Menu layouts: titles, slots and items
     */
    public static class Menus extends YamlConfig {
        public Menus() {
            super(plugin, "menus");
        }
    }

    public static class Discord extends YamlConfig {
        public Discord() {
            super(plugin, "discord");
        }
    }
}
