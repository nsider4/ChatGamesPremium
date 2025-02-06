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
import org.nsider.chatgames.races.RaceBuilder;
import org.nsider.chatgames.utils.management.LogUtils;

import java.io.File;

public final class ChatGames extends JavaPlugin {

    @Getter private GameAlgorithms gameAlgorithms;
    @Getter private DataHandler dataHandler;
    @Getter private GameBuilder gameBuilder;
    @Getter private GameConfigManager gameConfigManager;
    @Getter private GameUtils gameUtils;
    @Getter private GameManager gameManager;
    @Getter private LogUtils logUtils;
    @Getter private RaceBuilder raceBuilder;
    @Getter private boolean isPlaceHolderAPIEnabled = false;
    @Getter @Setter private boolean inGame = false;

    @Override
    public void onEnable() {
        File config = new File(getDataFolder()+File.separator+"config.yml");
        if (!config.exists()) {
            saveDefaultConfig();
        }

        this.gameAlgorithms = new GameAlgorithms(this);
        this.gameBuilder = new GameBuilder(this);
        this.gameConfigManager = new GameConfigManager(this);
        this.gameUtils = new GameUtils(this);
        this.logUtils = new LogUtils(this);
        this.gameManager = new GameManager(this);
        this.raceBuilder = new RaceBuilder(this);

        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceHolderHook(this).register();
            getLogger().info("PlaceholderAPI Hooked!");
            this.isPlaceHolderAPIEnabled = true;
        }

        if(this.getConfig().getString("Storage").equalsIgnoreCase("MySQL")) {
            this.dataHandler = new HikariDatabase(this, StorageType.MySQL);
        } else if (this.getConfig().getString("Storage").equalsIgnoreCase("SQLite")) {
            this.dataHandler = new HikariDatabase(this, StorageType.SQLite);
        }
        this.dataHandler.initialize();

        VersionUtils.correctConfigs(getConfig(), ConfigManager.getChatRaces());
        checkForUpdate();
        if(getConfig().getInt("time_minutes") != -1) { //DO IN THE OTHER CLASS
            mainTask.enable();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
