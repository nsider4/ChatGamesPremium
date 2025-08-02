package org.nsider.chatgames.utils.management;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.nsider.chatgames.ChatGames;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * Utility class to manage and schedule tasks for the plugin.
 */
public class TaskUtils {

    @Nonnull
    private final ChatGames plugin;

    /**
     * Constructs a {@code TaskHelper} instance.
     *
     * @param instance The plugin instance. Must not be null.
     * @throws IllegalArgumentException if the plugin instance is null.
     */
    public TaskUtils(@Nonnull ChatGames instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null.");
        }
        this.plugin = instance;
    }

    /**
     * Schedules an asynchronous task to run after a specified delay.
     *
     * @param task  The task to execute. Must not be null.
     * @param delay The delay (in ticks) before the task is run.
     * @return The {@link BukkitTask} representing the scheduled task.
     */
    @Nonnull
    public BukkitTask runAsyncTask(@Nonnull Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
    }

    /**
     * Schedules a synchronous task to run after a specified delay.
     *
     * @param task  The task to execute. Must not be null.
     * @param delay The delay (in ticks) before the task is run.
     * @return The {@link BukkitTask} representing the scheduled task.
     */
    @Nonnull
    public BukkitTask runSyncTask(@Nonnull Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    /**
     * Schedules an asynchronous repeating task.
     *
     * @param task   The task to execute. Must not be null.
     * @param delay  The delay (in ticks) before the first execution.
     * @param period The interval (in ticks) between subsequent executions.
     * @return The {@link BukkitTask} representing the scheduled task.
     */
    @Nonnull
    public BukkitTask runRepeatingAsyncTask(@Nonnull Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }

    /**
     * Schedules a synchronous repeating task.
     *
     * @param task   The task to execute. Must not be null.
     * @param delay  The delay (in ticks) before the first execution.
     * @param period The interval (in ticks) between subsequent executions.
     * @return The {@link BukkitTask} representing the scheduled task.
     */
    @Nonnull
    public BukkitTask runRepeatingSyncTask(@Nonnull Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    /**
     * Cancels a given task if it is not null.
     *
     * @param task The {@link BukkitTask} to cancel. Can be null.
     */
    public void cancelTask(@Nullable BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
