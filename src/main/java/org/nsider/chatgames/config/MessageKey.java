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
    UNKNOWN_MENU(
            "messages",
            "unknown_menu",
            "&8[%prefix%&8] &cThere is no menu called &f%menu%&c."
    ),
    DISCORD_TEST_SENT(
            "messages",
            "discord.test_sent",
            "&8[%prefix%&8] &fSent &a%amount% &ftest message(s) to Discord. If nothing shows up, the console says why."
    ),
    DISCORD_LEADERBOARD_POSTED(
            "messages",
            "discord.leaderboard_posted",
            "&8[%prefix%&8] &fThe leaderboard was sent to Discord."
    ),
    DISCORD_DISABLED(
            "messages",
            "discord.disabled",
            "&8[%prefix%&8] &cDiscord messages are off: set &fenabled: true &cin discord.yml."
    ),
    DISCORD_NO_CHANNEL(
            "messages",
            "discord.no_channel",
            "&8[%prefix%&8] &f%section% &chas no channel with a webhook or DiscordSRV channel in discord.yml."
    ),
    DISCORD_UNKNOWN_MESSAGE(
            "messages",
            "discord.unknown_message",
            "&8[%prefix%&8] &cThere is no Discord message called &f%message%&c."
    ),
    DISCORD_EMPTY_MESSAGE(
            "messages",
            "discord.empty_message",
            "&8[%prefix%&8] &f%message% &cis empty or missing in discord.yml."
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
    ),
    CHALLENGE_COMPLETED(
            "messages",
            "challenges.completed",
            "&8[%prefix%&8] &a%period% challenge complete: &f%challenge%&a! Claim it with &f/chatgames claim&a."
    ),
    CHALLENGE_CLAIMED(
            "messages",
            "challenges.claimed",
            "&8[%prefix%&8] &aYou claimed &f%challenge%&a!"
    ),
    NOTHING_TO_CLAIM(
            "messages",
            "challenges.nothing_to_claim",
            "&8[%prefix%&8] &cYou have nothing to claim right now."
    ),
    CHALLENGES_HEADER(
            "messages",
            "challenges.header",
            "&8[%prefix%&8] &e%period% challenges &7(%completed%/%total% done, resets in %resets%)"
    ),
    CHALLENGES_ENTRY(
            "messages",
            "challenges.entry",
            "&7- %challenge% &8- &7%description% &f%progress%/%amount% %status%"
    ),
    CHALLENGE_STATUS_PROGRESS(
            "messages",
            "challenges.status_in_progress",
            ""
    ),
    CHALLENGE_STATUS_READY(
            "messages",
            "challenges.status_ready",
            "&a[Ready to claim]"
    ),
    CHALLENGE_STATUS_CLAIMED(
            "messages",
            "challenges.status_claimed",
            "&7[Claimed]"
    ),
    CHALLENGES_NONE(
            "messages",
            "challenges.none",
            "&8[%prefix%&8] &cThere are no challenges right now."
    ),
    CHALLENGES_DAILY(
            "messages",
            "challenges.daily",
            "Daily"
    ),
    CHALLENGES_WEEKLY(
            "messages",
            "challenges.weekly",
            "Weekly"
    ),
    CHALLENGES_TIME_DAYS(
            "messages",
            "challenges.time_days",
            "%amount%d"
    ),
    CHALLENGES_TIME_HOURS(
            "messages",
            "challenges.time_hours",
            "%amount%h"
    ),
    CHALLENGES_TIME_MINUTES(
            "messages",
            "challenges.time_minutes",
            "%amount%m"
    ),
    CHALLENGES_RESET(
            "messages",
            "challenges.reset",
            "&8[%prefix%&8] &fReset &c%player%&f's daily and weekly challenge progress."
    ),
    STREAK_GREW(
            "messages",
            "streaks.grew",
            "&8[%prefix%&8] &aYour %streak% &ais now &f%days% &aday(s)! Claim the reward with &f/chatgames claim&a."
    ),
    STREAK_CLAIMED(
            "messages",
            "streaks.claimed",
            "&8[%prefix%&8] &aYou claimed your %streak% &areward (day %days%)!"
    ),
    STREAKS_HEADER(
            "messages",
            "streaks.header",
            "&8[%prefix%&8] &eStreaks:"
    ),
    STREAKS_ENTRY(
            "messages",
            "streaks.entry",
            "&7- %streak% &8- &f%days% &7day(s), best &f%longest% %status%"
    ),
    STREAK_STATUS_READY(
            "messages",
            "streaks.status_ready",
            "&a[%unclaimed% to claim]"
    ),
    STREAK_STATUS_TODAY(
            "messages",
            "streaks.status_today",
            "&7[done today]"
    ),
    STREAK_STATUS_WAITING(
            "messages",
            "streaks.status_waiting",
            "&e[not done today]"
    ),
    STREAKS_NONE(
            "messages",
            "streaks.none",
            "&8[%prefix%&8] &cStreaks are turned off."
    ),
    STREAK_SET(
            "messages",
            "streaks.set",
            "&8[%prefix%&8] &fSet &c%player%&f's %streak% &fto &a%days% &fday(s)."
    ),
    GLOBAL_NOT_ENABLED(
            "messages",
            "global_not_enabled",
            "&8[%prefix%&8] &f%game% &cisn't enabled as a global game in global_games.yml."
    ),
    STREAK_UNKNOWN(
            "messages",
            "streaks.unknown",
            "&8[%prefix%&8] &cThere is no streak called &f%type%&c. Use challenge, daily or global."
    ),
    EVENTS_UNKNOWN(
            "messages",
            "events.unknown",
            "&8[%prefix%&8] &cThere is no event called &f%event%&c."
    ),
    EVENTS_STARTED(
            "messages",
            "events.started",
            "&8[%prefix%&8] &fStarted the event %event%&f."
    ),
    EVENTS_ALREADY_RUNNING(
            "messages",
            "events.already_running",
            "&8[%prefix%&8] &cAn event is already running: %event%&c."
    ),
    EVENTS_CANCELLED(
            "messages",
            "events.cancelled",
            "&8[%prefix%&8] &cAnother plugin cancelled the event %event%&c."
    ),
    EVENTS_NOT_RUNNING(
            "messages",
            "events.not_running",
            "&8[%prefix%&8] &cNo event is running."
    ),
    EVENTS_STOPPED(
            "messages",
            "events.stopped",
            "&8[%prefix%&8] &fStopped the event %event%&f."
    ),
    EVENTS_LIST_HEADER(
            "messages",
            "events.list_header",
            "&8[%prefix%&8] &eEvents:"
    ),
    EVENTS_LIST_ENTRY(
            "messages",
            "events.list_entry",
            "&7- &f%id% &8(%event%&8) &7%steps% games, next: &f%next%%running%"
    ),
    EVENTS_LIST_RUNNING(
            "messages",
            "events.list_running",
            " &a(running, game %step%/%steps%)"
    ),
    EVENTS_LIST_EMPTY(
            "messages",
            "events.list_empty",
            "&8[%prefix%&8] &cNo events are set up in events.yml."
    ),
    EVENTS_NOT_SCHEDULED(
            "messages",
            "events.not_scheduled",
            "not scheduled"
    ),
    EVENTS_TOP_HEADER(
            "messages",
            "events.top_header",
            "&8[%prefix%&8] &eMost event points in %event%&e:"
    ),
    EVENTS_TOP_ENTRY(
            "messages",
            "events.top_entry",
            "&c%spot%. &f%player% &8| &e%points% &7points"
    ),
    EVENTS_TOP_EMPTY(
            "messages",
            "events.top_empty",
            "&8[%prefix%&8] &cNobody has scored in %event% &cyet."
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
