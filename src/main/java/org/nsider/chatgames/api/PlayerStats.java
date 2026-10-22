package org.nsider.chatgames.api;

import java.util.UUID;

/**
 * An online player's ChatGames numbers, as they are when read.
 */
public interface PlayerStats {

    UUID getUuid();

    int getPoints();

    /** @return chat games and races won, in any place */
    long getWins();

    long getWins(GameType game);

    /** @return the player's fastest win of that game in seconds, or 0 if they haven't won it */
    double getFastest(GameType game);

    /** @return whether the player gets chat games (they can turn them off) */
    boolean isPlaying();
}
