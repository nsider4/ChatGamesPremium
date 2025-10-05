package org.nsider.chatgames;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.database.DataHandler;
import org.nsider.chatgames.database.HikariDatabase;
import org.nsider.chatgames.database.enums.StorageType;
import org.nsider.chatgames.games.builder.GameAlgorithms;
import org.nsider.chatgames.games.builder.GameBuilder;
import org.nsider.chatgames.games.manager.GameConfigManager;
import org.nsider.chatgames.games.manager.GameManager;
import org.nsider.chatgames.games.utils.GameUtils;
import org.nsider.chatgames.managers.LanguageManager;
import org.nsider.chatgames.other.UpdateChecker;
import org.nsider.chatgames.races.RaceBuilder;
import org.nsider.chatgames.reward.RewardManager;
import org.nsider.chatgames.tasks.ExpirationTask;
import org.nsider.chatgames.tasks.MainTask;
import org.nsider.chatgames.utils.gameplay.CommandUtils;
import org.nsider.chatgames.utils.gameplay.VersionUtils;
import org.nsider.chatgames.utils.management.LogUtils;
import org.nsider.chatgames.utils.management.TaskUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.io.File;
import java.util.Random;

@Getter
public final class ChatGames extends JavaPlugin {

    private final Random random = new Random();

    private GameAlgorithms gameAlgorithms;
    private DataHandler dataHandler;
    private GameBuilder gameBuilder;
    private GameConfigManager gameConfigManager;
    private GameUtils gameUtils;
    private TaskUtils taskUtils;
    private GameManager gameManager;
    private LogUtils logUtils;
    private RaceBuilder raceBuilder;
    private MainTask mainTask;
    private ExpirationTask expirationTask;
    private RewardManager rewardManager;
    private LanguageManager languageManager;
    private CommandUtils commandUtils;
    private boolean isPlaceHolderAPIEnabled = false;
    private boolean updateNeeded;
    @Setter private boolean inGame = false;

    @Override
    public void onEnable() {
        File config = new File(getDataFolder()+File.separator+"config.yml");
        if (!config.exists()) {
            saveDefaultConfig();
        }

        MessageUtils.initialize(this);
        ConfigManager.initialize(this);

        this.gameAlgorithms = new GameAlgorithms(this);
        this.gameBuilder = new GameBuilder(this);
        this.gameConfigManager = new GameConfigManager(this);
        this.gameUtils = new GameUtils(this);
        this.taskUtils = new TaskUtils(this);
        this.logUtils = new LogUtils(this);
        this.gameManager = new GameManager(this);
        this.raceBuilder = new RaceBuilder(this);
        this.mainTask = new MainTask(this);
        this.expirationTask = new ExpirationTask(this);
        this.rewardManager = new RewardManager(this);
        this.commandUtils = new CommandUtils();
        this.languageManager = new LanguageManager(this, getConfig().getString("lang"));
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            //new PlaceHolderHook(this).register();
            getLogger().info("PlaceholderAPI Hooked!");
            this.isPlaceHolderAPIEnabled = true;
        }

        if(this.getConfig().getString("Storage").equalsIgnoreCase("MySQL")) {
            this.dataHandler = new HikariDatabase(this, StorageType.MySQL);
        } else if (this.getConfig().getString("Storage").equalsIgnoreCase("SQLite")) {
            this.dataHandler = new HikariDatabase(this, StorageType.SQLite);
        }
        this.dataHandler.initialize();

        VersionUtils.correctConfigs(getConfig(), ConfigManager.getRaces());
        checkForUpdate();
        if(getConfig().getInt("time_minutes") != -1) { //DO IN THE OTHER CLASS
            mainTask.enable();
        }

        loadRaces();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadRaces() {
        if (ConfigManager.getRaces().getBoolean("hunt.enable"))
            this.raceBuilder.setHuntData(this.raceBuilder.getRaceDataLoader().loadRace("hunt"));
        if (ConfigManager.getRaces().getBoolean("mine.enable"))
            this.raceBuilder.setMineData(this.raceBuilder.getRaceDataLoader().loadRace("mine"));
        if (ConfigManager.getRaces().getBoolean("place.enable"))
            this.raceBuilder.setPlaceData(this.raceBuilder.getRaceDataLoader().loadRace("place"));
        if (ConfigManager.getRaces().getBoolean("fish.enable"))
            this.raceBuilder.setFishData(this.raceBuilder.getRaceDataLoader().loadRace("fish"));
        if (ConfigManager.getRaces().getBoolean("eat.enable"))
            this.raceBuilder.setEatData(this.raceBuilder.getRaceDataLoader().loadRace("eat"));
        if (ConfigManager.getRaces().getBoolean("craft.enable"))
            this.raceBuilder.setCraftData(this.raceBuilder.getRaceDataLoader().loadRace("craft"));
        if (ConfigManager.getRaces().getBoolean("furnace.enable"))
            this.raceBuilder.setFurnaceData(this.raceBuilder.getRaceDataLoader().loadRace("furnace"));
    }

    public boolean checkForUpdate() {
        (new UpdateChecker(this, 107781)).getVersion(version -> {
            if (getDescription().getVersion().equals(version)) {
                this.updateNeeded = false;
            } else {
                this.updateNeeded = true;
                if (getConfig().getBoolean("check-update"))
                    Bukkit.getConsoleSender().sendMessage(MessageUtils.color("&8[%prefix%&8] &fThere is a new update available on Spigot!"));
            }
        });
        return this.updateNeeded;
    }
}
