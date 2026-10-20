package org.nsider.chatgames.model.player;

import org.nsider.chatgames.enums.GameType;

/**
 * Names of the per-player counters kept in the stats table.
 *
 * <p>Stats are stored as (player, name, value) rows, so a new statistic only needs
 * a new name here, with no database migration. Names are stored in the database:
 * never rename one that has shipped.</p>
 */
public final class Stat {

    /** Chat games won (any place), all games together. */
    public static final String WINS = "wins";
    /** Races won. */
    public static final String RACE_WINS = "race_wins";
    /** Global games taken part in. */
    public static final String GLOBAL_PLAYED = "global_played";
    /** Global games finished in a rewarded place. */
    public static final String GLOBAL_WON = "global_won";

    /** Daily and weekly challenges completed (claimed or not). */
    public static final String CHALLENGES_COMPLETED = "challenges_completed";

    /** Milestones reached but not yet paid out, as "milestone_pending.<points>" = 1. */
    public static final String MILESTONE_PENDING_PREFIX = "milestone_pending.";

    private Stat() {}

    public static String milestonePending(int points) {
        return MILESTONE_PENDING_PREFIX + points;
    }

    /**
     * @return the points of a {@link #milestonePending} stat name, or -1 if it isn't one
     */
    public static int milestoneOf(String statName) {
        try {
            return Integer.parseInt(statName.substring(MILESTONE_PENDING_PREFIX.length()));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /**
     * @return the counter for wins of one game or race, e.g. "wins.unscramble"
     */
    public static String wins(GameType type) {
        return "wins." + type.getConfigKey();
    }
}
