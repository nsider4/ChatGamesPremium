package org.nsider.chatgames.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.nsider.chatgames.ChatGames;

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

    private ConfigManager() {}

    /**
     * Loads every configuration file, then the messages built from them.
     * Must run before anything reads {@link org.nsider.chatgames.config.settings.ConfigPaths}.
     *
     * @param plugin The instance of the ChatGames plugin to be used for loading configurations.
     */
    public static void initialize(ChatGames plugin) {
        ConfigManager.plugin = plugin;
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

        // Messages read messages.yml, so they are built last
        messages = new Messages();
    }

    /**
     * Reloads all configuration files and the messages built from them.
     *
     * @return true if all configurations were reloaded successfully, false otherwise.
     */
    public static boolean reload() {
        plugin.reloadConfig();
        // reloadConfig() replaces the FileConfiguration instance
        config = plugin.getConfig();

        // Non-short-circuiting so one broken file doesn't stop the others reloading
        boolean success = messagesConfig.reload()
                & games.reload()
                & addons.reload()
                & words.reload()
                & rewards.reload()
                & races.reload()
                & milestones.reload()
                & globalGames.reload();

        messages = new Messages();
        return success;
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
}
