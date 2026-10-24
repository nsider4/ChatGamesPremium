package org.nsider.chatgames.config.settings;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Enum that defines paths and types for all configuration values across multiple config files.
 */
public enum ConfigPaths {

    /* ----------------------------------------------config.yml---------------------------------------------- */

    /** Whether the plugin should check for updates on startup. */
    CHECK_UPDATE(
            ConfigFile.CONFIG,
            "check-update",
            ConfigType.BOOLEAN
    ),

    /** Language code for internal plugin translations (materials, entities). */
    LANGUAGE(
            ConfigFile.CONFIG,
            "lang",
            ConfigType.STRING
    ),

    /** Language of the config files: 'auto' (follows lang), 'none', or a code such as 'es'. */
    CONFIG_LANGUAGE(
            ConfigFile.CONFIG,
            "config-language",
            ConfigType.STRING
    ),

    /** Storage backend to use: 'SQLite' or 'MySQL'. */
    STORAGE_TYPE(
            ConfigFile.CONFIG,
            "Storage",
            ConfigType.STRING
    ),

    /** Name of the database table used to store all ChatGames data. */
    TABLE_NAME(
            ConfigFile.CONFIG,
            "table_name",
            ConfigType.STRING
    ),

    /** Name of the database table used to store all the data for fastest times. */
    FASTEST_TIMES_TABLE(
            ConfigFile.CONFIG,
            "fastest_times_table",
            ConfigType.STRING
    ),

    /** SQLite database file name. */
    SQLITE_FILENAME(
            ConfigFile.CONFIG,
            "SQLite.Filename",
            ConfigType.STRING
    ),

    /** MySQL host address. */
    MYSQL_HOST(
            ConfigFile.CONFIG,
            "MySQL.host",
            ConfigType.STRING
    ),

    /** MySQL port number. */
    MYSQL_PORT(
            ConfigFile.CONFIG,
            "MySQL.port",
            ConfigType.STRING
    ),

    /** MySQL username. */
    MYSQL_USER(
            ConfigFile.CONFIG,
            "MySQL.user",
            ConfigType.STRING
    ),

    /** MySQL password. */
    MYSQL_PASSWORD(
            ConfigFile.CONFIG,
            "MySQL.password",
            ConfigType.STRING
    ),

    /** MySQL database name. */
    MYSQL_DATABASE(
            ConfigFile.CONFIG,
            "MySQL.database",
            ConfigType.STRING
    ),

    /** Connection parameters for MySQL JDBC URL. */
    MYSQL_CONNECTION_PARAMS(
            ConfigFile.CONFIG,
            "MySQL.connection-parameters",
            ConfigType.STRING
    ),

    /** Maximum number of connections in the MySQL pool. */
    MYSQL_POOL_MAX_SIZE(
            ConfigFile.CONFIG,
            "MySQL.Pool-Settings.max-pool-size",
            ConfigType.INTEGER
    ),

    /** Minimum number of idle connections in the MySQL pool. */
    MYSQL_POOL_MIN_IDLE(
            ConfigFile.CONFIG,
            "MySQL.Pool-Settings.min-idle",
            ConfigType.INTEGER
    ),

    /** Maximum lifetime of a MySQL connection in milliseconds. */
    MYSQL_POOL_MAX_LIFETIME(
            ConfigFile.CONFIG,
            "MySQL.Pool-Settings.max-lifetime",
            ConfigType.INTEGER
    ),

    /** Time to keep a MySQL connection alive in milliseconds. */
    MYSQL_POOL_KEEP_ALIVE(
            ConfigFile.CONFIG,
            "MySQL.Pool-Settings.keep-alive-time",
            ConfigType.INTEGER
    ),

    /** Timeout for acquiring a MySQL connection from the pool in milliseconds. */
    MYSQL_POOL_TIMEOUT(
            ConfigFile.CONFIG,
            "MySQL.Pool-Settings.time-out",
            ConfigType.INTEGER
    ),

    /** List of world names where ChatGames should be disabled. */
    DISABLED_WORLDS(
            ConfigFile.CONFIG,
            "disabled-worlds",
            ConfigType.LIST_STRING
    ),

    /** Chat prefix shown before all ChatGames messages. */
    PREFIX(
            ConfigFile.CONFIG,
            "prefix",
            ConfigType.STRING
    ),

    /** Allows players with 'chatgames.start.*' permission to start games even when disabled. */
    ALLOW_STARTING_WHEN_DISABLED(
            ConfigFile.CONFIG,
            "allow_starting_from_disabled",
            ConfigType.BOOLEAN
    ),

    /** If false, the player's message will not be cancelled when they win a game. */
    CANCEL_MESSAGE_ON_WIN(
            ConfigFile.CONFIG,
            "cancel-message-when-winning",
            ConfigType.BOOLEAN
    ),

    /** Minimum number of online players required to allow a game to start. */
    MIN_PLAYERS(
            ConfigFile.CONFIG,
            "min_players_online",
            ConfigType.INTEGER
    ),

    /** Fixed interval in minutes between game events. Set to -1 to disable the scheduler. */
    TIME_MINUTES(
            ConfigFile.CONFIG,
            "time_minutes",
            ConfigType.INTEGER
    ),

    /** Enables random delays between games instead of a fixed interval. */
    RANDOM_DELAY_ENABLED(
            ConfigFile.CONFIG,
            "randomDelay.enable",
            ConfigType.BOOLEAN
    ),

    /** Time unit for the random delay: 'minutes' or 'seconds'. */
    RANDOM_DELAY_UNIT(
            ConfigFile.CONFIG,
            "randomDelay.timeUnit",
            ConfigType.STRING
    ),

    /** Minimum delay used when random delays are enabled. */
    RANDOM_DELAY_MIN(
            ConfigFile.CONFIG,
            "randomDelay.minimum",
            ConfigType.INTEGER
    ),

    /** Maximum delay used when random delays are enabled. */
    RANDOM_DELAY_LIMIT(
            ConfigFile.CONFIG,
            "randomDelay.limit",
            ConfigType.INTEGER
    ),

    /** If true, only players with 'chatgames.play' permission can see or participate in games. */
    REQUIRE_PERMISSION_TO_PLAY(
            ConfigFile.CONFIG,
            "use-permission-toplay",
            ConfigType.BOOLEAN
    ),

    /** If true, all players except vanished ones are counted when checking player counts. */
    COUNT_ALL_PLAYERS(
            ConfigFile.CONFIG,
            "count-all-players",
            ConfigType.BOOLEAN
    ),

    /** Enables the leaderboard for fastest typers using /chatgames fastest. */
    FASTEST_TYPERS_TOP(
            ConfigFile.CONFIG,
            "fastest-typers-top",
            ConfigType.BOOLEAN
    ),

    /** How often the leaderboards behind the PlaceholderAPI placeholders are rebuilt, in seconds. */
    PLACEHOLDER_REFRESH_SECONDS(
            ConfigFile.CONFIG,
            "placeholders.refresh-seconds",
            ConfigType.INTEGER
    ),

    /** What a placeholder shows when there is nothing to show (an empty leaderboard place, no time yet). */
    PLACEHOLDER_EMPTY_VALUE(
            ConfigFile.CONFIG,
            "placeholders.empty-value",
            ConfigType.STRING
    ),

    /** Global sound volume used for all ChatGames sounds. */
    GAME_SOUND_VOLUME(
            ConfigFile.CONFIG,
            "GameSounds.volume",
            ConfigType.DOUBLE
    ),

    /** Global sound pitch used for all ChatGames sounds. */
    GAME_SOUND_PITCH(
            ConfigFile.CONFIG,
            "GameSounds.pitch",
            ConfigType.DOUBLE
    ),

    /** Sound played when a player wins a chat game. */
    SOUND_WIN(
            ConfigFile.CONFIG,
            "GameSounds.win",
            ConfigType.STRING
    ),

    /** Sound played when a chat game starts. */
    SOUND_GAME_START(
            ConfigFile.CONFIG,
            "GameSounds.game-start",
            ConfigType.STRING
    ),

    /** Sound played when a chat game expires without a winner. */
    SOUND_TIME_EXPIRED(
            ConfigFile.CONFIG,
            "GameSounds.time-expired",
            ConfigType.STRING
    ),

    /** Sound played when the player's answer is incorrect due to wrong casing. */
    SOUND_WRONG_CASE(
            ConfigFile.CONFIG,
            "GameSounds.wrong-case",
            ConfigType.STRING
    ),
    /* -----------------------------------------END OF config.yml----------------------------------------- */

    /* games.yml is read per game by org.nsider.chatgames.games.round.GameSettings */

    /* ----------------------------------------------races.yml---------------------------------------------- */

    /** Enables or disables the entire race system. */
    RACES_ENABLED(
            ConfigFile.RACES,
            "enable-races",
            ConfigType.BOOLEAN
    ),

    /** Enables race functionality even in creative mode. */
    RACES_CREATIVE_ENABLED(
            ConfigFile.RACES,
            "creative-enabled",
            ConfigType.BOOLEAN
    ),

    /** Whether mobs from spawners or spawn eggs are counted in the Hunt race. */
    RACE_HUNT_COUNT_SPAWNER_MOBS(
            ConfigFile.RACES,
            "hunt.count-spawner-mobs",
            ConfigType.BOOLEAN
    ),

    /* -----------------------------------------END OF races.yml----------------------------------------- */

    /* ----------------------------------------------add-ons.yml---------------------------------------------- */

    /** File path where chat game logs will be saved. */
    LOG_FILE_PATH(
            ConfigFile.ADDONS,
            "log-file",
            ConfigType.STRING
    ),

    /** Whether to log all chat events to the console. */
    LOG_TO_CONSOLE(
            ConfigFile.ADDONS,
            "log-console",
            ConfigType.BOOLEAN
    ),

    /** Whether to log all chat events to a local file. */
    LOG_TO_FILE(
            ConfigFile.ADDONS,
            "log-local",
            ConfigType.BOOLEAN
    );
    /* -----------------------------------------END OF add-ons.yml----------------------------------------- */

    @Getter private final ConfigFile source;
    @Getter private final ConfigType type;
    private final String path;

    ConfigPaths(ConfigFile source, String path, ConfigType type) {
        this.source = source;
        this.path = path;
        this.type = type;
    }

    /**
     * @return the currently loaded file this path belongs to
     */
    public FileConfiguration getFile() {
        return source.get();
    }

    public String path() {
        return path;
    }

    /**
     * Defines supported data types for configuration values.
     */
    public enum ConfigType {
        STRING,
        BOOLEAN,
        INTEGER,
        DOUBLE,
        LIST_STRING,
        LIST_INTEGER,
        SECTION
    }
}