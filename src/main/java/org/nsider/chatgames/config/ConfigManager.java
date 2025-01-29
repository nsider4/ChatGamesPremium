package org.nsider.chatgames.config;

import lombok.Getter;
import org.nsider.chatgames.ChatGames;

/**
 * Manages all configuration files and messages for the ElytraTweaks plugin.
 * This class is responsible for initializing and reloading configuration files,
 * as well as providing access to various configuration data used throughout the plugin.
 */
public class ConfigManager {

    private static ChatGames plugin;

    @Getter private static Messages messages;

    @Getter private static MessagesConfig MessagesConfig;
    @Getter private static Words Words;
    @Getter private static Rewards Rewards;
    @Getter private static ChatRaces ChatRaces;


    /**
     * Initializes the configuration manager and loads the configuration files.
     *
     * @param plugin The instance of the ElytraTweaks plugin to be used for loading configurations.
     */
    public static void initialize(ChatGames plugin) {
        ConfigManager.plugin = plugin;
        messages = new Messages();

        MessagesConfig = new MessagesConfig();
        Words = new Words();
        Rewards = new Rewards();
        ChatRaces = new ChatRaces();
    }

    /**
     * Reloads all configuration files.
     *
     * @return true if all configurations were reloaded successfully, false otherwise.
     */
    public static boolean reload() {
        plugin.reloadConfig();
        return MessagesConfig.reload()
                && Words.reload()
                && Rewards.reload()
                && ChatRaces.reload();
    }


    //Nested static classes for each configuration file

    /**
     * Configuration file for messages used in the plugin.
     */
    public static class Words extends YamlConfig {
        public Words() {
            super(plugin, "words");
        }
    }


    /**
     * Configuration file for global Elytra block settings.
     */
    public static class Rewards extends YamlConfig {
        public Rewards() {
            super(plugin, "rewards");
        }
    }

    /**
     * Configuration file for per-world Elytra settings.
     */
    public static class MessagesConfig extends YamlConfig {
        public MessagesConfig() {
            super(plugin, "messages");
        }
    }

    /**
     * Configuration file for firework boost settings.
     */
    public static class ChatRaces extends YamlConfig {
        public ChatRaces() {
            super(plugin, "chatRaces");
        }
    }
}
