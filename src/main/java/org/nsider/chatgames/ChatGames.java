package org.nsider.chatgames;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.nsider.chatgames.api.ChatGamesAPI;
import org.nsider.chatgames.commands.ChatGamesCommand;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.DataHandler;
import org.nsider.chatgames.database.HikariDatabase;
import org.nsider.chatgames.database.QueryInitializer;
import org.nsider.chatgames.database.enums.StorageType;
import org.nsider.chatgames.gameevents.EventManager;
import org.nsider.chatgames.menus.Menus;
import org.nsider.chatgames.games.builder.GameBuilder;
import org.nsider.chatgames.games.manager.GameConfigManager;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.utils.GameUtils;
import org.nsider.chatgames.hooks.MythicHook;
import org.nsider.chatgames.globalgames.GlobalGameManager;
import org.nsider.chatgames.listeners.ChatListener;
import org.nsider.chatgames.discord.DiscordManager;
import org.nsider.chatgames.listeners.LogListener;
import org.nsider.chatgames.listeners.LoginListener;
import org.nsider.chatgames.managers.LanguageManager;
import org.nsider.chatgames.config.ConfigLanguage;
import org.nsider.chatgames.managers.MilestoneManager;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.PlayerRepository;
import org.nsider.chatgames.other.UpdateChecker;
import org.nsider.chatgames.placeholders.ChatGamesExpansion;
import org.nsider.chatgames.placeholders.LeaderboardCache;
import org.nsider.chatgames.races.RaceListener;
import org.nsider.chatgames.progression.ProgressionManager;
import org.nsider.chatgames.races.RaceManager;
import org.nsider.chatgames.registry.ChatGamesApiImpl;
import org.nsider.chatgames.registry.GameRegistry;
import org.nsider.chatgames.reward.RewardManager;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.chatgames.utils.gameplay.VersionUtils;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.management.LogUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Random;

/**
 * The plugin: creates every manager on enable, in dependency order, and shuts them down on
 * disable. {@link #reloadAll()} is {@code /chatgames reload}.
 */
@Getter
public final class ChatGames extends JavaPlugin {

    private static final int SPIGOT_RESOURCE_ID = 107781;
    /** Cached player data is written back every 5 minutes, so a crash loses little. */
    private static final long AUTOSAVE_TICKS = 5 * 60 * 20L;
    // Servers stay up for weeks, so one check at startup isn't enough
    private static final long UPDATE_CHECK_TICKS = 12 * 60 * 60 * 20L;

    private final Random random = new Random();

    private DataHandler dataHandler;
    private PlayerRepository playerRepository;
    private GameBuilder gameBuilder;
    private GameConfigManager gameConfigManager;
    private GameUtils gameUtils;
    private ChatGamesScheduler scheduler;
    private GameManager gameManager;
    private LogUtils logUtils;
    private RaceManager raceManager;
    private ProgressionManager progressionManager;
    private RewardManager rewardManager;
    private LanguageManager languageManager;
    private ConfigLanguage configLanguage;
    private GlobalGameManager globalGameManager;
    private EventManager eventManager;
    private DiscordManager discordManager;
    private Menus menus;
    private MainTask mainTask;
    private GameRegistry gameRegistry;
    @Nullable private ChatGamesCommand chatGamesCommand;
    private LeaderboardCache leaderboardCache;
    private ChatGamesExpansion placeholderExpansion;
    private boolean isPlaceHolderAPIEnabled = false;
    private volatile boolean updateNeeded = false;
    // Set by the update check (async), read on join
    @Nullable private volatile String latestVersion;
    private UpdateChecker updateChecker;
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
        MilestoneManager.loadMilestones();
        ChatGamesPlayerManager.loadOnlinePlayers();
        this.leaderboardCache = new LeaderboardCache(this, playerRepository);

        // 5. Game systems. MythicMobs first: races.yml's mythic: targets are checked as races load
        MythicHook.initialize(getLogger());
        this.gameRegistry = new GameRegistry(this);
        this.languageManager = new LanguageManager(this);
        this.languageManager.load(ConfigValue.getString(ConfigPaths.LANGUAGE));
        this.configLanguage = new ConfigLanguage(this);
        this.gameBuilder = new GameBuilder(this);
        this.gameConfigManager = new GameConfigManager(this);
        this.gameUtils = new GameUtils(this);
        this.logUtils = new LogUtils(this);
        this.gameManager = new GameManager(this);
        this.raceManager = new RaceManager(this);
        this.progressionManager = new ProgressionManager(this);
        this.rewardManager = new RewardManager(this);
        this.globalGameManager = new GlobalGameManager(this);
        this.eventManager = new EventManager(this);
        this.discordManager = new DiscordManager(this);
        this.menus = new Menus(this);
        this.mainTask = new MainTask(this);

        // 6. Listeners
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new LoginListener(this), this);
        pluginManager.registerEvents(new ChatListener(this), this);
        pluginManager.registerEvents(new LogListener(this), this);
        pluginManager.registerEvents(new RaceListener(this), this);
        pluginManager.registerEvents(discordManager, this);
        pluginManager.registerEvents(eventManager, this);
        pluginManager.registerEvents(gameRegistry, this);

        // 7. Command
        PluginCommand command = getCommand("chatgames");
        if (command != null) {
            this.chatGamesCommand = new ChatGamesCommand(this);
            chatGamesCommand.bind(command);
        }

        // 8. Integrations
        this.isPlaceHolderAPIEnabled = pluginManager.getPlugin("PlaceholderAPI") != null;
        if (isPlaceHolderAPIEnabled) {
            this.placeholderExpansion = new ChatGamesExpansion(this);
            placeholderExpansion.register();
        }
        // After the integrations: it checks for DiscordSRV and PlaceholderAPI
        discordManager.load();

        // The API, for plugins that add games (they enable after ChatGames)
        getServer().getServicesManager().register(ChatGamesAPI.class, new ChatGamesApiImpl(this, gameRegistry), this, ServicePriority.Normal);

        // 9. Scheduled tasks. The first tick runs once every plugin has enabled.
        scheduler.global(gameRegistry::markStarted);
        startGameRotation();
        leaderboardCache.start();
        scheduler.asyncTimer(ChatGamesPlayerManager::saveAll, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
        checkForUpdate();
        configLanguage.check();
    }

    /**
     * Shuts down in reverse: stop new games, end the running one without rewards,
     * save everyone, then close the database.
     */
    @Override
    public void onDisable() {
        if (menus != null) {
            menus.closeAll();
        }
        if (mainTask != null) {
            mainTask.cancel();
        }
        if (globalGameManager != null) {
            globalGameManager.shutdown();
        }
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        if (leaderboardCache != null) {
            leaderboardCache.stop();
        }
        if (inGame && !raceManager.stop(false)) {
            gameManager.endGame();
        }
        if (scheduler != null) {
            scheduler.cancelAll();
        }
        if (discordManager != null) {
            // Gives messages already queued a moment to go out
            discordManager.shutdown();
        }

        HandlerList.unregisterAll(this);
        getServer().getServicesManager().unregisterAll(this);

        if (dataHandler != null) {
            // Blocks until every player is saved: the server would otherwise close the pool mid-save
            ChatGamesPlayerManager.shutdown();
            dataHandler.disable();
        }
    }

    /**
     * Reloads every configuration file and applies the changes that don't need a restart:
     * messages, the enabled games, race targets, the game rotation timer and the translation language.
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
        configLanguage.check();

        MilestoneManager.loadMilestones();
        raceManager.load();
        progressionManager.load();
        globalGameManager.load();
        eventManager.load();
        discordManager.load();
        menus.reload();
        mainTask.reloadEnabledGames();
        startGameRotation();
        leaderboardCache.start();
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

        // Table names go into the SQL as written, so only plain identifiers are accepted
        String mainTable = ConfigValue.getString(ConfigPaths.TABLE_NAME);
        String fastestTable = ConfigValue.getString(ConfigPaths.FASTEST_TIMES_TABLE);
        if (!isTableName(mainTable) || !isTableName(fastestTable)) {
            getLogger().severe("table_name and fastest_times_table may only use letters, digits and underscores"
                    + " (found '" + mainTable + "' and '" + fastestTable + "').");
            return false;
        }

        this.dataHandler = new HikariDatabase(this, storageType);
        if (!dataHandler.initialize()) return false;

        this.playerRepository = new PlayerRepository(dataHandler::getConnection, mainTable,
                fastestTable, mainTable + "_stats");
        return QueryInitializer.initialize(this, playerRepository);
    }

    private static boolean isTableName(String name) {
        // Leaves room for the _stats, _upgrading and _1_9_9 suffixes within MySQL's 64 characters
        return name != null && name.matches("[A-Za-z0-9_]{1,48}");
    }

    private static String[] versionParts() {
        int[] version = VersionUtils.getVersion();
        return new String[]{String.valueOf(version[0]), String.valueOf(version[1]), String.valueOf(version[2])};
    }

    private void checkForUpdate() {
        if (!ConfigValue.getBoolean(ConfigPaths.CHECK_UPDATE)) return;

        updateChecker = new UpdateChecker(this, SPIGOT_RESOURCE_ID);
        scheduler.asyncTimer(() -> {
            String version = updateChecker.fetchVersion();
            if (version == null || !UpdateChecker.isNewer(version, getDescription().getVersion())) return;
            // Logged once per new version, not on every check
            if (!version.equals(latestVersion)) {
                getLogger().info("ChatGames-Premium " + version + " is available on Spigot (this server runs "
                        + getDescription().getVersion() + "): " + updateChecker.getResourceUrl());
            }
            latestVersion = version;
            updateNeeded = true;
        }, 1L, UPDATE_CHECK_TICKS);
    }
}