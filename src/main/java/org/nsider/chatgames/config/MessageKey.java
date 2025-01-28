package org.nsider.chatgames.config;

/**
 * Enum representing the different message keys used in the ElytraTweaks plugin.
 * Each key corresponds to a message that can be retrieved from configuration files
 * or has a default message value. This is used to centralize all messages in the plugin.
 */
public enum MessageKey {
    CONFIG_RELOADED(
            "messages",
            "config_reloaded",
            "&aConfiguration reloaded successfully."
    ),
    SELECTION_NONE_TO_CLEAR(
            "regions/RegionConfig",
            "messages.selection_none_to_clear.message",
            "&eYou have no selection to clear."
    );

    private final String fileName;
    private final String path;
    private final String defaultValue;

    /**
     * Constructor for the MessageKey enum, defining the file name, path, and default value for the message.
     *
     * @param fileName The name of the file where the message is stored.
     * @param path The path within the file to retrieve the message.
     * @param defaultValue The default message value if the message cannot be found in the file.
     */
    MessageKey(String fileName, String path, String defaultValue) {
        this.fileName = fileName;
        this.path = path;
        this.defaultValue = defaultValue;
    }

    /**
     * Retrieves the file name associated with this message key.
     *
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Retrieves the path within the file associated with this message key.
     *
     * @return The path to the message.
     */
    public String getPath() {
        return path;
    }

    /**
     * Retrieves the default value for the message associated with this key.
     *
     * @return The default message.
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
