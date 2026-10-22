package org.nsider.chatgames.config;

import org.nsider.chatgames.api.GameType;
import org.nsider.inventorymanager.extras.Nonnull;

/**
 * Every permission the plugin checks. plugin.yml declares the same list;
 * {@code PermissionKeyTest} fails the build if the two drift apart.
 */
public enum PermissionKey {
    START_TYPE("chatgames.start.<type>", "Allows starting one game or race by command, e.g. chatgames.start.unscramble.", "op"),
    START_ALL("chatgames.start.*", "Allows starting any game or race, and starting games from disabled worlds.", "op"),
    GLOBAL("chatgames.global", "Allows starting a global game or race by command.", "op"),
    STOP("chatgames.stop", "Allows stopping the running game with no winner.", "op"),
    TOP("chatgames.top", "Allows viewing the top players by points.", "true"),
    FASTEST_TOP("chatgames.fastesttop", "Allows viewing the fastest players, overall or per game.", "true"),
    POINTS("chatgames.points", "Allows checking your own points.", "true"),
    POINTS_OTHERS("chatgames.points.others", "Allows checking another player's points.", "op"),
    MANAGE_POINTS("chatgames.manage.points", "Allows adding, removing and setting players' points.", "op"),
    PLAY("chatgames.play", "Allows playing chat games. Only checked when use-permission-toplay is true.", "true"),
    TOGGLE("chatgames.toggle", "Allows toggling chat games on or off for yourself.", "true"),
    TOGGLE_OTHERS("chatgames.toggle.others", "Allows toggling chat games on or off for another player.", "op"),
    TOGGLE_SOUND("chatgames.togglesound", "Allows toggling game sounds for yourself.", "true"),
    TOGGLE_SOUND_OTHERS("chatgames.togglesound.others", "Allows toggling game sounds for another player.", "op"),
    HELP("chatgames.help", "Allows viewing the command list.", "true"),
    RELOAD("chatgames.reload", "Allows reloading the configuration files.", "op"),
    TOGGLE_ALL("chatgames.toggleAll", "Allows turning the automatic game rotation on or off.", "op"),
    PRESET("chatgames.preset", "Allows listing, loading and saving game presets.", "op"),
    REMOVE("chatgames.remove", "Allows removing a player from the database.", "op"),
    LOGS("chatgames.logs", "Allows viewing the game logs.", "op"),
    CHALLENGES("chatgames.challenges", "Allows viewing your daily and weekly challenges.", "true"),
    STREAKS("chatgames.streaks", "Allows viewing your streaks.", "true"),
    CLAIM("chatgames.claim", "Allows claiming finished challenges and streak rewards.", "true"),
    CHALLENGES_RESET("chatgames.challenges.reset", "Allows clearing a player's challenge progress.", "op"),
    STREAKS_SET("chatgames.streaks.set", "Allows setting a player's streak.", "op"),
    EVENT("chatgames.event", "Allows listing game events and viewing their leaderboards.", "true"),
    EVENT_MANAGE("chatgames.event.manage", "Allows starting and stopping game events.", "op"),
    MENU("chatgames.menu", "Allows opening the main menu.", "true"),
    PROFILE("chatgames.profile", "Allows viewing your profile.", "true"),
    PROFILE_OTHERS("chatgames.profile.others", "Allows viewing another player's profile.", "true"),
    ADMIN("chatgames.admin", "Allows opening the admin menu (each button still needs its command's permission).", "op"),
    DEBUG("chatgames.debug", "Allows opening the debug menu to test games privately.", "op");

    private final String permission;
    private final String description;
    private final String defaultValue;

    PermissionKey(String permission, String description, String defaultValue) {
        this.permission = permission;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the permission to start one game, e.g. "chatgames.start.unscramble"
     */
    @Nonnull
    public static String startPermission(@Nonnull GameType type) {
        return "chatgames.start." + type.getConfigKey();
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return "op" or "true", as in plugin.yml
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
