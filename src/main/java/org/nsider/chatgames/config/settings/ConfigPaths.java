package org.nsider.chatgames.config.settings;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.nsider.chatgames.config.ConfigManager;

/**
 * Enum that defines paths and types for all configuration values across multiple config files.
 */
public enum ConfigPaths {

    /* ----------------------------------------------config.yml---------------------------------------------- */

    /** Whether the plugin should check for updates on startup. */
    CHECK_UPDATE(
            ConfigManager.getConfig(),
            "check-update",
            ConfigType.BOOLEAN
    ),

    /** Language code for internal plugin translations (materials, entities). */
    LANGUAGE(
            ConfigManager.getConfig(),
            "lang",
            ConfigType.STRING
    ),

    /** Storage backend to use: 'SQLite' or 'MySQL'. */
    STORAGE_TYPE(
            ConfigManager.getConfig(),
            "Storage",
            ConfigType.STRING
    ),

    /** Name of the database table used to store all ChatGames data. */
    TABLE_NAME(
            ConfigManager.getConfig(),
            "table_name",
            ConfigType.STRING
    ),

    /** Name of the database table used to store all the data for fastest times. */
    FASTEST_TIMES_TABLE(
            ConfigManager.getConfig(),
            "fastest_times_table",
            ConfigType.STRING
    ),

    /** SQLite database file name. */
    SQLITE_FILENAME(
            ConfigManager.getConfig(),
            "SQLite.Filename",
            ConfigType.STRING
    ),

    /** MySQL host address. */
    MYSQL_HOST(
            ConfigManager.getConfig(),
            "MySQL.host",
            ConfigType.STRING
    ),

    /** MySQL port number. */
    MYSQL_PORT(
            ConfigManager.getConfig(),
            "MySQL.port",
            ConfigType.STRING
    ),

    /** MySQL username. */
    MYSQL_USER(
            ConfigManager.getConfig(),
            "MySQL.user",
            ConfigType.STRING
    ),

    /** MySQL password. */
    MYSQL_PASSWORD(
            ConfigManager.getConfig(),
            "MySQL.password",
            ConfigType.STRING
    ),

    /** MySQL database name. */
    MYSQL_DATABASE(
            ConfigManager.getConfig(),
            "MySQL.database",
            ConfigType.STRING
    ),

    /** Connection parameters for MySQL JDBC URL. */
    MYSQL_CONNECTION_PARAMS(
            ConfigManager.getConfig(),
            "MySQL.connection-parameters",
            ConfigType.STRING
    ),

    /** Maximum number of connections in the MySQL pool. */
    MYSQL_POOL_MAX_SIZE(
            ConfigManager.getConfig(),
            "MySQL.Pool-Settings.max-pool-size",
            ConfigType.INTEGER
    ),

    /** Minimum number of idle connections in the MySQL pool. */
    MYSQL_POOL_MIN_IDLE(
            ConfigManager.getConfig(),
            "MySQL.Pool-Settings.min-idle",
            ConfigType.INTEGER
    ),

    /** Maximum lifetime of a MySQL connection in milliseconds. */
    MYSQL_POOL_MAX_LIFETIME(
            ConfigManager.getConfig(),
            "MySQL.Pool-Settings.max-lifetime",
            ConfigType.INTEGER
    ),

    /** Time to keep a MySQL connection alive in milliseconds. */
    MYSQL_POOL_KEEP_ALIVE(
            ConfigManager.getConfig(),
            "MySQL.Pool-Settings.keep-alive-time",
            ConfigType.INTEGER
    ),

    /** Timeout for acquiring a MySQL connection from the pool in milliseconds. */
    MYSQL_POOL_TIMEOUT(
            ConfigManager.getConfig(),
            "MySQL.Pool-Settings.time-out",
            ConfigType.INTEGER
    ),

    /** List of world names where ChatGames should be disabled. */
    DISABLED_WORLDS(
            ConfigManager.getConfig(),
            "disabled-worlds",
            ConfigType.LIST_STRING
    ),

    /** Chat prefix shown before all ChatGames messages. */
    PREFIX(
            ConfigManager.getConfig(),
            "prefix",
            ConfigType.STRING
    ),

    /** Allows players with 'chatgames.start.*' permission to start games even when disabled. */
    ALLOW_STARTING_WHEN_DISABLED(
            ConfigManager.getConfig(),
            "allow_starting_from_disabled",
            ConfigType.BOOLEAN
    ),

    /** If false, the player's message will not be cancelled when they win a game. */
    CANCEL_MESSAGE_ON_WIN(
            ConfigManager.getConfig(),
            "cancel-message-when-winning",
            ConfigType.BOOLEAN
    ),

    /** Minimum number of online players required to allow a game to start. */
    MIN_PLAYERS(
            ConfigManager.getConfig(),
            "min_players_online",
            ConfigType.INTEGER
    ),

    /** Fixed interval in minutes between game events. Set to -1 to disable the scheduler. */
    TIME_MINUTES(
            ConfigManager.getConfig(),
            "time_minutes",
            ConfigType.INTEGER
    ),

    /** Enables random delays between games instead of a fixed interval. */
    RANDOM_DELAY_ENABLED(
            ConfigManager.getConfig(),
            "randomDelay.enable",
            ConfigType.BOOLEAN
    ),

    /** Time unit for the random delay: 'minutes' or 'seconds'. */
    RANDOM_DELAY_UNIT(
            ConfigManager.getConfig(),
            "randomDelay.timeUnit",
            ConfigType.STRING
    ),

    /** Minimum delay used when random delays are enabled. */
    RANDOM_DELAY_MIN(
            ConfigManager.getConfig(),
            "randomDelay.minimum",
            ConfigType.INTEGER
    ),

    /** Maximum delay used when random delays are enabled. */
    RANDOM_DELAY_LIMIT(
            ConfigManager.getConfig(),
            "randomDelay.limit",
            ConfigType.INTEGER
    ),

    /** If true, only players with 'chatgames.play' permission can see or participate in games. */
    REQUIRE_PERMISSION_TO_PLAY(
            ConfigManager.getConfig(),
            "use-permission-toplay",
            ConfigType.BOOLEAN
    ),

    /** If true, all players except vanished ones are counted when checking player counts. */
    COUNT_ALL_PLAYERS(
            ConfigManager.getConfig(),
            "count-all-players",
            ConfigType.BOOLEAN
    ),

    /** Enables the leaderboard for fastest typers using /chatgames fastest. */
    FASTEST_TYPERS_TOP(
            ConfigManager.getConfig(),
            "fastest-typers-top",
            ConfigType.BOOLEAN
    ),

    /** Global sound volume used for all ChatGames sounds. */
    GAME_SOUND_VOLUME(
            ConfigManager.getConfig(),
            "GameSounds.volume",
            ConfigType.DOUBLE
    ),

    /** Global sound pitch used for all ChatGames sounds. */
    GAME_SOUND_PITCH(
            ConfigManager.getConfig(),
            "GameSounds.pitch",
            ConfigType.DOUBLE
    ),

    /** Sound played when a player wins a chat game. */
    SOUND_WIN(
            ConfigManager.getConfig(),
            "GameSounds.win",
            ConfigType.STRING
    ),

    /** Sound played when a chat game starts. */
    SOUND_GAME_START(
            ConfigManager.getConfig(),
            "GameSounds.game-start",
            ConfigType.STRING
    ),

    /** Sound played when a chat game expires without a winner. */
    SOUND_TIME_EXPIRED(
            ConfigManager.getConfig(),
            "GameSounds.time-expired",
            ConfigType.STRING
    ),

    /** Sound played when the player's answer is incorrect due to wrong casing. */
    SOUND_WRONG_CASE(
            ConfigManager.getConfig(),
            "GameSounds.wrong-case",
            ConfigType.STRING
    ),
    /* -----------------------------------------END OF config.yml----------------------------------------- */


    /* ----------------------------------------------games.yml---------------------------------------------- */

    /** Whether Unscramble game is enabled. */
    UNSCRAMBLE_ENABLE(
            ConfigManager.getGames(),
            "unscramble.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Unscramble game. */
    UNSCRAMBLE_WINNERS(
            ConfigManager.getGames(),
            "unscramble.winners",
            ConfigType.INTEGER
    ),

    /** Whether Unscramble game is case sensitive. */
    UNSCRAMBLE_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "unscramble.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Unscramble game. */
    UNSCRAMBLE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "unscramble.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Unscramble game round. */
    UNSCRAMBLE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "unscramble.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Unscramble game round. */
    UNSCRAMBLE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "unscramble.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Unreverse game is enabled. */
    UNREVERSE_ENABLE(
            ConfigManager.getGames(),
            "unreverse.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Unreverse game. */
    UNREVERSE_WINNERS(
            ConfigManager.getGames(),
            "unreverse.winners",
            ConfigType.INTEGER
    ),

    /** Whether Unreverse game is case sensitive. */
    UNREVERSE_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "unreverse.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Unreverse game. */
    UNREVERSE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "unreverse.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Unreverse game round. */
    UNREVERSE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "unreverse.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Unreverse game round. */
    UNREVERSE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "unreverse.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Reaction game is enabled. */
    REACTION_ENABLE(
            ConfigManager.getGames(),
            "reaction.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Reaction game. */
    REACTION_WINNERS(
            ConfigManager.getGames(),
            "reaction.winners",
            ConfigType.INTEGER
    ),

    /** Whether Reaction game is case sensitive. */
    REACTION_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "reaction.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Reaction game. */
    REACTION_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "reaction.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Reaction game round. */
    REACTION_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "reaction.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Reaction game round. */
    REACTION_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "reaction.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Guess‑the‑Number game is enabled. */
    GUESS_NUMBER_ENABLE(
            ConfigManager.getGames(),
            "guess_the_number.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Guess‑the‑Number game. */
    GUESS_NUMBER_WINNERS(
            ConfigManager.getGames(),
            "guess_the_number.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Guess‑the‑Number game (must remain false). */
    GUESS_NUMBER_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "guess_the_number.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Guess‑the‑Number game. */
    GUESS_NUMBER_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "guess_the_number.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Lower bound for Guess‑the‑Number game range. */
    GUESS_NUMBER_LOWER_LIMIT(
            ConfigManager.getGames(),
            "guess_the_number.lower-limit",
            ConfigType.INTEGER
    ),

    /** Upper bound for Guess‑the‑Number game range. */
    GUESS_NUMBER_UPPER_LIMIT(
            ConfigManager.getGames(),
            "guess_the_number.upper-limit",
            ConfigType.INTEGER
    ),

    /** Whether Shopping List game is enabled. */
    MEMORIZE_ENABLE(
            ConfigManager.getGames(),
            "memorize.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Shopping List game. */
    MEMORIZE_WINNERS(
            ConfigManager.getGames(),
            "memorize.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Shopping List game. */
    MEMORIZE_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "memorize.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Number of items in the list for Shopping List game. */
    MEMORIZE_LIST_LENGTH(
            ConfigManager.getGames(),
            "memorize.listLength",
            ConfigType.INTEGER
    ),

    /** Time to memorize items in Shopping List game. */
    MEMORIZE_TIME_TO_MEMORIZE(
            ConfigManager.getGames(),
            "memorize.timeToMemorize",
            ConfigType.INTEGER
    ),

    /** Time (in seconds) players have to guess in Shopping List game. */
    MEMORIZE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "memorize.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Shopping List game round. */
    MEMORIZE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "memorize.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Shopping List game round. */
    MEMORIZE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "memorize.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Clickable game is enabled. */
    CLICKABLE_ENABLE(
            ConfigManager.getGames(),
            "clickable.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Clickable game. */
    CLICKABLE_WINNERS(
            ConfigManager.getGames(),
            "clickable.winners",
            ConfigType.INTEGER
    ),

    /** Time (in seconds) players have to guess in Clickable game. */
    CLICKABLE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "clickable.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Clickable game round. */
    CLICKABLE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "clickable.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Clickable game round. */
    CLICKABLE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "clickable.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Fill‑Out game is enabled. */
    FILLOUT_ENABLE(
            ConfigManager.getGames(),
            "fillout.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Fill‑Out game. */
    FILLOUT_WINNERS(
            ConfigManager.getGames(),
            "fillout.winners",
            ConfigType.INTEGER
    ),

    /** Difficulty percentage for Fill‑Out game (1–100). */
    FILLOUT_DIFFICULTY(
            ConfigManager.getGames(),
            "fillout.difficulty",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Fill‑Out game. */
    FILLOUT_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "fillout.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Fill‑Out game. */
    FILLOUT_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "fillout.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Fill‑Out game round. */
    FILLOUT_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "fillout.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Fill‑Out game round. */
    FILLOUT_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "fillout.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Hoverable game is enabled. */
    HOVERABLE_ENABLE(
            ConfigManager.getGames(),
            "hoverable.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Hoverable game. */
    HOVERABLE_WINNERS(
            ConfigManager.getGames(),
            "hoverable.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Hoverable game. */
    HOVERABLE_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "hoverable.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Hoverable game. */
    HOVERABLE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "hoverable.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Hoverable game round. */
    HOVERABLE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "hoverable.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Hoverable game round. */
    HOVERABLE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "hoverable.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Random game is enabled. */
    RANDOM_ENABLE(
            ConfigManager.getGames(),
            "random.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Random game. */
    RANDOM_WINNERS(
            ConfigManager.getGames(),
            "random.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Random game. */
    RANDOM_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "random.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to guess in Random game. */
    RANDOM_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "random.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Character length range for Random game (formatted as "min-max"). */
    RANDOM_LENGTH_RANGE(
            ConfigManager.getGames(),
            "random.lengthRange",
            ConfigType.STRING
    ),

    /** Type of characters allowed in Random game (ALPHA, NUMERIC, etc.). */
    RANDOM_CHARACTER_TYPE(
            ConfigManager.getGames(),
            "random.character_type",
            ConfigType.STRING
    ),

    /** Custom characters used when 'CUSTOM' type is selected. */
    RANDOM_CUSTOM_CHARACTERS(
            ConfigManager.getGames(),
            "random.custom_characters",
            ConfigType.STRING
    ),

    /** Title message for Random game round. */
    RANDOM_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "random.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Random game round. */
    RANDOM_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "random.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Math game is enabled. */
    MATH_ENABLE(
            ConfigManager.getGames(),
            "math.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Math game. */
    MATH_WINNERS(
            ConfigManager.getGames(),
            "math.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Math game (must remain false). */
    MATH_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "math.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to solve in Math game. */
    MATH_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "math.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Difficulty level for Math game (1 = 1–9, 5 = 1–99999). */
    MATH_DIFFICULTY(
            ConfigManager.getGames(),
            "math.difficulty",
            ConfigType.INTEGER
    ),

    /** Operator symbols allowed in Math game. */
    MATH_OPERATORS(
            ConfigManager.getGames(),
            "math.operators",
            ConfigType.LIST_STRING
    ),

    /** Title message for Math game round. */
    MATH_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "math.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Math game round. */
    MATH_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "math.subtitleMessage",
            ConfigType.STRING
    ),

// variable game
    /** Whether Variable game is enabled. */
    VARIABLE_ENABLE(
            ConfigManager.getGames(),
            "variable.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Variable game. */
    VARIABLE_WINNERS(
            ConfigManager.getGames(),
            "variable.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Variable game (must remain false). */
    VARIABLE_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "variable.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to solve in Variable game. */
    VARIABLE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "variable.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Minimum result value for Variable game. */
    VARIABLE_RESULT_MIN(
            ConfigManager.getGames(),
            "variable.result-min",
            ConfigType.INTEGER
    ),

    /** Maximum result value for Variable game. */
    VARIABLE_RESULT_MAX(
            ConfigManager.getGames(),
            "variable.result-max",
            ConfigType.INTEGER
    ),

    /** Symbol shown to represent the 'toGet' value in Variable game. */
    VARIABLE_TO_GET_SYMBOL(
            ConfigManager.getGames(),
            "variable.toGet",
            ConfigType.STRING
    ),


    /** Title message for Variable game round. */
    VARIABLE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "variable.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Variable game round. */
    VARIABLE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "variable.subtitleMessage",
            ConfigType.STRING
    ),

    /** Key‑value pairs used as data for Variable game (format: 'symbol;value'). */
    VARIABLE_DATA(
            ConfigManager.getGames(),
            "variable.data",
            ConfigType.LIST_STRING
    ),

    /** Whether Trivia game is enabled. */
    TRIVIA_ENABLE(
            ConfigManager.getGames(),
            "trivia.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Trivia game. */
    TRIVIA_WINNERS(
            ConfigManager.getGames(),
            "trivia.winners",
            ConfigType.INTEGER
    ),

    /** Case sensitivity for Trivia game (should be false). */
    TRIVIA_CASE_SENSITIVE(
            ConfigManager.getGames(),
            "trivia.case-sensitive",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) players have to answer in Trivia game. */
    TRIVIA_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "trivia.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Title message for Trivia game round. */
    TRIVIA_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "trivia.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Trivia game round. */
    TRIVIA_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "trivia.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether Sequence game is enabled. */
    SEQUENCE_ENABLE(
            ConfigManager.getGames(),
            "sequence.enable",
            ConfigType.BOOLEAN
    ),

    /** Number of winners allowed in Sequence game. */
    SEQUENCE_WINNERS(
            ConfigManager.getGames(),
            "sequence.winners",
            ConfigType.INTEGER
    ),

    /** Time (in seconds) players have to solve in Sequence game. */
    SEQUENCE_TIME_TO_GUESS(
            ConfigManager.getGames(),
            "sequence.timeToGuess_seconds",
            ConfigType.INTEGER
    ),

    /** Number of operations to include in the equation. For example, 3 -> "3 + 4 - x = 10" */
    SEQUENCE_OPERATIONS(
            ConfigManager.getGames(),
            "sequence.number-of-operations",
            ConfigType.INTEGER
    ),

    /** Allowed operators in the equation */
    SEQUENCE_OPERATORS(
            ConfigManager.getGames(),
            "sequence.operators",
            ConfigType.LIST_STRING
    ),

    /** Title message for Sequence game round. */
    SEQUENCE_TITLE_MESSAGE(
            ConfigManager.getGames(),
            "sequence.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message for Sequence game round. */
    SEQUENCE_SUBTITLE_MESSAGE(
            ConfigManager.getGames(),
            "sequence.subtitleMessage",
            ConfigType.STRING
    ),
    /* -----------------------------------------END OF games.yml----------------------------------------- */


    /* ----------------------------------------------races.yml---------------------------------------------- */

    /** Enables or disables the entire race system. */
    RACES_ENABLED(
            ConfigManager.getRaces(),
            "enable-races",
            ConfigType.BOOLEAN
    ),

    /** Enables race functionality even in creative mode. */
    RACES_CREATIVE_ENABLED(
            ConfigManager.getRaces(),
            "creative-enabled",
            ConfigType.BOOLEAN
    ),

    /** Enables the Hunt race event. */
    RACE_HUNT_ENABLED(
            ConfigManager.getRaces(),
            "hunt.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Hunt race. */
    RACE_HUNT_TIME_LIMIT(
            ConfigManager.getRaces(),
            "hunt.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Hunt race. */
    RACE_HUNT_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "hunt.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Hunt race. */
    RACE_HUNT_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "hunt.subtitleMessage",
            ConfigType.STRING
    ),

    /** Whether mobs from spawners or spawn eggs are counted in the Hunt race. */
    RACE_HUNT_COUNT_SPAWNER_MOBS(
            ConfigManager.getRaces(),
            "hunt.count-spawner-mobs",
            ConfigType.BOOLEAN
    ),

    /** Enables the Mine race event. */
    RACE_MINE_ENABLED(
            ConfigManager.getRaces(),
            "mine.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Mine race. */
    RACE_MINE_TIME_LIMIT(
            ConfigManager.getRaces(),
            "mine.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Mine race. */
    RACE_MINE_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "mine.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Mine race. */
    RACE_MINE_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "mine.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Place race event. */
    RACE_PLACE_ENABLED(
            ConfigManager.getRaces(),
            "place.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Place race. */
    RACE_PLACE_TIME_LIMIT(
            ConfigManager.getRaces(),
            "place.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Place race. */
    RACE_PLACE_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "place.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Place race. */
    RACE_PLACE_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "place.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Fish race event. */
    RACE_FISH_ENABLED(
            ConfigManager.getRaces(),
            "fish.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Fish race. */
    RACE_FISH_TIME_LIMIT(
            ConfigManager.getRaces(),
            "fish.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Fish race. */
    RACE_FISH_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "fish.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Fish race. */
    RACE_FISH_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "fish.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Eat race event. */
    RACE_EAT_ENABLED(
            ConfigManager.getRaces(),
            "eat.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Eat race. */
    RACE_EAT_TIME_LIMIT(
            ConfigManager.getRaces(),
            "eat.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Eat race. */
    RACE_EAT_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "eat.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Eat race. */
    RACE_EAT_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "eat.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Craft race event. */
    RACE_CRAFT_ENABLED(
            ConfigManager.getRaces(),
            "craft.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Craft race. */
    RACE_CRAFT_TIME_LIMIT(
            ConfigManager.getRaces(),
            "craft.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Craft race. */
    RACE_CRAFT_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "craft.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Craft race. */
    RACE_CRAFT_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "craft.subtitleMessage",
            ConfigType.STRING
    ),

    /** Enables the Furnace race event. */
    RACE_FURNACE_ENABLED(
            ConfigManager.getRaces(),
            "furnace.enable",
            ConfigType.BOOLEAN
    ),

    /** Time (in seconds) to complete the Furnace race. */
    RACE_FURNACE_TIME_LIMIT(
            ConfigManager.getRaces(),
            "furnace.timeToComplete_seconds",
            ConfigType.INTEGER
    ),

    /** Title message shown during the Furnace race. */
    RACE_FURNACE_TITLE_MESSAGE(
            ConfigManager.getRaces(),
            "furnace.titleMessage",
            ConfigType.STRING
    ),

    /** Subtitle message shown during the Furnace race. */
    RACE_FURNACE_SUBTITLE_MESSAGE(
            ConfigManager.getRaces(),
            "furnace.subtitleMessage",
            ConfigType.STRING
    ),
    /* -----------------------------------------END OF races.yml----------------------------------------- */

    /* ----------------------------------------------add-ons.yml---------------------------------------------- */

    /** Webhook URL for sending Discord notifications. Leave empty to disable Discord integration. */
    DISCORD_WEBHOOK_URL(
            ConfigManager.getAddons(),
            "discord.webhook-url",
            ConfigType.STRING
    ),

    /** Content shown as the main message in the Discord embed. */
    DISCORD_MESSAGE_CONTENT(
            ConfigManager.getAddons(),
            "discord.message-content",
            ConfigType.STRING
    ),

    /** Image URL used in the Discord embed. */
    DISCORD_IMAGE_URL(
            ConfigManager.getAddons(),
            "discord.img-url",
            ConfigType.STRING
    ),

    /** Username that appears as the sender in the Discord embed. */
    DISCORD_USERNAME(
            ConfigManager.getAddons(),
            "discord.username",
            ConfigType.STRING
    ),

    /** Title of the Discord embed message. */
    DISCORD_TITLE(
            ConfigManager.getAddons(),
            "discord.title",
            ConfigType.STRING
    ),

    /** Description section of the Discord embed. */
    DISCORD_DESCRIPTION(
            ConfigManager.getAddons(),
            "discord.description",
            ConfigType.STRING
    ),

    /** Format for the winner field in the Discord embed. */
    DISCORD_WINNER_FIELD(
            ConfigManager.getAddons(),
            "discord.winner-field",
            ConfigType.STRING
    ),

    /** Format for the game field in the Discord embed. */
    DISCORD_GAME_FIELD(
            ConfigManager.getAddons(),
            "discord.game-field",
            ConfigType.STRING
    ),

    /** File path where chat game logs will be saved. */
    LOG_FILE_PATH(
            ConfigManager.getAddons(),
            "log-file",
            ConfigType.STRING
    ),

    /** Whether to log all chat events to the console. */
    LOG_TO_CONSOLE(
            ConfigManager.getAddons(),
            "log-console",
            ConfigType.BOOLEAN
    ),

    /** Whether to log all chat events to a local file. */
    LOG_TO_FILE(
            ConfigManager.getAddons(),
            "log-local",
            ConfigType.BOOLEAN
    );
    /* -----------------------------------------END OF add-ons.yml----------------------------------------- */

    @Getter private final FileConfiguration file;
    @Getter private final ConfigType type;
    private final String path;

    ConfigPaths(FileConfiguration file, String path, ConfigType type) {
        this.file = file;
        this.path = path;
        this.type = type;
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