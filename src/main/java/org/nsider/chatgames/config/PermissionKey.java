package org.nsider.chatgames.config;

/**
 * Enum representing plugin permissions with their descriptions and default values.
 */
public enum PermissionKey {
    START_TYPE(
            "chatgames.start.<type>",
            "Allows starting a specific type of event.",
            "op"
    ),
    START_ALL(
            "chatgames.start.*",
            "Allows starting any event type.",
            "op"
    ),
    TOP(
            "chatgames.top",
            "Allows viewing the top players by points.",
            "true"
    ),
    FASTEST_TOP(
            "chatgames.fastesttop",
            "Allows viewing the fastest players.",
            "true"
    ),
    PLAY(
            "chatgames.play",
            "Allows playing chat games. Optional permission.",
            "true"
    ),
    TOGGLE(
            "chatgames.toggle",
            "Allows toggling games on or off.",
            "true"
    ),
    TOGGLE_SOUND(
            "chatgames.togglesound",
            "Allows toggling sound effects for games. Optional permission.",
            "true"
    ),
    HELP(
            "chatgames.help",
            "Allows accessing help commands.",
            "true"
    ),
    RELOAD(
            "chatgames.reload",
            "Allows reloading the configuration files.",
            "op"
    ),
    TOGGLE_ALL(
            "chatgames.toggleAll",
            "Allows toggling all games on or off.",
            "op"
    ),
    REMOVE(
            "chatgames.remove",
            "Allows removing a player from the database.",
            "op"
    ),
    LOGS(
            "chatgames.logs",
            "Allows viewing logs.",
            "op"
    );

    private final String permission;
    private final String description;
    private final String defaultValue;

    PermissionKey(String permission, String description, String defaultValue) {
        this.permission = permission;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * @return The permission string.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return The description of the permission.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The default value for the permission.
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
