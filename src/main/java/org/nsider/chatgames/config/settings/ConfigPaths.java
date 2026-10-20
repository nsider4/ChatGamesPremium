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

    /** Enables the Hunt race event. */
    RACE_HUNT_ENABLED(
            ConfigFile.RACES,
            "hunt.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Hunt race. */
    RACE_HUNT_TIME_LIMIT(
            ConfigFile.RACES,
            "hunt.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Hunt race. */
    RACE_HUNT_TITLE_MESSAGE(
            ConfigFile.RACES,
            "hunt.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Hunt race. */
    RACE_HUNT_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "hunt.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether mobs from spawners or spawn eggs are counted in the Hunt race. */
    RACE_HUNT_COUNT_SPAWNER_MOBS(
            ConfigFile.RACES,
            "hunt.count-spawner-mobs",
            ConfigType.BOOLEAN
    ),

    /** Enables the Mine race event. */
    RACE_MINE_ENABLED(
            ConfigFile.RACES,
            "mine.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Mine race. */
    RACE_MINE_TIME_LIMIT(
            ConfigFile.RACES,
            "mine.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Mine race. */
    RACE_MINE_TITLE_MESSAGE(
            ConfigFile.RACES,
            "mine.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Mine race. */
    RACE_MINE_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "mine.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Place race event. */
    RACE_PLACE_ENABLED(
            ConfigFile.RACES,
            "place.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Place race. */
    RACE_PLACE_TIME_LIMIT(
            ConfigFile.RACES,
            "place.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Place race. */
    RACE_PLACE_TITLE_MESSAGE(
            ConfigFile.RACES,
            "place.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Place race. */
    RACE_PLACE_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "place.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Fish race event. */
    RACE_FISH_ENABLED(
            ConfigFile.RACES,
            "fish.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Fish race. */
    RACE_FISH_TIME_LIMIT(
            ConfigFile.RACES,
            "fish.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Fish race. */
    RACE_FISH_TITLE_MESSAGE(
            ConfigFile.RACES,
            "fish.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Fish race. */
    RACE_FISH_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "fish.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Eat race event. */
    RACE_EAT_ENABLED(
            ConfigFile.RACES,
            "eat.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Eat race. */
    RACE_EAT_TIME_LIMIT(
            ConfigFile.RACES,
            "eat.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Eat race. */
    RACE_EAT_TITLE_MESSAGE(
            ConfigFile.RACES,
            "eat.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Eat race. */
    RACE_EAT_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "eat.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Craft race event. */
    RACE_CRAFT_ENABLED(
            ConfigFile.RACES,
            "craft.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Craft race. */
    RACE_CRAFT_TIME_LIMIT(
            ConfigFile.RACES,
            "craft.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Craft race. */
    RACE_CRAFT_TITLE_MESSAGE(
            ConfigFile.RACES,
            "craft.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Craft race. */
    RACE_CRAFT_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "craft.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Furnace race event. */
    RACE_FURNACE_ENABLED(
            ConfigFile.RACES,
            "furnace.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Furnace race. */
    RACE_FURNACE_TIME_LIMIT(
            ConfigFile.RACES,
            "furnace.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Furnace race. */
    RACE_FURNACE_TITLE_MESSAGE(
            ConfigFile.RACES,
            "furnace.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Furnace race. */
    RACE_FURNACE_SUBTITLE_MESSAGE(
            ConfigFile.RACES,
            "furnace.subtitleMessage",
            ConfigType.STRING
    ),
    /* -----------------------------------------END OF races.yml----------------------------------------- */

    /* ----------------------------------------------add-ons.yml---------------------------------------------- */

    /** Webhook URL for sending Discord notifications. Leave empty to disable Discord integration. */
    DISCORD_WEBHOOK_URL(
            ConfigFile.ADDONS,
            "discord.webhook-url",
            ConfigType.STRING
    ),

    /** Content shown as the main message in the Discord embed. */
    DISCORD_MESSAGE_CONTENT(
            ConfigFile.ADDONS,
            "discord.message-content",
            ConfigType.STRING
    ),

    /** Image URL used in the Discord embed. */
    DISCORD_IMAGE_URL(
            ConfigFile.ADDONS,
            "discord.img-url",
            ConfigType.STRING
    ),

    /** Username that appears as the sender in the Discord embed. */
    DISCORD_USERNAME(
            ConfigFile.ADDONS,
            "discord.username",
            ConfigType.STRING
    ),

    /** Title of the Discord embed message. */
    DISCORD_TITLE(
            ConfigFile.ADDONS,
            "discord.title",
            ConfigType.STRING
    ),

    /** Description section of the Discord embed. */
    DISCORD_DESCRIPTION(
            ConfigFile.ADDONS,
            "discord.description",
            ConfigType.STRING
    ),

    /** Format for the winner field in the Discord embed. */
    DISCORD_WINNER_FIELD(
            ConfigFile.ADDONS,
            "discord.winner-field",
            ConfigType.STRING
    ),

    /** Format for the game field in the Discord embed. */
    DISCORD_GAME_FIELD(
            ConfigFile.ADDONS,
            "discord.game-field",
            ConfigType.STRING
    ),

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