package org.nsider.chatgames.api;

import java.util.UUID;

/**
 * One place on a leaderboard.
 */
public final class TopEntry {

    private final UUID uuid;
    private final String name;
    private final double value;
    private final int rank;

    public TopEntry(UUID uuid, String name, double value, int rank) {
        this.uuid = uuid;
        this.name = name;
        this.value = value;
        this.rank = rank;
    }

    public UUID getUuid() {
        return uuid;
    }

    /** @return the player's last known name */
    public String getName() {
        return name;
    }

    /** @return the points, or the time in seconds on a fastest-times board */
    public double getValue() {
        return value;
    }

    /** @return 1 for the top place */
    public int getRank() {
        return rank;
    }
}
