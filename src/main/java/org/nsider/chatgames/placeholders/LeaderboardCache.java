package org.nsider.chatgames.placeholders;

import org.bukkit.Bukkit;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.player.PlayerRepository;
import org.nsider.chatgames.utils.management.ChatGamesScheduler;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Keeps a {@link LeaderboardSnapshot} for the placeholders, rebuilt from the database
 * every {@code placeholders.refresh-seconds} and after each game with winners.
 *
 * <p>A rebuild first saves online players and then reads on the player-data thread,
 * so it sees their latest points. Readers always get a complete snapshot: the new one
 * replaces the old one in a single write.</p>
 */
public final class LeaderboardCache {

    private static final int DEFAULT_REFRESH_SECONDS = 60;

    private final ChatGames plugin;
    private final PlayerRepository repository;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile LeaderboardSnapshot snapshot = LeaderboardSnapshot.EMPTY;
    @Nullable private ChatGamesScheduler.Task task;

    public LeaderboardCache(@Nonnull ChatGames plugin, @Nonnull PlayerRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @Nonnull
    public LeaderboardSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Builds the first snapshot now and schedules the rest; call again after a reload
     * to pick up a new interval.
     */
    public void start() {
        ChatGamesScheduler.cancel(task);
        int seconds = ConfigValue.getInt(ConfigPaths.PLACEHOLDER_REFRESH_SECONDS);
        if (seconds <= 0) seconds = DEFAULT_REFRESH_SECONDS;
        long period = seconds * 20L;
        refresh();
        task = plugin.getScheduler().asyncTimer(this::refresh, period, period);
    }

    public void stop() {
        ChatGamesScheduler.cancel(task);
        task = null;
    }

    /**
     * Rebuilds the snapshot in the background. Does nothing if a rebuild is already running.
     */
    public void refresh() {
        if (!refreshing.compareAndSet(false, true)) return;
        ChatGamesPlayerManager.saveAll();
        ChatGamesPlayerManager.runAfterSaves(() -> {
            try {
                snapshot = repository.readLeaderboards(uuid -> Bukkit.getOfflinePlayer(uuid).getName());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Could not refresh the leaderboards for placeholders", e);
            } finally {
                refreshing.set(false);
            }
        });
    }
}
