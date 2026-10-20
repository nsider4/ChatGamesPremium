package org.nsider.chatgames;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.DataHandler;
import org.nsider.chatgames.database.HikariDatabase;
import org.nsider.chatgames.database.enums.StorageType;
import org.nsider.chatgames.games.builder.GameAlgorithms;
import org.nsider.chatgames.games.builder.GameBuilder;
import org.nsider.chatgames.games.manager.GameConfigManager;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.utils.GameUtils;
import org.nsider.chatgames.globalgames.GlobalGameController;
import org.nsider.chatgames.managers.LanguageManager;
import org.nsider.chatgames.other.UpdateChecker;
import org.nsider.chatgames.races.RaceBuilder;
import org.nsider.chatgames.reward.RewardManager;
import org.nsider.chatgames.tasks.ExpirationTask;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.chatgames.utils.management.LogUtils;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;

import java.util.Random;

@Getter
public final class ChatGames extends JavaPlugin {

    private static final int SPIGOT_RESOURCE_ID = 107781;

    private final Random random = new Random();

    private ExpirationTask expirationTask;
    private GameAlgorithms gameAlgorithms;
    private DataHandler dataHandler;
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
    @Setter private boolean inGame = false;

    @Override
    public void onEnable() {
        ConfigManager.initialize(this);
        this.scheduler = new ChatGamesScheduler(this);

        this.expirationTask = new ExpirationTask(this);
        this.gameAlgorithms = new GameAlgorithms(this);
        this.gameBuilder = new GameBuilder(this);
        this.gameConfigManager = new GameConfigManager(this);
        this.gameUtils = new GameUtils(this);
        this.logUtils = new LogUtils(this);
        this.gameManager = new GameManager(this);
        this.raceBuilder = new RaceBuilder(this);
        this.rewardManager = new RewardManager(this);
        this.languageManager = new LanguageManager(this, ConfigValue.getString(ConfigPaths.LANGUAGE));
        this.globalGameController = new GlobalGameController(this);
        this.mainTask = new MainTask(this);

        this.isPlaceHolderAPIEnabled = Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;

        if(this.getConfig().getString("Storage").equalsIgnoreCase("MySQL")) {
            this.dataHandler = new HikariDatabase(this, StorageType.MySQL);
        } else if (this.getConfig().getString("Storage").equalsIgnoreCase("SQLite")) {
            this.dataHandler = new HikariDatabase(this, StorageType.SQLite);
        }
        this.dataHandler.initialize();

        checkForUpdate();
        if (ConfigValue.getInt(ConfigPaths.TIME_MINUTES) != -1) {
            mainTask.enable();
        }
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelAll();
        }
    }

    private void checkForUpdate() {
        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(version -> {
            this.updateNeeded = !getDescription().getVersion().equals(version);
            if (updateNeeded && ConfigValue.getBoolean(ConfigPaths.CHECK_UPDATE)) {
                getLogger().info("There is a new update available on Spigot!");
            }
        });
    }
}
