package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.logging.Level;

/**
 * Handles the primary task logic for managing and scheduling games.
 */
public class MainTask {

    @Nonnull
    private final ChatGames plugin;
    @Getter
    private final List<GameType> enabledGames = new ArrayList<>();
    @Getter
    private volatile boolean taskEnabled;
    @Getter
    private volatile ChatGamesScheduler.Task task;

    private String activePreset;
    /** Set and read on the main/global thread only. */
    private GameType lastGame;

    /**
     * Constructs a new {@code MainTask}.
     *
     * @param instance The plugin instance. Must not be null.
     */
    public MainTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
        this.activePreset = plugin.getConfig().getString("active-preset", null); //TODO CHANGE TO CONFIG VALUE ENUM
        if(Objects.equals(this.activePreset, "none")) this.activePreset = null;
        reloadEnabledGames();
    }

    /**
     * Enables the main task, scheduling it to run periodically.
     */
    public void enable() {
        ChatGamesScheduler.cancel(task);
        taskEnabled = true;
        scheduleNext();
    }

    /**
     * Schedules the next round. Each round asks for a fresh delay, so a random
     * min/max interval really varies between games.
     */
    private void scheduleNext() {
        long delayTicks = plugin.getGameConfigManager().getNextGameDelay();
        task = plugin.getScheduler().globalLater(() -> {
            if (!taskEnabled) return;
            try {
                doTask();
            } catch (RuntimeException e) {
                // One broken game must not stop the rotation for good
                plugin.getLogger().log(Level.SEVERE, "A game failed to start; the rotation continues with the next one.", e);
            } finally {
                scheduleNext();
            }
        }, delayTicks);
    }

    /**
     * Cancels the currently scheduled task.
     */
    public void cancel() {
        ChatGamesScheduler.cancel(task);
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
                    GameType type = GameType.fromName(key);
                    if (type != null) enabledGames.add(type);
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
     * Performs one round on the main thread (global region on Folia): starts a game if conditions are met.
     */
    private void doTask() {
        // A game started by command, or a long game, may still be running: skip this round
        if (plugin.isInGame() || !checkEnoughPlayers()) return;
        GameType type;
        synchronized (enabledGames) {
            type = pickGame(enabledGames, lastGame, plugin.getRandom(), this::weightOf);
        }
        if (type != null && plugin.getGameManager().startGame(type)) {
            lastGame = type;
        }
    }

    /**
     * Checks if the number of online players meets the minimum required to start a game.
     *
     * @return True if the minimum player requirement is met, false otherwise.
     */
    public boolean checkEnoughPlayers() {
        int minPlayers = ConfigValue.getInt(ConfigPaths.MIN_PLAYERS);
        return plugin.getGameManager().getGamePlayerManager().checkMinimumPlayersOnline(minPlayers);
    }

    /**
     * Picks a game at random, weighted by each game's {@code weight}, never the
     * previous game twice in a row unless it is the only one enabled.
     *
     * @return null if no game has a weight above 0
     */
    @Nullable
    static GameType pickGame(@Nonnull List<GameType> games, @Nullable GameType previous, @Nonnull Random random,
                             @Nonnull ToIntFunction<GameType> weightOf) {
        List<GameType> candidates = new ArrayList<>(games);
        if (candidates.size() > 1) candidates.remove(previous);
        int total = 0;
        int[] weights = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            weights[i] = Math.max(0, weightOf.applyAsInt(candidates.get(i)));
            total += weights[i];
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights[i];
            if (roll < 0) return candidates.get(i);
        }
        return null;
    }

    private int weightOf(GameType type) {
        if (type.isRace()) {
            return Math.max(0, ConfigManager.getRaces().getInt(type.getConfigKey() + ".weight", 1));
        }
        return plugin.getGameConfigManager().settings(type).getWeight();
    }

    /**
     * Checks if a game type is enabled in the configuration.
     *
     * @param type The game type to check.
     * @return True if the game type is enabled, false otherwise.
     */
    private boolean isEnabledInConfig(@Nonnull GameType type) {
        if (type.isRace()) {
            return ConfigValue.getBoolean(ConfigPaths.RACES_ENABLED)
                    && ConfigManager.getRaces().getBoolean(type.getConfigKey() + ".enable");
        }
        return plugin.getGameConfigManager().settings(type).isEnabled();
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