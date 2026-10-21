package org.nsider.chatgames.tasks;

import lombok.Getter;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.managers.PresetManager;
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
    /** Replaced as a whole on reload, never changed in place, so readers on other threads see a full list. */
    @Getter
    private volatile List<GameType> enabledGames = Collections.emptyList();
    @Getter
    private volatile boolean taskEnabled;
    @Getter
    private volatile ChatGamesScheduler.Task task;

    /** When the next scheduled round runs, in epoch millis; read by placeholders on any thread. */
    private volatile long nextRoundAt;
    /** Set and read on the main/global thread only. */
    private GameType lastGame;

    /**
     * Constructs a new {@code MainTask}.
     *
     * @param instance The plugin instance. Must not be null.
     */
    public MainTask(@Nonnull ChatGames instance) {
        this.plugin = instance;
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
        nextRoundAt = System.currentTimeMillis() + delayTicks * 50L;
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
     * @return whole seconds until the rotation's next round, or -1 if the rotation is off
     */
    public long getSecondsUntilNextGame() {
        if (!taskEnabled) return -1;
        return Math.max(0, (nextRoundAt - System.currentTimeMillis() + 999) / 1000);
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
     * Reloads the enabled games: the active preset's, or else every game enabled in games.yml / races.yml.
     */
    public void reloadEnabledGames() {
        List<GameType> games = new ArrayList<>();
        String preset = PresetManager.getActive();
        if (preset != null) {
            for (GameType type : PresetManager.getGames(preset, plugin.getLogger())) {
                // Races stay off when races are turned off globally, preset or not
                if (type.isGlobalOnly()) continue;
                if (!type.isRace() || ConfigValue.getBoolean(ConfigPaths.RACES_ENABLED)) games.add(type);
            }
        } else {
            for (GameType type : GameType.values()) {
                if (isEnabledInConfig(type)) games.add(type);
            }
        }
        enabledGames = Collections.unmodifiableList(games);
    }

    /**
     * Performs one round on the main thread (global region on Folia): starts a game if conditions are met.
     */
    private void doTask() {
        // A game started by command, or a long game, may still be running: skip this round.
        // A game event plays its own games.
        if (plugin.isInGame() || plugin.getEventManager().isRunning() || !checkEnoughPlayers()) return;
        GameType type = pickNext();
        if (type != null && plugin.getGameManager().startGame(type)) {
            lastGame = type;
        }
    }

    /**
     * Picks the next game the way the rotation does (weighted, no repeat), without starting it.
     *
     * @return null if no game is enabled
     */
    @Nullable
    public GameType pickNext() {
        return pickGame(enabledGames, lastGame, plugin.getRandom(), this::weightOf);
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
        if (type.isGlobalOnly()) return false;
        if (type.isRace()) {
            return ConfigValue.getBoolean(ConfigPaths.RACES_ENABLED)
                    && ConfigManager.getRaces().getBoolean(type.getConfigKey() + ".enable");
        }
        return plugin.getGameConfigManager().settings(type).isEnabled();
    }

    /**
     * Makes a preset active (null for none) and switches the rotation to its games.
     * The running game, if any, finishes normally.
     */
    public void applyPreset(@Nullable String presetName) {
        PresetManager.setActive(presetName);
        reloadEnabledGames();
        if (taskEnabled) restart();
    }
}