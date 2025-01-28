package org.nsider.chatgames.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all messages for the ElytraTweaks plugin. This includes loading
 * messages from configuration files, replacing placeholders, and retrieving
 * formatted messages.
 */
public class Messages {

    private final Map<MessageKey, String> messages = new HashMap<>();

    public Messages() {
        loadMessages();
    }

    /**
     * Get a message by key, or return the default if not found. Supports placeholder replacement.
     *
     * @param key The MessageKey for the desired message.
     * @param placeholders Optional placeholder-value pairs for replacement.
     * @return The formatted message string with placeholders replaced.
     */
    public String get(MessageKey key, String... placeholders) {
        String message = messages.getOrDefault(key, key.getDefaultValue());

        if (placeholders.length > 0) {
            message = replacePlaceholders(message, placeholders);
        }

        return message;
    }

    /**
     * Load all messages from their respective configuration files.
     */
    private void loadMessages() {
        for (MessageKey key : MessageKey.values()) {
            YamlConfig config = getConfig(key.getFileName());
            if (config == null) continue;
            String message = config.getString(key.getPath(), key.getDefaultValue());
            messages.put(key, MessageUtils.color(message));
        }
    }

    /**
     * Replaces placeholders in a message with corresponding values.
     *
     * @param message The original message containing placeholders.
     * @param placeholders A varargs array of placeholder-value pairs.
     * @return The message with placeholders replaced.
     * @throws IllegalArgumentException If an odd number of arguments is provided.
     */
    private String replacePlaceholders(String message, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be provided in pairs.");
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace(placeholder, value);
        }
        return message;
    }

    /**
     * Get the appropriate configuration file by name.
     *
     * @param fileName The name of the file to retrieve.
     * @return The YamlConfig object for the file, or null if not found.
     */
    private YamlConfig getConfig(String fileName) {
        switch (fileName.toLowerCase()) {
            case "messages":
                return ConfigManager.getMessagesConfig();
            case "GlobalElytraBlock":
                return ConfigManager.getGlobalElytraBlock();
            case "PerWorldElytra":
                return ConfigManager.getPerWorldElytra();
            case "FireworkBoost":
                return ConfigManager.getFireworkBoost();
            case "RiptideBoost":
                return ConfigManager.getRiptideBoost();
            default:
                return null;
        }
    }

    /**
     * Reload all message configurations.
     *
     * @return True if all configurations were reloaded successfully, false otherwise.
     */
    public boolean reloadMessages() {
        messages.clear();
        boolean success = ConfigManager.reload();

        if (success) {
            loadMessages();
        }

        return success;
    }
}
