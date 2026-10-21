package org.nsider.chatgames.progression;

import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.inventorymanager.extras.Nonnull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Challenge progress, kept in a player's stats:
 * <ul>
 *   <li>{@code challenge.<period>.period}: the period the values below belong to</li>
 *   <li>{@code challenge.<period>.<id>}: progress, never above the challenge's amount</li>
 *   <li>{@code challenge.<period>.<id>.claimed}: 1 once the reward was claimed</li>
 * </ul>
 * When a new period starts they are set back to 0 rather than deleted, so a player has
 * at most one row per challenge id. Each call holds the player's lock throughout, so
 * progress from several threads at once is never lost and a reward is only claimed once.
 * Holds no state of its own.
 */
public final class ChallengeTracker {

    private ChallengeTracker() {}

    /**
     * Adds an activity to every active challenge it counts for.
     *
     * @return the challenges this completed, in daily-then-weekly order
     */
    @Nonnull
    public static Result record(@Nonnull ChatGamesPlayer player, @Nonnull ChallengeConfig config,
                                @Nonnull Activity activity, @Nonnull LocalDate today) {
        List<Challenge> completed = new ArrayList<>();
        boolean allDaily = false;
        synchronized (player) {
            for (Period period : Period.values()) {
                List<Challenge> active = config.active(period, today);
                if (active.isEmpty()) continue;
                rollover(player, config, period, today);
                boolean completedHere = false;
                for (Challenge challenge : active) {
                    int add = challenge.progressFor(activity);
                    if (add <= 0) continue;
                    String key = progressKey(challenge);
                    long before = player.getStat(key);
                    if (before >= challenge.getAmount()) continue;
                    long after = Math.min(challenge.getAmount(), before + add);
                    player.setStat(key, after);
                    if (after >= challenge.getAmount()) {
                        completed.add(challenge);
                        player.incrementStat(Stat.CHALLENGES_COMPLETED, 1);
                        completedHere = true;
                    }
                }
                if (period == Period.DAILY && completedHere) {
                    allDaily = countCompleted(player, active) == active.size();
                }
            }
        }
        return new Result(completed, allDaily);
    }

    /**
     * @return the challenge's progress for the current period, 0 to its amount
     */
    public static long progress(@Nonnull ChatGamesPlayer player, @Nonnull ChallengeConfig config,
                                @Nonnull Challenge challenge, @Nonnull LocalDate today) {
        synchronized (player) {
            if (!isCurrent(player, config, challenge.getPeriod(), today)) return 0;
            return Math.min(challenge.getAmount(), player.getStat(progressKey(challenge)));
        }
    }

    public static boolean isClaimed(@Nonnull ChatGamesPlayer player, @Nonnull ChallengeConfig config,
                                    @Nonnull Challenge challenge, @Nonnull LocalDate today) {
        synchronized (player) {
            return isCurrent(player, config, challenge.getPeriod(), today) && player.getStat(claimedKey(challenge)) > 0;
        }
    }

    /**
     * @return how many of the period's active challenges are complete
     */
    public static int completed(@Nonnull ChatGamesPlayer player, @Nonnull ChallengeConfig config,
                                @Nonnull Period period, @Nonnull LocalDate today) {
        synchronized (player) {
            if (!isCurrent(player, config, period, today)) return 0;
            return countCompleted(player, config.active(period, today));
        }
    }

    /**
     * Marks every complete, unclaimed challenge as claimed.
     *
     * @return the challenges claimed now, whose rewards are to be given
     */
    @Nonnull
    public static List<Challenge> claimAll(@Nonnull ChatGamesPlayer player, @Nonnull ChallengeConfig config,
                                           @Nonnull LocalDate today) {
        List<Challenge> claimed = new ArrayList<>();
        synchronized (player) {
            for (Period period : Period.values()) {
                List<Challenge> active = config.active(period, today);
                if (active.isEmpty()) continue;
                rollover(player, config, period, today);
                for (Challenge challenge : active) {
                    if (player.getStat(progressKey(challenge)) >= challenge.getAmount()
                            && player.getStat(claimedKey(challenge)) == 0) {
                        player.setStat(claimedKey(challenge), 1);
                        claimed.add(challenge);
                    }
                }
            }
        }
        return claimed;
    }

    /**
     * Marks one challenge as claimed if it is complete and not yet claimed.
     *
     * @return true if it was claimed now, so its reward is to be given
     */
    public static boolean claim(@Nonnull ChatGamesPlayer player, @Nonnull ChallengeConfig config,
                                @Nonnull Challenge challenge, @Nonnull LocalDate today) {
        synchronized (player) {
            // By id: a menu opened before a reload holds the old Challenge objects
            boolean active = false;
            for (Challenge current : config.active(challenge.getPeriod(), today)) {
                if (current.getId().equals(challenge.getId())) active = true;
            }
            if (!active) return false;
            rollover(player, config, challenge.getPeriod(), today);
            if (player.getStat(progressKey(challenge)) < challenge.getAmount()
                    || player.getStat(claimedKey(challenge)) != 0) return false;
            player.setStat(claimedKey(challenge), 1);
            return true;
        }
    }

    /**
     * Clears the player's progress and claims for the current daily and weekly challenges.
     */
    public static void reset(@Nonnull ChatGamesPlayer player) {
        synchronized (player) {
            for (Period period : Period.values()) {
                clear(player, period);
                player.setStat(periodKey(period), 0);
            }
        }
    }

    private static int countCompleted(ChatGamesPlayer player, List<Challenge> active) {
        int done = 0;
        for (Challenge challenge : active) {
            if (player.getStat(progressKey(challenge)) >= challenge.getAmount()) done++;
        }
        return done;
    }

    private static boolean isCurrent(ChatGamesPlayer player, ChallengeConfig config, Period period, LocalDate today) {
        return player.getStat(periodKey(period)) == config.periodId(period, today);
    }

    /** Starts the new period if the stored values belong to an older one. */
    private static void rollover(ChatGamesPlayer player, ChallengeConfig config, Period period, LocalDate today) {
        long id = config.periodId(period, today);
        if (player.getStat(periodKey(period)) == id) return;
        clear(player, period);
        player.setStat(periodKey(period), id);
    }

    private static void clear(ChatGamesPlayer player, Period period) {
        String current = periodKey(period);
        for (String name : player.getStatNames(prefix(period))) {
            if (!name.equals(current) && player.getStat(name) != 0) player.setStat(name, 0);
        }
    }

    private static String prefix(Period period) {
        return "challenge." + period.getKey() + ".";
    }

    static String periodKey(Period period) {
        return prefix(period) + "period";
    }

    static String progressKey(Challenge challenge) {
        return prefix(challenge.getPeriod()) + challenge.getId();
    }

    static String claimedKey(Challenge challenge) {
        return progressKey(challenge) + ".claimed";
    }

    /** What {@link #record} changed. */
    public static final class Result {
        private final List<Challenge> completed;
        private final boolean allDailyCompleted;

        Result(List<Challenge> completed, boolean allDailyCompleted) {
            this.completed = completed;
            this.allDailyCompleted = allDailyCompleted;
        }

        /** @return the challenges this activity completed */
        @Nonnull
        public List<Challenge> getCompleted() {
            return completed;
        }

        /** @return true if this activity completed the last of today's daily challenges */
        public boolean isAllDailyCompleted() {
            return allDailyCompleted;
        }
    }
}
