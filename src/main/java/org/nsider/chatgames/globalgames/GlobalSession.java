package org.nsider.chatgames.globalgames;

import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.menus.Menus;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.chatgames.utils.messages.Msg;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One running global game: the shared goal, the time limit and what each kind of game
 * does with chat. {@link GlobalGameManager} starts and ends it, counts contributions and
 * shows the progress; subclasses turn play into contributions:
 * {@link QuestionStream} (chat games), {@link GlobalRace} and {@link CountSession}.
 *
 * <p>{@link #onChat} and {@link #onClick} run on async chat threads; everything else on
 * the main/global thread.</p>
 */
public abstract class GlobalSession {

    protected final ChatGames plugin;
    protected final GlobalGameManager manager;
    protected final GlobalGameConfig.Entry entry;
    protected final CoopProgress progress;
    private final long startNanos = System.nanoTime();
    private final List<ChatGamesScheduler.Task> tasks = new ArrayList<>();

    GlobalSession(@Nonnull ChatGames plugin, @Nonnull GlobalGameManager manager, @Nonnull GlobalGameConfig.Entry entry, int goal) {
        this.plugin = plugin;
        this.manager = manager;
        this.entry = entry;
        this.progress = new CoopProgress(goal);
    }

    /** The start announcement, sent to everyone who can play. */
    @Nonnull
    abstract Msg startMessage();

    /** Opens it for play, right after the start announcement. */
    void begin() {
    }

    /**
     * A chat message from a player who can play. Async chat thread.
     *
     * @return true to hide the message
     */
    boolean onChat(@Nonnull Player player, @Nonnull String message) {
        return false;
    }

    /**
     * {@code /chatgames <key>} from clicking a Clickable question. Async or main thread.
     *
     * @return true if the key was the current question's
     */
    boolean onClick(@Nonnull Player player, @Nonnull String key) {
        return false;
    }

    /** Cleans up after the game ended (however it ended). */
    void end() {
    }

    /** @return false where progress can go back (Count), so 25/50/75% would repeat */
    boolean announcesMilestones() {
        return true;
    }

    /** @return the goal in words for messages and logs, e.g. "64 Stone" or "1 to 20" */
    @Nonnull
    public abstract String getGoalText();

    /** Runs a task for this game only: it is cancelled when the game ends. */
    final void schedule(@Nullable ChatGamesScheduler.Task task) {
        if (task == null) return;
        synchronized (tasks) {
            tasks.add(task);
        }
    }

    final void cancelTasks() {
        synchronized (tasks) {
            for (ChatGamesScheduler.Task task : tasks) ChatGamesScheduler.cancel(task);
            tasks.clear();
        }
    }

    @Nonnull
    public GameType getType() {
        return entry.getType();
    }

    @Nonnull
    public GlobalGameConfig.Entry getEntry() {
        return entry;
    }

    @Nonnull
    public CoopProgress getProgress() {
        return progress;
    }

    public double getElapsedSeconds() {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }

    /** @return whole seconds left, never below 0 */
    public int getSecondsLeft() {
        return Math.max(0, (int) Math.ceil(entry.getDurationSeconds() - getElapsedSeconds()));
    }

    public int contribution(@Nonnull UUID player) {
        return progress.contribution(player);
    }

    /** Fills the placeholders every global message shares. */
    @Nonnull
    Msg fill(@Nonnull Msg message) {
        return message.with("%game%", Menus.gameName(getType()))
                .with("%key%", getType().getConfigKey())
                .with("%goal%", progress.getGoal())
                .with("%goal_text%", getGoalText())
                .with("%progress%", progress.getProgress())
                .with("%percent%", progress.getPercent())
                .with("%time%", getSecondsLeft())
                .with("%duration%", entry.getDurationSeconds());
    }
}
