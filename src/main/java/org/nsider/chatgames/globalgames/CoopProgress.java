package org.nsider.chatgames.globalgames;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The server's shared score in one global game, and what each player put into it.
 *
 * <p>Answers arrive on async chat threads and race actions on region threads (Folia),
 * so every method is synchronized. The game ends exactly once: the contribution that
 * reaches the goal completes it, or it is closed (time up, stopped, a broken count);
 * whatever comes second is ignored.</p>
 */
public final class CoopProgress {

    public enum Outcome { COMPLETED, FAILED, STOPPED }

    private final int goal;
    private int progress;
    /** By when each player first contributed, which breaks ties in the ranking. */
    private final Map<UUID, Integer> contributions = new LinkedHashMap<>();
    @Nullable private Outcome outcome;

    public CoopProgress(int goal) {
        this.goal = Math.max(1, goal);
    }

    /**
     * Adds a player's contribution to the shared score.
     *
     * @return what changed, or null if the game is over (nothing was added)
     */
    @Nullable
    public synchronized Step add(@Nonnull UUID player, int amount) {
        if (amount <= 0 || outcome != null) return null;
        int before = progress;
        progress = Math.min(goal, progress + amount);
        Integer had = contributions.get(player);
        int mine = (had == null ? 0 : had) + amount;
        contributions.put(player, mine);
        boolean completed = progress >= goal;
        if (completed) outcome = Outcome.COMPLETED;
        return new Step(before, progress, mine, completed);
    }

    /**
     * Sets the shared score back to 0 (a broken count); contributions are kept.
     *
     * @return false if the game is over
     */
    public synchronized boolean resetProgress() {
        if (outcome != null) return false;
        progress = 0;
        return true;
    }

    /**
     * Ends the game without reaching the goal.
     *
     * @return false if it had already ended
     */
    public synchronized boolean close(@Nonnull Outcome how) {
        if (outcome != null) return false;
        outcome = how;
        return true;
    }

    @Nullable
    public synchronized Outcome getOutcome() {
        return outcome;
    }

    public synchronized boolean isOver() {
        return outcome != null;
    }

    public synchronized int getProgress() {
        return progress;
    }

    public int getGoal() {
        return goal;
    }

    /** @return the progress as a percentage, 0 to 100 */
    public synchronized int getPercent() {
        return (int) (progress * 100L / goal);
    }

    public synchronized int contribution(@Nonnull UUID player) {
        Integer amount = contributions.get(player);
        return amount == null ? 0 : amount;
    }

    public synchronized int contributors() {
        return contributions.size();
    }

    /** @return everyone who contributed, most first; ties keep whoever contributed first ahead */
    @Nonnull
    public synchronized List<Contributor> ranking() {
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(contributions.entrySet());
        // A stable sort, so ties stay in the order players first contributed
        Collections.sort(entries, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<Contributor> ranking = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ranking.add(new Contributor(entries.get(i).getKey(), entries.get(i).getValue(), i + 1));
        }
        return ranking;
    }

    /**
     * @param percents the percentages that are announced, e.g. 25, 50 and 75
     * @return the ones that going from {@code before} to {@code after} crossed, lowest first
     */
    @Nonnull
    public static List<Integer> crossed(int before, int after, int goal, @Nonnull List<Integer> percents) {
        List<Integer> crossed = new ArrayList<>();
        for (int percent : percents) {
            if (percent <= 0 || percent >= 100) continue;
            // The progress at which this percentage is reached, rounded up
            long mark = (goal * (long) percent + 99) / 100;
            if (before < mark && after >= mark) crossed.add(percent);
        }
        Collections.sort(crossed);
        return crossed;
    }

    /** What one contribution changed. */
    public static final class Step {
        private final int before;
        private final int after;
        private final int contribution;
        private final boolean completed;

        Step(int before, int after, int contribution, boolean completed) {
            this.before = before;
            this.after = after;
            this.contribution = contribution;
            this.completed = completed;
        }

        public int getBefore() {
            return before;
        }

        public int getAfter() {
            return after;
        }

        /** @return the player's total contribution so far */
        public int getContribution() {
            return contribution;
        }

        /** @return true if this contribution reached the goal */
        public boolean isCompleted() {
            return completed;
        }
    }

    /** One player's contribution and place in the ranking. */
    public static final class Contributor {
        private final UUID uuid;
        private final int amount;
        private final int place;

        Contributor(UUID uuid, int amount, int place) {
            this.uuid = uuid;
            this.amount = amount;
            this.place = place;
        }

        @Nonnull
        public UUID getUuid() {
            return uuid;
        }

        public int getAmount() {
            return amount;
        }

        public int getPlace() {
            return place;
        }
    }
}
