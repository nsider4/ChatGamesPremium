package org.nsider.chatgames;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.nsider.chatgames.commands.ChatGamesCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.DataHandler;
import org.nsider.chatgames.database.HikariDatabase;
import org.nsider.chatgames.database.QueryInitializer;
import org.nsider.chatgames.database.enums.StorageType;
import org.nsider.chatgames.games.builder.GameAlgorithms;
import org.nsider.chatgames.games.builder.GameBuilder;
import org.nsider.chatgames.games.manager.GameConfigManager;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.utils.GameUtils;
import org.nsider.chatgames.globalgames.GlobalGameController;
import org.nsider.chatgames.listeners.ChatListener;
import org.nsider.chatgames.listeners.DiscordListener;
import org.nsider.chatgames.listeners.LogListener;
import org.nsider.chatgames.listeners.LoginListener;
import org.nsider.chatgames.managers.ChallengeManager;
import org.nsider.chatgames.managers.LanguageManager;
import org.nsider.chatgames.managers.MilestoneManager;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.PlayerRepository;
import org.nsider.chatgames.other.UpdateChecker;
import org.nsider.chatgames.races.RaceBuilder;
import org.nsider.chatgames.reward.RewardManager;
import org.nsider.chatgames.tasks.ExpirationTask;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.chatgames.utils.gameplay.VersionUtils;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.management.LogUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.Random;

@Getter
public final class ChatGames extends JavaPlugin {

    private static final int SPIGOT_RESOURCE_ID = 107781;
    /** Cached player data is written back every 5 minutes, so a crash loses little. */
    private static final long AUTOSAVE_TICKS = 5 * 60 * 20L;

    private final Random random = new Random();

    private ExpirationTask expirationTask;
    private GameAlgorithms gameAlgorithms;
    private DataHandler dataHandler;
    private PlayerRepository playerRepository;
    private GameBuilder gameBuilder;
    private GameConfigManager gameConfigManager;
    private GameUtils gameUtils;
    private ChatGamesScheduler scheduler;
    private GameManager gameManager;
    private LogUtils logUtils;
    private RaceBuilder raceBuilder;
    private RewardManager rewardManager;
    private LanguageManager languageManager;
    private GlobalGameController globalGameController;
    private MainTask mainTask;
    private boolean isPlaceHolderAPIEnabled = false;
    private volatile boolean updateNeeded = false;
    // Set on the main thread, read by async chat threads
    @Setter private volatile boolean inGame = false;

    /**
     * Startup order matters: each step only uses what the steps before it set up.
     * configs → platform → database → player cache → game systems → listeners
     * → command → integrations → scheduled tasks.
     */
    @Override
    public void onEnable() {
        // 1. Configs and messages. Everything else reads them; messages are coloured as they load.
        MessageUtils.initialize(this);
        ConfigManager.initialize(this);

        // 2. Platform
        this.scheduler = new ChatGamesScheduler(this);
        getLogger().info("Running on Minecraft " + String.join(".", versionParts())
                + (scheduler.isFolia() ? " (Folia)" : ""));

        // 3. Database. Without it nothing can be saved, so the plugin stops here.
        if (!connectDatabase()) {
            getLogger().severe("Disabling ChatGames: the database is unavailable. Check the Storage settings in config.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Player cache, including anyone already online after a /reload
        ChatGamesPlayerManager.initialize(this, playerRepository);
        ChallengeManager.initialize(this);
        MilestoneManager.loadMilestones();
        ChatGamesPlayerManager.loadOnlinePlayers();

        // 5. Game systems
        this.languageManager = new LanguageManager(this);
        this.languageManager.load(ConfigValue.getString(ConfigPaths.LANGUAGE));
        this.expirationTask = new ExpirationTask(this);
        this.gameAlgorithms = new GameAlgorithms(this);
        this.gameBuilder = new GameBuilder(this);
        this.gameConfigManager = new GameConfigManager(this);
        this.gameUtils = new GameUtils(this);
        this.logUtils = new LogUtils(this);
        this.gameManager = new GameManager(this);
        this.raceBuilder = new RaceBuilder(this);
        this.rewardManager = new RewardManager(this);
        this.globalGameController = new GlobalGameController(this);
        this.mainTask = new MainTask(this);

        // 6. Listeners
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new LoginListener(this), this);
        pluginManager.registerEvents(new ChatListener(this), this);
        pluginManager.registerEvents(new LogListener(this), this);
        pluginManager.registerEvents(new DiscordListener(this), this);

        // 7. Command
        PluginCommand command = getCommand("chatgames");
        if (command != null) {
            new ChatGamesCommand(this).bind(command);
        }

        // 8. Integrations. The PlaceholderAPI expansion is registered in step 7 of the recode.
        this.isPlaceHolderAPIEnabled = pluginManager.getPlugin("PlaceholderAPI") != null;

        // 9. Scheduled tasks
        startGameRotation();
        scheduler.asyncTimer(ChatGamesPlayerManager::saveAll, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
        checkForUpdate();
    }

    /**
     * Shuts down in reverse: stop new games, end the running one without rewards,
     * save everyone, then close the database.
     */
    @Override
    public void onDisable() {
        if (mainTask != null) {
            mainTask.cancel();
        }
        if (globalGameController != null) {
            globalGameController.stop();
        }
        if (inGame) {
            // A running chat game has a Game object; otherwise the running round is a race
            if (gameManager.getGame() != null) {
                gameManager.endGame();
            } else {
                raceBuilder.endRace();
            }
        }
        if (scheduler != null) {
            scheduler.cancelAll();
        }

        HandlerList.unregisterAll(this);

        if (dataHandler != null) {
            // Blocks until every player is saved: the server would otherwise close the pool mid-save
            ChatGamesPlayerManager.shutdown();
            dataHandler.disable();
        }
    }

    /**
     * Reloads every configuration file and applies the changes that don't need a restart:
     * messages, the enabled games, the game rotation timer and the translation language.
     * Database settings and table names only change on restart.
     *
     * @return false if any file failed to load (those files keep their previous values)
     */
    public boolean reloadAll() {
        String oldLanguage = ConfigValue.getString(ConfigPaths.LANGUAGE);

        mainTask.cancel();
        boolean success = ConfigManager.reload();

        String newLanguage = ConfigValue.getString(ConfigPaths.LANGUAGE);
        if (newLanguage != null && !newLanguage.equalsIgnoreCase(oldLanguage)) {
            languageManager.load(newLanguage);
        }

        MilestoneManager.loadMilestones();
        mainTask.reloadEnabledGames();
        startGameRotation();
        return success;
    }

    private void startGameRotation() {
        // -1 turns automatic games off; they can still be started by command
        if (ConfigValue.getInt(ConfigPaths.TIME_MINUTES) != -1) {
            mainTask.enable();
        }
    }

    private boolean connectDatabase() {
        String configured = ConfigValue.getString(ConfigPaths.STORAGE_TYPE);
        StorageType storageType = StorageType.SQLite;
        if (configured != null && configured.trim().equalsIgnoreCase("MySQL")) {
            storageType = StorageType.MySQL;
        } else if (configured == null || !configured.trim().equalsIgnoreCase("SQLite")) {
            getLogger().warning("Unknown Storage type '" + configured + "'; using SQLite.");
        }

        this.dataHandler = new HikariDatabase(this, storageType);
        if (!dataHandler.initialize()) return false;

        String mainTable = ConfigValue.getString(ConfigPaths.TABLE_NAME);
        this.playerRepository = new PlayerRepository(dataHandler::getConnection, mainTable,
                ConfigValue.getString(ConfigPaths.FASTEST_TIMES_TABLE), mainTable + "_stats", mainTable + "_daily");
        return QueryInitializer.initialize(this, playerRepository);
    }

    private static String[] versionParts() {
        int[] version = VersionUtils.getVersion();
        return new String[]{String.valueOf(version[0]), String.valueOf(version[1]), String.valueOf(version[2])};
    }

    private void checkForUpdate() {
        if (!ConfigValue.getBoolean(ConfigPaths.CHECK_UPDATE)) return;

        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(version -> {
            this.updateNeeded = UpdateChecker.isNewer(version, getDescription().getVersion());
            if (updateNeeded) {
                getLogger().info("There is a new update available on Spigot: " + version);
            }
        });
    }
}
