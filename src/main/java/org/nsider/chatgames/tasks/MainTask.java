package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.annotations.Nonnull;
import org.nsider.chatgames.utils.management.TaskUtils;
import org.nsider.chatgamespremium.enums.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Constructs a new {@code MainTask}.
     *
     * @param instance The plugin instance. Must not be null.
     */
    public MainTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
        this.taskUtils = new TaskUtils(plugin);
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
     * Reloads the list of enabled games from the plugin's configuration.
     */
    public void reloadEnabledGames() {
        enabledGames.clear();
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
            int randomIndex = ThreadLocalRandom.current().nextInt(GameType.values().length);
            GameType type = GameType.values()[randomIndex];

            if (enabledGames.contains(type)) {
                validGameChosen = true;
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGameManager().startGame(type));
            }
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
}
