package org.nsider.chatgames.model.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.gameplay.MilestoneUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Keeps online players' data in memory and moves it to and from the database.
 *
 * <p>Game code reads and changes {@link ChatGamesPlayer} objects only; it never
 * waits on the database. All database work for players runs on one dedicated
 * thread, in submission order. That ordering is what makes a quick
 * leave-and-rejoin safe: the save from the quit always finishes before the load
 * from the join starts.</p>
 */
public final class ChatGamesPlayerManager {

    private static final Map<UUID, ChatGamesPlayer> cache = new ConcurrentHashMap<>();
    private static ChatGames plugin;
    private static PlayerRepository repository;
    private static ExecutorService io;

    private ChatGamesPlayerManager() {}

    public static void initialize(@Nonnull ChatGames instance, @Nonnull PlayerRepository playerRepository) {
        plugin = instance;
        repository = playerRepository;
        io = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ChatGames-PlayerData");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * @return the player's data, or null if they are offline or still loading
     */
    @Nullable
    public static ChatGamesPlayer get(UUID uuid) {
        return cache.get(uuid);
    }

    public static boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    /**
     * Loads a player who just joined. Their data appears in {@link #get} a moment later.
     */
    public static void load(@Nonnull OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        io.execute(() -> {
            try {
                ChatGamesPlayer data = repository.load(uuid);
                // They may have left while this was loading; their quit already ran, so don't cache them
                if (Bukkit.getPlayer(uuid) != null) {
                    cache.put(uuid, data);
                    // Milestones reached while offline (points given by an admin)
                    MilestoneUtils.payPendingLater(uuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load ChatGames data for " + player.getName(), e);
            }
        });
    }

    /**
     * Loads everyone already online, for when the plugin is enabled or reloaded mid-game.
     */
    public static void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            load(player);
        }
    }

    /**
     * Saves and forgets a player who left.
     */
    public static void unload(@Nonnull OfflinePlayer player) {
        ChatGamesPlayer data = cache.remove(player.getUniqueId());
        if (data != null) {
            save(data);
        }
    }

    /**
     * Queues a save of whatever changed.
     */
    public static void save(@Nonnull ChatGamesPlayer data) {
        ChatGamesPlayer.Snapshot snapshot = data.snapshot();
        io.execute(() -> write(data, snapshot));
    }

    /**
     * Saves every cached player in the background. Used by the periodic autosave.
     */
    public static void saveAll() {
        for (ChatGamesPlayer data : cache.values()) {
            save(data);
        }
    }

    /**
     * Saves everyone, waits for every queued save to finish and stops the data thread.
     * Only for shutdown.
     */
    public static void shutdown() {
        if (io == null) return;
        saveAll();
        cache.clear();
        io.shutdown();
        try {
            if (!io.awaitTermination(30, TimeUnit.SECONDS)) {
                plugin.getLogger().severe("Timed out saving player data; some recent changes may be lost.");
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs a task on the player-data thread after every save queued so far, e.g. a
     * leaderboard query that must see the latest points.
     */
    public static void runAfterSaves(@Nonnull Runnable task) {
        io.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "A ChatGames data task failed", e);
            }
        });
    }

    /**
     * Reads a player who may be offline, from the cache if they are online.
     * The callback runs on the player-data thread, with null if loading failed.
     */
    public static void loadOffline(@Nonnull UUID uuid, @Nonnull Consumer<ChatGamesPlayer> callback) {
        ChatGamesPlayer cached = cache.get(uuid);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        io.execute(() -> {
            try {
                callback.accept(repository.load(uuid));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load ChatGames data for " + uuid, e);
                callback.accept(null);
            }
        });
    }

    /**
     * Changes a player who may be offline: the change is applied to their cached
     * data if online, otherwise loaded, changed and saved straight back.
     */
    public static void modify(@Nonnull UUID uuid, @Nonnull Consumer<ChatGamesPlayer> change) {
        ChatGamesPlayer cached = cache.get(uuid);
        if (cached != null) {
            change.accept(cached);
            return;
        }
        io.execute(() -> {
            try {
                ChatGamesPlayer data = repository.load(uuid);
                change.accept(data);
                repository.save(data.snapshot());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update ChatGames data for " + uuid, e);
            }
        });
    }

    /**
     * Deletes all of a player's data. If they are online they start over from zero.
     *
     * @return completes with true if the player had data
     */
    public static CompletableFuture<Boolean> delete(@Nonnull UUID uuid) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        io.execute(() -> {
            try {
                boolean existed = repository.delete(uuid);
                if (cache.containsKey(uuid)) {
                    cache.put(uuid, repository.load(uuid));
                }
                result.complete(existed);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not delete ChatGames data for " + uuid, e);
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private static void write(ChatGamesPlayer data, ChatGamesPlayer.Snapshot snapshot) {
        try {
            repository.save(snapshot);
        } catch (SQLException e) {
            data.restoreDirty(snapshot);
            plugin.getLogger().log(Level.SEVERE, "Could not save ChatGames data for " + data.getUuid(), e);
        }
    }
}
