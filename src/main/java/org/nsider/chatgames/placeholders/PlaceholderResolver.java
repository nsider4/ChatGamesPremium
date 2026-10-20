package org.nsider.chatgames.placeholders;

import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Locale;

/**
 * Works out a {@code %chatgames_<id>%} value from data already in memory: the player's
 * cached data and the latest {@link LeaderboardSnapshot}. It never touches the database
 * or the server, so any thread may call it (scoreboard plugins ask from their own).
 *
 * <p>Per-player values need the player's data in the cache (online, or loading a
 * moment after joining); without it counters show 0 and other values show the
 * configured empty value.</p>
 */
public final class PlaceholderResolver {

    /** What is happening right now, for the game placeholders. */
    public static final class Status {
        @Nullable final GameType currentGame;
        /** Seconds until the rotation's next game, or -1 if none is scheduled. */
        final long nextGameSeconds;

        public Status(@Nullable GameType currentGame, long nextGameSeconds) {
            this.currentGame = currentGame;
            this.nextGameSeconds = nextGameSeconds;
        }
    }

    private final LeaderboardSnapshot board;
    @Nullable private final ChatGamesPlayer data;
    private final Status status;
    private final String empty;

    public PlaceholderResolver(@Nonnull LeaderboardSnapshot board, @Nullable ChatGamesPlayer data,
                               @Nonnull Status status, @Nonnull String empty) {
        this.board = board;
        this.data = data;
        this.status = status;
        this.empty = empty;
    }

    /**
     * @param id the part after {@code chatgames_}, any case
     * @return the value, or null if no such placeholder exists
     */
    @Nullable
    public String resolve(@Nonnull String id) {
        String key = id.toLowerCase(Locale.ROOT);
        switch (key) {
            case "points": return data == null ? "0" : String.valueOf(data.getPoints());
            case "toggle": return String.valueOf(data == null || data.isToggled());
            case "sound_toggle":
            case "togglesound": return String.valueOf(data == null || data.isSoundEnabled());
            case "wins": return stat(Stat.WINS);
            case "race_wins": return stat(Stat.RACE_WINS);
            case "global_played": return stat(Stat.GLOBAL_PLAYED);
            case "global_won": return stat(Stat.GLOBAL_WON);
            case "personal_best": return data == null ? empty : time(data.getBestOverall());
            case "show_current_pos_top": return rank(data == null ? 0 : board.pointsRank(data.getUuid()));
            case "show_current_pos_fastest": return rank(data == null ? 0 : board.fastestRank(data.getUuid()));
            case "total_points": return String.valueOf(board.getTotalPoints());
            case "game_running": return String.valueOf(status.currentGame != null);
            case "current_game": return status.currentGame == null ? empty : status.currentGame.getConfigKey();
            case "next_game_in":
                return status.currentGame != null || status.nextGameSeconds < 0 ? empty : String.valueOf(status.nextGameSeconds);
            default:
                break;
        }

        if (key.startsWith("wins_")) {
            GameType game = GameType.fromName(key.substring("wins_".length()));
            return game == null ? null : stat(Stat.wins(game));
        }
        if (key.startsWith("top_")) {
            LeaderboardSnapshot.Entry entry = board.points(number(key.substring("top_".length())));
            return entry == null ? empty : entry.getName();
        }
        if (key.startsWith("points_top_")) {
            LeaderboardSnapshot.Entry entry = board.points(number(key.substring("points_top_".length())));
            return entry == null ? empty : String.valueOf((long) entry.getValue());
        }
        if (key.startsWith("fastest_")) {
            return fastest(key.substring("fastest_".length()));
        }
        return null;
    }

    /**
     * {@code fastest_<n>}, {@code fastest_time_<n>} (overall), {@code fastest_<game>} (your best),
     * {@code fastest_<game>_<n>} and {@code fastest_<game>_time_<n>}.
     */
    @Nullable
    private String fastest(String rest) {
        if (isNumber(rest)) return name(board.fastest(number(rest)));
        if (rest.startsWith("time_") && isNumber(rest.substring(5))) return value(board.fastest(number(rest.substring(5))));

        GameType game = gamePrefix(rest);
        if (game == null) return null;
        String suffix = rest.substring(game.getConfigKey().length());
        if (suffix.isEmpty()) {
            return data == null ? empty : time(data.getFastest(game));
        }
        suffix = suffix.substring(1);
        if (isNumber(suffix)) return name(board.fastest(game, number(suffix)));
        if (suffix.startsWith("time_") && isNumber(suffix.substring(5))) {
            return value(board.fastest(game, number(suffix.substring(5))));
        }
        return null;
    }

    /** The game whose key {@code text} starts with, followed by its end or "_" (keys may contain "_"). */
    @Nullable
    private static GameType gamePrefix(String text) {
        GameType match = null;
        for (GameType type : GameType.values()) {
            String key = type.getConfigKey();
            if ((text.equals(key) || text.startsWith(key + "_"))
                    && (match == null || key.length() > match.getConfigKey().length())) {
                match = type;
            }
        }
        return match;
    }

    private String stat(String name) {
        return String.valueOf(data == null ? 0 : data.getStat(name));
    }

    private String rank(int rank) {
        return rank <= 0 ? empty : String.valueOf(rank);
    }

    private String name(@Nullable LeaderboardSnapshot.Entry entry) {
        return entry == null ? empty : entry.getName();
    }

    private String value(@Nullable LeaderboardSnapshot.Entry entry) {
        return entry == null ? empty : time(entry.getValue());
    }

    private String time(double seconds) {
        return seconds <= 0 ? empty : String.format(Locale.ROOT, "%.3f", seconds);
    }

    private static boolean isNumber(String text) {
        if (text.isEmpty() || text.length() > 6) return false;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) return false;
        }
        return true;
    }

    /** @return the number, or 0 (never a valid place) if it isn't one */
    private static int number(String text) {
        return isNumber(text) ? Integer.parseInt(text) : 0;
    }
}
