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
    ),
    TOGGLE_GAME(
            "messages",
            "toggle_game",
            "&8[%prefix%&8] &fYou have toggled &a%state% &fchat events!"
    ),
    TOGGLE_GAME_OTHER(
            "messages",
            "toggle_game_other",
            "&8[%prefix%&8] &fYou have toggled &a%state% &fchat events for &c%player%&f!"
    ),
    TOGGLE_SOUND(
            "messages",
            "toggle_sound",
            "&8[%prefix%&8] &fYou have toggled &a%state% &fchat game sounds!"
    ),
    TOGGLE_SOUND_OTHER(
            "messages",
            "toggle_sound_other",
            "&8[%prefix%&8] &fYou have toggled &a%state% &fchat game sounds for &c%player%&f!"
    ),
    TOGGLE_ALL(
            "messages",
            "toggleAllGames",
            "&8[%prefix%&8] &fYou have toggled &a%state% &cALL &fchatgames!"
    ),
    STATE_ON(
            "messages",
            "state-ON",
            "ON"
    ),
    STATE_OFF(
            "messages",
            "state-OFF",
            "OFF"
    ),
    PLAYER_POINTS_OTHER(
            "messages",
            "player_points_other",
            "&8[%prefix%&8] &c%player% &fhas &a%amount% &fpoints!"
    ),
    DATA_LOADING(
            "messages",
            "data_loading",
            "&8[%prefix%&8] &cThat player's data is still loading, try again in a moment."
    ),
    USAGE(
            "messages",
            "usage",
            "&8[%prefix%&8] &cUsage: &f%usage%"
    ),
    GAME_ALREADY_RUNNING(
            "messages",
            "game_already_running",
            "&8[%prefix%&8] &cA chat game is already running! Use &f/chatgames stop &cto end it first."
    ),
    GAME_NOT_STARTED(
            "messages",
            "game_not_started",
            "&8[%prefix%&8] &cThe %game% game couldn't start with those values. Check the console for details."
    ),
    NO_GAMES_ENABLED(
            "messages",
            "no_games_enabled",
            "&8[%prefix%&8] &cNo games are enabled at the moment."
    ),
    GAME_STOPPED(
            "messages",
            "game_stopped",
            "&8[%prefix%&8] &fThe current game has been stopped with no winner."
    ),
    NO_GAME_RUNNING(
            "messages",
            "no_game_running",
            "&8[%prefix%&8] &cNo chat game is running right now."
    ),
    FASTEST_DISABLED(
            "messages",
            "TypersTop.top_disabled_message",
            "&8[%prefix%&8] &cThis function is currently disabled!"
    ),
    UNKNOWN_GAME(
            "messages",
            "unknown_game",
            "&8[%prefix%&8] &cThere is no game called &f%game%&c."
    ),
    LOGS_HEADER(
            "messages",
            "logs.header",
            "&8[%prefix%&8] &eLast &6%amount% &elogs:"
    ),
    LOGS_EMPTY(
            "messages",
            "logs.no_logs",
            "&cNo logs available or an error occurred."
    ),
    PRESET_LOADED(
            "messages",
            "presets.loaded",
            "&8[%prefix%&8] &fLoaded the preset &a%preset% &f(%games%)."
    ),
    PRESET_CLEARED(
            "messages",
            "presets.cleared",
            "&8[%prefix%&8] &fNo preset is active any more; the games enabled in games.yml are used."
    ),
    PRESET_SAVED(
            "messages",
            "presets.saved",
            "&8[%prefix%&8] &fSaved the enabled games as the preset &a%preset% &f(%games%)."
    ),
    PRESET_NOT_FOUND(
            "messages",
            "presets.not_found",
            "&8[%prefix%&8] &cThere is no preset called &f%preset%&c."
    ),
    PRESET_INVALID_NAME(
            "messages",
            "presets.invalid_name",
            "&8[%prefix%&8] &cPreset names can only use letters, numbers, - and _ (up to 32)."
    ),
    PRESET_LIST_HEADER(
            "messages",
            "presets.list_header",
            "&8[%prefix%&8] &ePresets:"
    ),
    PRESET_LIST_ENTRY(
            "messages",
            "presets.list_entry",
            "&7- &f%preset%%active% &8(%games%)"
    ),
    PRESET_ACTIVE_MARKER(
            "messages",
            "presets.active_marker",
            " &a(active)"
    ),
    PRESET_LIST_EMPTY(
            "messages",
            "presets.list_empty",
            "&8[%prefix%&8] &cNo presets are configured in presets.yml."
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
