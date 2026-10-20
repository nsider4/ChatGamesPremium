package org.nsider.chatgames.utils.management;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.function.Consumer;

/**
 * The plugin's only entry point for scheduling work.
 *
 * <p>Spigot and Paper have one main thread. Folia has none: world state belongs to
 * regions that tick in parallel, and each player can only be touched from the
 * region they are in. This class hides that difference, so every call site states
 * <em>what</em> it touches and runs correctly on both:</p>
 * <ul>
 *   <li>{@code global*}: server-wide state such as starting games, console
 *   commands and plugin events. The main thread on Spigot/Paper, the global
 *   region on Folia.</li>
 *   <li>{@code atPlayer} / {@code forEachPlayer}: anything that changes a player,
 *   like giving items, playing sounds or making them run a command.</li>
 *   <li>{@code async*}: database and web requests. Never touch the world here.</li>
 * </ul>
 *
 * <p>Delays and periods are in ticks (20 per second).</p>
 */
public final class ChatGamesScheduler {

    private final FoliaLib foliaLib;
    private final PlatformScheduler scheduler;

    public ChatGamesScheduler(@Nonnull ChatGames plugin) {
        this.foliaLib = new FoliaLib(plugin);
        this.scheduler = foliaLib.getScheduler();
    }

    /**
     * @return true when the server runs Folia's regionised threading
     */
    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    /* ------------------------------------------ global ------------------------------------------ */

    /**
     * Runs a task on the next tick of the main thread (global region on Folia).
     */
    public void global(@Nonnull Runnable task) {
        scheduler.runNextTick(ignored -> task.run());
    }

    /**
     * Runs a task on the main thread (global region on Folia) after a delay.
     */
    @Nonnull
    public Task globalLater(@Nonnull Runnable task, long delayTicks) {
        // Folia rejects delays below one tick
        return new Task(scheduler.runLater(task, Math.max(1L, delayTicks)));
    }

    /**
     * Repeats a task on the main thread (global region on Folia).
     */
    @Nonnull
    public Task globalTimer(@Nonnull Runnable task, long delayTicks, long periodTicks) {
        return new Task(scheduler.runTimer(task, Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    /* ------------------------------------------ players ----------------------------------------- */

    /**
     * Runs a task on the thread that owns this player. Skipped if the player logs out first.
     */
    public void atPlayer(@Nonnull Player player, @Nonnull Runnable task) {
        scheduler.runAtEntity(player, ignored -> task.run());
    }

    /**
     * Runs {@code action} once for every online player, each on the thread that owns that player.
     */
    public void forEachPlayer(@Nonnull Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            atPlayer(player, () -> action.accept(player));
        }
    }

    /* ------------------------------------------ async ------------------------------------------- */

    /**
     * Runs a task off the server threads, for database or web requests.
     */
    public void async(@Nonnull Runnable task) {
        scheduler.runAsync(ignored -> task.run());
    }

    /**
     * Runs a task off the server threads after a delay.
     */
    @Nonnull
    public Task asyncLater(@Nonnull Runnable task, long delayTicks) {
        return new Task(scheduler.runLaterAsync(task, Math.max(1L, delayTicks)));
    }

    /**
     * Repeats a task off the server threads.
     */
    @Nonnull
    public Task asyncTimer(@Nonnull Runnable task, long delayTicks, long periodTicks) {
        return new Task(scheduler.runTimerAsync(task, Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    /* ------------------------------------------ lifecycle --------------------------------------- */

    /**
     * Cancels every task this plugin scheduled. Called on disable.
     */
    public void cancelAll() {
        scheduler.cancelAllTasks();
    }

    /**
     * Cancels a task if there is one.
     */
    public static void cancel(@Nullable Task task) {
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * A scheduled task that can be cancelled. Wraps the platform's own task type
     * so the rest of the plugin doesn't depend on FoliaLib.
     */
    public static final class Task {

        private final WrappedTask handle;

        private Task(WrappedTask handle) {
            this.handle = handle;
        }

        public void cancel() {
            handle.cancel();
        }

        public boolean isCancelled() {
            return handle.isCancelled();
        }
    }
}
