package org.nsider.chatgames.config;

/**
 * Enum representing the different message keys used in the ChatGames plugin.
 * Each key corresponds to a message that can be retrieved from configuration files
 * or has a default message value. This is used to centralize all messages in the plugin.
 */
public enum MessageKey {
    CONFIG_RELOADED(
            "messages",
            "config_reloaded",
            "&cMessage has not been configured"
    ),
    CONFIG_RELOAD_FAILED(
            "messages",
            "config_reload_failed",
            "&cSome files failed to load; check the console. Those files keep their previous values."
    ),
    UNKNOWN_COMMAND(
            "messages",
            "unknown_command",
            "&cMessage has not been configured"
    ),
    NOT_ENOUGH_PERMISSIONS(
            "messages",
            "not_enough_permissions",
            "&cMessage has not been configured"
    ),
    CANNOT_START_DISABLED(
            "messages",
            "cannot_start_from_disabled",
            "&cMessage has not been configured"
    ),
    PLAYER_ONLY_COMMAND(
            "messages",
            "player_only_command",
            "&cMessage has not been configured"
    ),
    WRONG_CASE(
            "messages",
            "wrong_case",
            "&cMessage has not been configured"
    ),
    PLAYER_DELETED(
            "messages",
            "player_deleted",
            "&cMessage has not been configured"
    ),
    PLAYER_NOT_FOUND(
            "messages",
            "player_not_found",
            "&cMessage has not been configured"
    ),
    NUMBER_NOT_FOUND(
            "messages",
            "number_not_found_commands",
            "&cMessage has not been configured"
    ),
    POINTS_ADDED(
            "messages",
            "points_added",
            "&cMessage has not been configured"
    ),
    POINTS_REMOVED(
            "messages",
            "points_removed",
            "&cMessage has not been configured"
    ),
    POINTS_SET(
            "messages",
            "points_set",
            "&cMessage has not been configured"
    ),
    PLAYER_POINTS(
            "messages",
            "player_points",
            "&cMessage has not been configured"
    ),
    IN_COOLDOWN(
            "messages",
            "in_cooldown",
            "&cMessage has not been configured"
    ),
    GAME_STARTED(
            "messages",
            "game_started",
            "&cMessage has not been configured"
    ),
    TYPE_NOT_FOUND(
            "messages",
            "type_not_found",
            "&cMessage has not been configured"
    ),
    INVALID_ARGUMENTS(
            "messages",
            "invalid_arguments",
            "&cMessage has not been configured"
    ),
    CANT_LOG_LOCAL(
            "messages",
            "cant_log_local",
            "&cMessage has not been configured"
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
