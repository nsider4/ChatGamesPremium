package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.utils.management.TaskUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.*;

/**
 * Handles the primary task logic for managing and scheduling games.
 */
public class MainTask {

    @Nonnull
    private final ChatGames plugin;
    @Nonnull
    private final TaskUtils taskUtils;
    @Getter
    private final List<GameType> enabledGames = new ArrayList<>();
    @Getter
    private boolean taskEnabled;
    @Getter
    private BukkitTask task;

    private String activePreset;

    /**
     * Constructs a new {@code MainTask}.
     *
     * @param instance The plugin instance. Must not be null.
     */
    public MainTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
        this.taskUtils = new TaskUtils(plugin);
        this.activePreset = plugin.getConfig().getString("active-preset", null); //TODO CHANGE TO CONFIG VALUE ENUM
        if(Objects.equals(this.activePreset, "none")) this.activePreset = null;
        reloadEnabledGames();
    }

    /**
     * Enables the main task, scheduling it to run periodically.
     */
    public void enable() {
        taskUtils.cancelTask(task);
        taskEnabled = true;
        long delayTicks = plugin.getGameManager().getNextGameDelay();
        task = taskUtils.runRepeatingAsyncTask(this::doTask, delayTicks, delayTicks);
    }

    /**
     * Cancels the currently scheduled task.
     */
    public void cancel() {
        taskUtils.cancelTask(task);
        taskEnabled = false;
    }

    /**
     * Restarts the main task by canceling and enabling it again.
     */
    public void restart() {
        cancel();
        enable();
    }

    /**
     * Reloads the list of enabled games from the plugin's configuration or active preset.
     */
    public void reloadEnabledGames() {
        enabledGames.clear();

        if (activePreset != null && plugin.getConfig().isConfigurationSection("presets." + activePreset)) {
            ConfigurationSection preset = plugin.getConfig().getConfigurationSection("presets." + activePreset);
            if (preset != null) {
                for (String key : preset.getStringList("games")) {
                    try {
                        GameType type = GameType.valueOf(key.toUpperCase());
                        enabledGames.add(type);
                    } catch (IllegalArgumentException ignored) {}
                }
                return;
            }
        }

        //Default fallback if no preset or invalid
        for (GameType type : GameType.values()) {
            if (isEnabledInConfig(type)) {
                enabledGames.add(type);
            }
        }
    }

    /**
     * Performs the scheduled task logic, selecting and starting a game if conditions are met.
     */
    private void doTask() {
        if (checkEnoughPlayers()) {
            synchronized (enabledGames) {
                if (!enabledGames.isEmpty()) {
                    selectAndStartGame();
                }
            }
        }
    }

    /**
     * Checks if the number of online players meets the minimum required to start a game.
     *
     * @return True if the minimum player requirement is met, false otherwise.
     */
    public boolean checkEnoughPlayers() {
        int minPlayers = plugin.getConfig().getInt("min_players_online");
        return plugin.getGameManager().checkPlayersOnline(minPlayers);
    }

    /**
     * Selects a valid game randomly from the enabled games and starts it.
     */
    private void selectAndStartGame() {
        boolean validGameChosen = false;
        while (!validGameChosen) {
            int randomIndex = new Random().nextInt(enabledGames.size());
            GameType type = enabledGames.get(randomIndex);

            validGameChosen = true;
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGameManager().startGame(type));
        }
    }

    /**
     * Checks if a game type is enabled in the configuration.
     *
     * @param type The game type to check.
     * @return True if the game type is enabled, false otherwise.
     */
    private boolean isEnabledInConfig(@Nonnull GameType type) {
        if (plugin.getGameManager().isRace(type)) {
            return plugin.getConfigManager().getChatRaces().getBoolean(type.toString().toLowerCase() + ".enable") &&
                    plugin.getConfigManager().getChatRaces().getBoolean("enable-races");
        }
        return plugin.getConfig().getBoolean(type.toString().toLowerCase() + ".enable");
    }

    /**
     * Sets a new preset and reloads the enabled games accordingly.
     *
     * @param presetName The name of the preset to apply.
     */
    public void applyPreset(String presetName) {
        this.activePreset = presetName;
        plugin.getConfig().set("active-preset", presetName);
        plugin.saveConfig();
        reloadEnabledGames();
        restart();
    }
}