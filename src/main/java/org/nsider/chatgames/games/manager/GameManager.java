package org.nsider.chatgames.games.manager;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.events.ChatGamesEndEvent;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the running chat game and its scheduled tasks (time limit, hints,
 * countdown, memorise phase).
 *
 * <p>Only one chat game or race runs at a time; {@link ChatGames#isInGame()} is
 * the flag for both. A chat game goes through: {@link #tryBegin()} reserves the
 * slot, {@link #activate(Game)} opens it for answers, {@link #conclude(Game)}
 * announces the result, and {@link #endGame()} frees the slot.</p>
 */
public class GameManager {

    private final ChatGames plugin;
    @Getter private final GamePlayerManager gamePlayerManager;
    @Getter private final RoundAnnouncer announcer;
    @Getter private final AnswerHandler answerHandler;

    /** Set on the main/global thread, read by async chat threads. Null while no game takes answers. */
    @Getter @Nullable private volatile Game game;
    private final List<ChatGamesScheduler.Task> roundTasks = new ArrayList<>();
    /** The game or race started last; only meaningful while {@link ChatGames#isInGame()}. */
    @Nullable private volatile GameType lastStarted;

    public GameManager(ChatGames plugin) {
        this.plugin = plugin;
        this.gamePlayerManager = new GamePlayerManager(plugin);
        this.announcer = new RoundAnnouncer(plugin);
        this.answerHandler = new AnswerHandler(plugin);
        GameSoundManager.initialize(plugin);
    }

    /**
     * Starts a chat game or race now.
     *
     * @param args optional values for the round, e.g. the word to use, or a race's target and amount
     * @return false if something is already running, the game can't be generated, or a plugin cancelled it
     */
    public boolean startGame(@Nonnull GameType type, @Nonnull String... args) {
        return startGame(type, null, args);
    }

    /**
     * Starts a global game or race now, with its settings from global_games.yml.
     *
     * @return false if something is already running, the game can't be generated, or a plugin cancelled it
     */
    public boolean startGlobal(@Nonnull GameType type, @Nonnull GlobalGameConfig.Entry global, @Nonnull String... args) {
        return startGame(type, global, args);
    }

    private boolean startGame(GameType type, @Nullable GlobalGameConfig.Entry global, String[] args) {
        boolean started;
        if (type.isRace()) {
            try {
                started = global == null ? plugin.getRaceManager().start(type, args)
                        : plugin.getRaceManager().startGlobal(type, global, args);
            } catch (RuntimeException e) {
                // Free the slot, or no game could ever start again
                if (!plugin.getRaceManager().stop(false)) endGame();
                throw e;
            }
        } else {
            started = global == null ? plugin.getGameBuilder().start(type, args)
                    : plugin.getGameBuilder().startGlobal(type, global, args);
        }
        if (started) lastStarted = type;
        return started;
    }

    /**
     * @return the game or race running now (including a Memorize list still on screen), or null
     */
    @Nullable
    public GameType getCurrentGame() {
        return plugin.isInGame() ? lastStarted : null;
    }

    /**
     * Reserves the game slot.
     *
     * @return false if a game or race is already running
     */
    public synchronized boolean tryBegin() {
        if (plugin.isInGame()) return false;
        plugin.setInGame(true);
        return true;
    }

    /** Opens the game for answers. */
    public synchronized void activate(@Nonnull Game game) {
        this.game = game;
    }

    public synchronized void addTask(@Nonnull ChatGamesScheduler.Task task) {
        roundTasks.add(task);
    }

    /**
     * Announces the result and ends the game. Called on the main/global thread by the
     * last winner and by the time limit; whichever comes second does nothing.
     */
    public void conclude(@Nonnull Game game) {
        if (!game.finish()) return;
        cancelRoundTasks();

        Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(game.getGameType(), game.getAnswer(), game.getResultsByPlace()));
        if (game.isGlobal()) {
            plugin.getGlobalGameManager().announceEnd(game.getGameType(), game.getAnswer(), game.getResultsByPlace());
        } else if (game.hasWinners()) {
            announcer.announceWinners(game);
            // Points and times changed: the placeholders' leaderboards catch up now rather than at the next refresh
            plugin.getLeaderboardCache().refresh();
        } else {
            announcer.announceTimeExpired(game);
        }
        if (this.game == game) {
            endGame();
        }
        if (!game.isGlobal()) plugin.getGlobalGameManager().afterRegularGame();
    }

    /**
     * Stops the running chat game, if any, without announcing anything, and frees the slot.
     */
    public void endGame() {
        Game current = this.game;
        if (current != null) current.finish();
        cancelRoundTasks();
        synchronized (this) {
            this.game = null;
        }
        plugin.setInGame(false);
    }

    /**
     * Ends the running chat game (including one still in its Memorize phase) or race,
     * with no winner and no announcement. Main/global thread only.
     *
     * @return false if nothing was running
     */
    public boolean stopCurrent() {
        if (!plugin.isInGame()) return false;
        Game current;
        boolean chatGame;
        synchronized (this) {
            current = game;
            chatGame = current != null || !roundTasks.isEmpty();
        }
        if (chatGame) {
            // Listeners (and the game log) see every started game end, stopped ones included.
            // A game stopped while its Memorize list is shown has no Game yet, so nothing to report.
            if (current != null && current.finish()) {
                Bukkit.getPluginManager().callEvent(new ChatGamesEndEvent(current.getGameType(), current.getAnswer(),
                        current.getResultsByPlace()));
            }
            endGame();
        } else if (!plugin.getRaceManager().stop(true)) {
            endGame();
        }
        return true;
    }

    private synchronized void cancelRoundTasks() {
        for (ChatGamesScheduler.Task task : roundTasks) {
            ChatGamesScheduler.cancel(task);
        }
        roundTasks.clear();
    }

    public boolean gamesIsToggled(Player player) {
        if (ConfigValue.getBoolean(ConfigPaths.REQUIRE_PERMISSION_TO_PLAY) && !player.hasPermission("chatgames.play")) {
            return false;
        }
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        // Data still loading (a moment after joining): they can't win yet, as wins couldn't be recorded
        return data != null && data.isToggled();
    }

    public boolean worldEnabled(Player player) {
        for (String world : plugin.getGameConfigManager().getDisabledWorlds()) {
            if (world.equalsIgnoreCase(player.getWorld().getName())) {
                return false;
            }
        }
        return true;
    }

    public void toggleGames(Player player, boolean value) {
        ChatGamesPlayer data = ChatGamesPlayerManager.get(player.getUniqueId());
        if (data != null) {
            data.setToggled(value);
        }
    }
}
