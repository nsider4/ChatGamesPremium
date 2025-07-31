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
    @Getter private static MessagesConfig MessagesConfig;
    @Getter private static Words Words;
    @Getter private static Rewards Rewards;
    @Getter private static Races Races;
    @Getter private static Games Games;
    @Getter private static Addons Addons;
    @Getter private static Milestones Milestones;


    /**
     * Initializes the configuration manager and loads the configuration files.
     *
     * @param plugin The instance of the ChatGames plugin to be used for loading configurations.
     */
    public static void initialize(ChatGames plugin) {
        ConfigManager.plugin = plugin;
        messages = new Messages();

        config = plugin.getConfig();
        MessagesConfig = new MessagesConfig();
        Words = new Words();
        Rewards = new Rewards();
        Races = new Races();
        Games = new Games();
        Addons = new Addons();
        Milestones = new Milestones();
    }

    /**
     * Reloads all configuration files.
     *
     * @return true if all configurations were reloaded successfully, false otherwise.
     */
    public static boolean reload() {
        plugin.reloadConfig();
        return MessagesConfig.reload()
                && Games.reload()
                && Addons.reload()
                && Words.reload()
                && Rewards.reload()
                && Races.reload()
                && Milestones.reload();
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
}
