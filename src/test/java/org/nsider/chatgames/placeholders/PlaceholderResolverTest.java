package org.nsider.chatgames.placeholders;

import org.junit.jupiter.api.Test;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.Stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderResolverTest {

    private static final UUID ALEX = id(1);
    private static final UUID STEVE = id(2);
    private static final UUID NOTCH = id(3);
    private static final UUID JEB = id(4);
    private static final Map<UUID, String> NAMES = new HashMap<>();

    static {
        NAMES.put(ALEX, "Alex");
        NAMES.put(STEVE, "Steve");
        NAMES.put(NOTCH, "Notch");
        NAMES.put(JEB, "Jeb");
    }

    private static UUID id(int n) {
        return new UUID(0, n);
    }

    private static LeaderboardSnapshot board() {
        List<LeaderboardSnapshot.Row> points = Arrays.asList(
                new LeaderboardSnapshot.Row(ALEX, 10),
                new LeaderboardSnapshot.Row(STEVE, 25),
                new LeaderboardSnapshot.Row(NOTCH, 10),
                new LeaderboardSnapshot.Row(JEB, 3));
        List<LeaderboardSnapshot.TimeRow> times = Arrays.asList(
                new LeaderboardSnapshot.TimeRow(ALEX, GameType.UNSCRAMBLE, 2.5),
                new LeaderboardSnapshot.TimeRow(ALEX, GameType.GUESS_THE_NUMBER, 1.25),
                new LeaderboardSnapshot.TimeRow(STEVE, GameType.GUESS_THE_NUMBER, 0.9),
                new LeaderboardSnapshot.TimeRow(NOTCH, GameType.UNSCRAMBLE, 3.0));
        return LeaderboardSnapshot.build(points, times, NAMES::get);
    }

    private static ChatGamesPlayer alex() {
        ChatGamesPlayer data = new ChatGamesPlayer(ALEX);
        data.setPoints(10);
        data.setStat(Stat.WINS, 7);
        data.setStat(Stat.wins(GameType.GUESS_THE_NUMBER), 2);
        data.recordTime(GameType.UNSCRAMBLE, 2.5);
        data.recordTime(GameType.GUESS_THE_NUMBER, 1.25);
        data.setSoundEnabled(false);
        return data;
    }

    private static String resolve(String id, ChatGamesPlayer data, PlaceholderResolver.Status status) {
        return new PlaceholderResolver(board(), data, status, "-").resolve(id);
    }

    private static String resolve(String id) {
        return resolve(id, alex(), new PlaceholderResolver.Status(null, 42));
    }

    @Test
    void pointsLeaderboardSharesRanksOnTies() {
        assertEquals("Steve", resolve("top_1"));
        assertEquals("25", resolve("points_top_1"));
        assertEquals("10", resolve("points_top_2"));
        assertEquals("10", resolve("points_top_3"));
        assertEquals("Jeb", resolve("top_4"));
        assertEquals("-", resolve("top_5"), "an empty place shows the empty value");
        assertEquals("-", resolve("top_0"));
        // Alex and Notch both have 10: both are 2nd, and Jeb is 4th
        assertEquals("2", resolve("show_current_pos_top"));
        assertEquals("48", resolve("total_points"));
    }

    @Test
    void fastestOverallUsesEachPlayersBestTime() {
        assertEquals("Steve", resolve("fastest_1"));
        assertEquals("0.900", resolve("fastest_time_1"));
        assertEquals("Alex", resolve("fastest_2"));
        assertEquals("1.250", resolve("fastest_time_2"));
        assertEquals("Notch", resolve("fastest_3"));
        assertEquals("2", resolve("show_current_pos_fastest"));
        assertEquals("1.250", resolve("personal_best"));
    }

    @Test
    void perGameFastestHandlesGameNamesWithUnderscores() {
        assertEquals("Steve", resolve("fastest_guess_the_number_1"));
        assertEquals("0.900", resolve("fastest_guess_the_number_time_1"));
        assertEquals("Alex", resolve("fastest_guess_the_number_2"));
        assertEquals("1.250", resolve("fastest_guess_the_number"), "your own best for that game");
        assertEquals("Alex", resolve("fastest_unscramble_1"));
        assertEquals("-", resolve("fastest_math_1"), "nobody won math yet");
        assertEquals("-", resolve("fastest_math"));
        assertNull(resolve("fastest_banana"));
        assertNull(resolve("fastest_unscramble_x"));
    }

    @Test
    void personalCountersAndToggles() {
        assertEquals("10", resolve("points"));
        assertEquals("7", resolve("wins"));
        assertEquals("2", resolve("wins_guess_the_number"));
        assertEquals("0", resolve("wins_math"));
        assertEquals("0", resolve("global_played"));
        assertEquals("true", resolve("toggle"));
        assertEquals("false", resolve("sound_toggle"));
        assertEquals("10", resolve("POINTS"), "ids ignore case");
        assertNull(resolve("wins_banana"));
        assertNull(resolve("nonsense"));
    }

    @Test
    void withoutCachedDataCountersAreZeroAndTheRestEmpty() {
        PlaceholderResolver.Status idle = new PlaceholderResolver.Status(null, -1);
        assertEquals("0", resolve("points", null, idle));
        assertEquals("0", resolve("wins", null, idle));
        assertEquals("-", resolve("personal_best", null, idle));
        assertEquals("-", resolve("show_current_pos_top", null, idle));
        assertEquals("Steve", resolve("top_1", null, idle), "leaderboards don't need the viewer");
    }

    @Test
    void gameStatus() {
        assertEquals("false", resolve("game_running"));
        assertEquals("-", resolve("current_game"));
        assertEquals("42", resolve("next_game_in"));

        PlaceholderResolver.Status running = new PlaceholderResolver.Status(GameType.MEMORIZE, 42);
        assertEquals("true", resolve("game_running", alex(), running));
        assertEquals("memorize", resolve("current_game", alex(), running));
        assertEquals("-", resolve("next_game_in", alex(), running));
        assertEquals("-", resolve("next_game_in", alex(), new PlaceholderResolver.Status(null, -1)), "rotation off");
    }

    @Test
    void racePlaceholders() {
        assertEquals("-", resolve("race_target"));
        assertEquals("-", resolve("race_progress"));
        PlaceholderResolver.Status race = new PlaceholderResolver.Status(GameType.MINE, -1, "Diamond Ore", 5, 2);
        assertEquals("Diamond Ore", resolve("race_target", alex(), race));
        assertEquals("5", resolve("race_amount", alex(), race));
        assertEquals("2", resolve("race_progress", alex(), race));
        assertEquals("mine", resolve("current_game", alex(), race));
    }

    @Test
    void globalGamePlaceholders() {
        assertEquals("false", resolve("global_running"));
        assertEquals("-", resolve("global_progress"));
        assertEquals("-", resolve("global_contribution"));
        PlaceholderResolver.Status global = new PlaceholderResolver.Status(GameType.COUNT, -1).withGlobal(3, 12, 2, 40);
        assertEquals("true", resolve("global_running", alex(), global));
        assertEquals("3", resolve("global_progress", alex(), global));
        assertEquals("12", resolve("global_goal", alex(), global));
        assertEquals("25", resolve("global_percent", alex(), global));
        assertEquals("2", resolve("global_contribution", alex(), global));
        assertEquals("40", resolve("global_time_left", alex(), global));

        ChatGamesPlayer data = alex();
        data.setStat(Stat.CONTRIBUTIONS, 17);
        assertEquals("17", resolve("contributions", data, global));
    }

    @Test
    void raceTimesOnlyCountInTheirOwnList() {
        List<LeaderboardSnapshot.TimeRow> times = Arrays.asList(
                new LeaderboardSnapshot.TimeRow(ALEX, GameType.CRAFT, 0.5),
                new LeaderboardSnapshot.TimeRow(ALEX, GameType.MATH, 3.0),
                new LeaderboardSnapshot.TimeRow(STEVE, GameType.MATH, 2.0));
        LeaderboardSnapshot board = LeaderboardSnapshot.build(Collections.<LeaderboardSnapshot.Row>emptyList(), times, NAMES::get);
        assertEquals("Steve", board.fastest(1).getName(), "Alex's 0.5s craft race isn't a typing time");
        assertEquals(3.0, board.fastest(2).getValue());
        assertEquals("Alex", board.fastest(GameType.CRAFT, 1).getName());

        ChatGamesPlayer data = new ChatGamesPlayer(ALEX);
        data.recordTime(GameType.CRAFT, 0.5);
        data.recordTime(GameType.MATH, 3.0);
        assertEquals(3.0, data.getBestOverall());
        assertEquals(0.5, data.getFastest(GameType.CRAFT));
    }

    @Test
    void onlyTheTopOfEachListGetsANameLookup() {
        List<LeaderboardSnapshot.Row> points = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) points.add(new LeaderboardSnapshot.Row(id(i), i));
        int[] lookups = {0};
        LeaderboardSnapshot board = LeaderboardSnapshot.build(points, Collections.<LeaderboardSnapshot.TimeRow>emptyList(),
                uuid -> {
                    lookups[0]++;
                    return "p" + uuid.getLeastSignificantBits();
                });
        assertEquals(LeaderboardSnapshot.NAMED_ENTRIES, lookups[0]);
        assertEquals("p1000", board.points(1).getName());
        assertEquals(1000, board.pointsRank(id(1)), "players outside the named top still have a rank");
        assertTrue(board.points(LeaderboardSnapshot.NAMED_ENTRIES + 1) == null);
    }
}
