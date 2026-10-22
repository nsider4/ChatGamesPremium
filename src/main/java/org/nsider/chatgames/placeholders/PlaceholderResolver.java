package org.nsider.chatgames.placeholders;

import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.progression.ChallengeConfig;
import org.nsider.chatgames.progression.ChallengeTracker;
import org.nsider.chatgames.progression.Period;
import org.nsider.chatgames.progression.StreakTracker;
import org.nsider.chatgames.progression.StreakType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.LocalDate;
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
        /** The running race's target as shown in messages, or null if no race runs. */
        @Nullable final String raceTarget;
        final int raceAmount;
        /** The viewer's count in the running race. */
        final int raceProgress;
        /** The running game event's name, or null if none runs. */
        @Nullable final String eventName;
        final int eventStep;
        final int eventSteps;
        /** The viewer's event points and place (0 if they haven't scored). */
        final int eventPoints;
        final int eventPlace;
        /** The global game in play (progress, goal, the viewer's contribution, seconds left), or null. */
        @Nullable final int[] global;

        public Status(@Nullable GameType currentGame, long nextGameSeconds) {
            this(currentGame, nextGameSeconds, null, 0, 0);
        }

        public Status(@Nullable GameType currentGame, long nextGameSeconds, @Nullable String raceTarget,
                      int raceAmount, int raceProgress) {
            this(currentGame, nextGameSeconds, raceTarget, raceAmount, raceProgress, null, 0, 0, 0, 0, null);
        }

        private Status(@Nullable GameType currentGame, long nextGameSeconds, @Nullable String raceTarget,
                       int raceAmount, int raceProgress, @Nullable String eventName, int eventStep, int eventSteps,
                       int eventPoints, int eventPlace, @Nullable int[] global) {
            this.currentGame = currentGame;
            this.nextGameSeconds = nextGameSeconds;
            this.raceTarget = raceTarget;
            this.raceAmount = raceAmount;
            this.raceProgress = raceProgress;
            this.eventName = eventName;
            this.eventStep = eventStep;
            this.eventSteps = eventSteps;
            this.eventPoints = eventPoints;
            this.eventPlace = eventPlace;
            this.global = global;
        }

        /**
         * @param name   the running event's name
         * @param step   the game playing or next, from 1
         * @param points the viewer's event points
         * @param place  the viewer's place, 0 if they haven't scored
         * @return this status with a game event running
         */
        @Nonnull
        public Status withEvent(@Nonnull String name, int step, int steps, int points, int place) {
            return new Status(currentGame, nextGameSeconds, raceTarget, raceAmount, raceProgress, name, step, steps, points,
                    place, global);
        }

        /**
         * @param contribution the viewer's contribution so far
         * @return this status with a global game in play
         */
        @Nonnull
        public Status withGlobal(int progress, int goal, int contribution, int secondsLeft) {
            return new Status(currentGame, nextGameSeconds, raceTarget, raceAmount, raceProgress, eventName, eventStep,
                    eventSteps, eventPoints, eventPlace, new int[]{progress, goal, contribution, secondsLeft});
        }
    }

    private final LeaderboardSnapshot board;
    @Nullable private final ChatGamesPlayer data;
    private final Status status;
    private final String empty;
    private final ChallengeConfig challenges;
    private final LocalDate today;

    public PlaceholderResolver(@Nonnull LeaderboardSnapshot board, @Nullable ChatGamesPlayer data,
                               @Nonnull Status status, @Nonnull String empty) {
        this(board, data, status, empty, ChallengeConfig.EMPTY, LocalDate.now());
    }

    /**
     * @param challenges for the daily and weekly placeholders
     * @param today      in the challenges' time zone, for those and the streaks
     */
    public PlaceholderResolver(@Nonnull LeaderboardSnapshot board, @Nullable ChatGamesPlayer data,
                               @Nonnull Status status, @Nonnull String empty,
                               @Nonnull ChallengeConfig challenges, @Nonnull LocalDate today) {
        this.board = board;
        this.data = data;
        this.status = status;
        this.empty = empty;
        this.challenges = challenges;
        this.today = today;
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
            case "contributions": return stat(Stat.CONTRIBUTIONS);
            case "global_running": return String.valueOf(status.global != null);
            case "global_progress": return status.global == null ? empty : String.valueOf(status.global[0]);
            case "global_goal": return status.global == null ? empty : String.valueOf(status.global[1]);
            case "global_percent": return status.global == null ? empty : String.valueOf(status.global[0] * 100L / Math.max(1, status.global[1]));
            case "global_contribution": return status.global == null ? empty : String.valueOf(status.global[2]);
            case "global_time_left": return status.global == null ? empty : String.valueOf(status.global[3]);
            case "events_played": return stat(Stat.EVENTS_PLAYED);
            case "event_wins": return stat(Stat.EVENT_WINS);
            case "event_best_place": return stat(Stat.EVENT_BEST_PLACE);
            case "personal_best": return data == null ? empty : time(data.getBestOverall());
            case "show_current_pos_top": return rank(data == null ? 0 : board.pointsRank(data.getUuid()));
            case "show_current_pos_fastest": return rank(data == null ? 0 : board.fastestRank(data.getUuid()));
            case "total_points": return String.valueOf(board.getTotalPoints());
            case "game_running": return String.valueOf(status.currentGame != null);
            case "current_game": return status.currentGame == null ? empty : status.currentGame.getConfigKey();
            case "next_game_in":
                return status.currentGame != null || status.nextGameSeconds < 0 ? empty : String.valueOf(status.nextGameSeconds);
            case "race_target": return status.raceTarget == null ? empty : status.raceTarget;
            case "race_amount": return status.raceTarget == null ? empty : String.valueOf(status.raceAmount);
            case "race_progress": return status.raceTarget == null ? empty : String.valueOf(status.raceProgress);
            case "event_running": return String.valueOf(status.eventName != null);
            case "event_name": return status.eventName == null ? empty : status.eventName;
            case "event_step": return status.eventName == null ? empty : String.valueOf(status.eventStep);
            case "event_steps": return status.eventName == null ? empty : String.valueOf(status.eventSteps);
            case "event_points": return status.eventName == null ? empty : String.valueOf(status.eventPoints);
            case "event_place": return status.eventName == null ? empty : rank(status.eventPlace);
            case "challenges_completed": return stat(Stat.CHALLENGES_COMPLETED);
            case "daily_completed": return completed(Period.DAILY);
            case "daily_total": return String.valueOf(challenges.active(Period.DAILY, today).size());
            case "weekly_completed": return completed(Period.WEEKLY);
            case "weekly_total": return String.valueOf(challenges.active(Period.WEEKLY, today).size());
            default:
                break;
        }

        if (key.startsWith("streak_")) {
            String rest = key.substring("streak_".length());
            boolean longest = rest.endsWith("_longest");
            StreakType type = StreakType.fromName(longest ? rest.substring(0, rest.length() - "_longest".length()) : rest);
            if (type == null) return null;
            if (data == null) return "0";
            return String.valueOf(longest ? StreakTracker.longest(data, type) : StreakTracker.current(data, type, today));
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

    private String completed(Period period) {
        return data == null ? "0" : String.valueOf(ChallengeTracker.completed(data, challenges, period, today));
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
