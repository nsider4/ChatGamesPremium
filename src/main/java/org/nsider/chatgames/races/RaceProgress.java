package org.nsider.chatgames.races;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Everyone's count in one race, and how it ended.
 *
 * <p>On Folia each player's actions arrive on their own region thread, so counts are
 * added atomically and the race ends exactly once: either the first player to reach
 * the goal wins, or it is closed (time up, stopped) with no winner. Whatever comes
 * second is ignored.</p>
 *
 * <p>In a global race nobody wins outright: everyone who reaches the goal before it is
 * closed finishes, in order.</p>
 */
public final class RaceProgress {

    /** Stands for "ended with no winner". */
    private static final UUID NO_WINNER = new UUID(0, 0);

    private final int goal;
    private final Map<UUID, Integer> counts = new ConcurrentHashMap<>();
    private final AtomicReference<UUID> outcome = new AtomicReference<>();
    private final boolean global;
    private final Set<UUID> finished = ConcurrentHashMap.newKeySet();
    private final AtomicInteger finishers = new AtomicInteger();

    public RaceProgress(int goal) {
        this(goal, false);
    }

    public RaceProgress(int goal, boolean global) {
        this.goal = goal;
        this.global = global;
    }

    /**
     * Adds to a player's count.
     *
     * @return true if this made the player the winner (in a global race: made them
     *         finish); false otherwise, including once the race has ended
     */
    public boolean add(@Nonnull UUID player, int amount) {
        return addAndPlace(player, amount) > 0;
    }

    /**
     * Adds to a player's count.
     *
     * @return the place this made the player finish in (always 1 outside global races),
     *         or 0 if it didn't
     */
    public int addAndPlace(@Nonnull UUID player, int amount) {
        if (amount <= 0 || outcome.get() != null) return 0;
        int count = counts.merge(player, amount, Integer::sum);
        if (count < goal) return 0;
        if (!global) return outcome.compareAndSet(null, player) ? 1 : 0;
        return finished.add(player) ? finishers.incrementAndGet() : 0;
    }

    /** @return how many finished a global race */
    public int getFinishers() {
        return finishers.get();
    }

    /**
     * Ends the race with no winner.
     *
     * @return false if it had already ended
     */
    public boolean close() {
        return outcome.compareAndSet(null, NO_WINNER);
    }

    public boolean isOver() {
        return outcome.get() != null;
    }

    /** @return the winner, or null if there is none (yet) */
    @Nullable
    public UUID getWinner() {
        UUID uuid = outcome.get();
        return NO_WINNER.equals(uuid) ? null : uuid;
    }

    /** @return how many the player has so far */
    public int getCount(@Nonnull UUID player) {
        Integer count = counts.get(player);
        return count == null ? 0 : count;
    }

    public int getGoal() {
        return goal;
    }
}
