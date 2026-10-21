package org.nsider.chatgames.progression;

import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * Something a player did that challenges and streaks count: a win, a global game
 * or taking part in an event.
 */
public final class Activity {

    enum Kind { WIN, GLOBAL, EVENT }

    private final Kind kind;
    @Nullable private final GameType game;
    private final double seconds;
    private final int points;
    private final boolean placed;

    private Activity(Kind kind, @Nullable GameType game, double seconds, int points, boolean placed) {
        this.kind = kind;
        this.game = game;
        this.seconds = seconds;
        this.points = points;
        this.placed = placed;
    }

    /**
     * A chat game won in any place, or a race won.
     *
     * @param points the points the win paid
     */
    @Nonnull
    public static Activity win(@Nonnull GameType game, double seconds, int points) {
        return new Activity(Kind.WIN, game, seconds, points, true);
    }

    /**
     * Taking part in a global game.
     *
     * @param points    the points it paid
     * @param completed true if the server reached the goal and this player contributed
     *                  enough to be rewarded, which counts as winning it
     */
    @Nonnull
    public static Activity global(@Nonnull GameType game, int points, boolean completed) {
        // Nobody answers "in N seconds" in a global game, so it never counts for FAST_WIN
        return new Activity(Kind.GLOBAL, game, Double.MAX_VALUE, points, completed);
    }

    /** Taking part in a game event. */
    @Nonnull
    public static Activity event() {
        return new Activity(Kind.EVENT, null, 0, 0, false);
    }

    Kind getKind() {
        return kind;
    }

    @Nullable
    GameType getGame() {
        return game;
    }

    double getSeconds() {
        return seconds;
    }

    int getPoints() {
        return points;
    }

    /** @return true for a win, or a completed global game */
    boolean isWin() {
        return kind == Kind.WIN || (kind == Kind.GLOBAL && placed);
    }
}
