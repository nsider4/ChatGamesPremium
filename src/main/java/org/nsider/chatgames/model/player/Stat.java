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

    private Stat() {}

    /**
     * @return the counter for wins of one game or race, e.g. "wins.unscramble"
     */
    public static String wins(GameType type) {
        return "wins." + type.getConfigKey();
    }
}
