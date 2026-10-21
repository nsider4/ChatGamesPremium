package org.nsider.chatgames.races;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Everyone's count in one race, and how it ended.
 *
 * <p>On Folia each player's actions arrive on their own region thread, so counts are
 * added atomically and the race ends exactly once: either the first player to reach
 * the goal wins, or it is closed (time up, stopped) with no winner. Whatever comes
 * second is ignored.</p>
 */
public final class RaceProgress {

    /** Stands for "ended with no winner". */
    private static final UUID NO_WINNER = new UUID(0, 0);

    private final int goal;
    private final Map<UUID, Integer> counts = new ConcurrentHashMap<>();
    private final AtomicReference<UUID> outcome = new AtomicReference<>();

    public RaceProgress(int goal) {
        this.goal = goal;
    }

    /**
     * Adds to a player's count.
     *
     * @return true if this made the player the winner; false otherwise, including once
     *         the race has ended
     */
    public boolean add(@Nonnull UUID player, int amount) {
        if (amount <= 0 || outcome.get() != null) return false;
        int count = counts.merge(player, amount, Integer::sum);
        return count >= goal && outcome.compareAndSet(null, player);
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
