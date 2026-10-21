package org.nsider.chatgames.progression;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.Stat;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.chatgames.placeholders.PlaceholderResolver;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionTest {

    /** A Wednesday. */
    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);

    private static final String FIXED = "daily:\n  mode: fixed\n  challenges:\n"
            + "    win_2:\n      type: WIN_GAMES\n      amount: 2\n      name: '&eWin %amount%'\n      rewards: ['[playermsg] %challenge%']\n"
            + "    fast:\n      type: FAST_WIN\n      seconds: 3\n      amount: 1\n"
            + "    points:\n      type: EARN_POINTS\n      amount: 5\n"
            + "weekly:\n  reset-day: friday\n  challenges:\n"
            + "    races:\n      type: WIN_RACES\n      amount: 2\n"
            + "    math:\n      type: WIN_GAME\n      game: math\n      amount: 1\n";

    private static ChallengeConfig config(String yaml, List<String> warnings) throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return ChallengeConfig.load(file, warnings::add);
    }

    private static ChallengeConfig config(String yaml) throws Exception {
        List<String> warnings = new ArrayList<>();
        ChallengeConfig config = config(yaml, warnings);
        assertTrue(warnings.isEmpty(), "unexpected warnings: " + warnings);
        return config;
    }

    private static YamlConfiguration resource(String name) throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(ProgressionTest.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8)) {
            file.load(reader);
        }
        return file;
    }

    private static List<String> ids(List<Challenge> challenges) {
        List<String> ids = new ArrayList<>();
        for (Challenge challenge : challenges) ids.add(challenge.getId());
        return ids;
    }

    /* ------------------------------------------ periods and config ------------------------------ */

    @Test
    void weeksStartOnTheResetDay() {
        long friday = LocalDate.of(2026, 10, 16).toEpochDay();
        assertEquals(friday, Period.WEEKLY.id(DAY, DayOfWeek.FRIDAY));
        assertEquals(friday, Period.WEEKLY.id(LocalDate.of(2026, 10, 16), DayOfWeek.FRIDAY), "the reset day itself");
        assertEquals(friday + 7, Period.WEEKLY.id(LocalDate.of(2026, 10, 23), DayOfWeek.FRIDAY));
        assertEquals(DAY.toEpochDay(), Period.DAILY.id(DAY, DayOfWeek.FRIDAY));
    }

    @Test
    void brokenChallengesAreSkippedWithAWarning() throws Exception {
        List<String> warnings = new ArrayList<>();
        ChallengeConfig config = config("timezone: Mars/Base\ndaily:\n  challenges:\n"
                + "    ok:\n      type: WIN_GAMES\n"
                + "    Bad Id:\n      type: WIN_GAMES\n"
                + "    no_type:\n      type: DANCE\n"
                + "    no_game:\n      type: WIN_GAME\n      game: chess\n"
                + "    no_seconds:\n      type: FAST_WIN\n"
                + "    zero:\n      type: WIN_GAMES\n      amount: 0\n", warnings);
        assertEquals(Collections.singletonList("ok"), ids(config.active(Period.DAILY, DAY)));
        assertEquals(6, warnings.size(), "timezone and five challenges: " + warnings);
        assertFalse(config.isEnabled(Period.WEEKLY));
    }

    @Test
    void randomModeDrawsTheSameChallengesForEveryoneButChangesByDay() throws Exception {
        StringBuilder yaml = new StringBuilder("daily:\n  mode: random\n  amount: 3\n  challenges:\n");
        for (int i = 0; i < 10; i++) yaml.append("    c").append(i).append(":\n      type: WIN_GAMES\n");
        ChallengeConfig config = config(yaml.toString());

        assertEquals(ids(config.active(Period.DAILY, DAY)), ids(config.active(Period.DAILY, DAY)));
        Set<List<String>> seen = new HashSet<>();
        for (int day = 0; day < 30; day++) {
            List<String> active = ids(config.active(Period.DAILY, DAY.plusDays(day)));
            assertEquals(3, active.size());
            List<String> sorted = new ArrayList<>(active);
            Collections.sort(sorted);
            assertEquals(sorted, active, "kept in file order");
            seen.add(active);
        }
        assertTrue(seen.size() > 20, "neighbouring days draw different sets, got " + seen.size() + " of 30");
    }

    @Test
    void theDefaultFilesLoadCleanly() throws Exception {
        List<String> warnings = new ArrayList<>();
        ChallengeConfig challenges = ChallengeConfig.load(resource("challenges.yml"), warnings::add);
        StreakConfig streaks = StreakConfig.load(resource("streaks.yml"), warnings::add);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(3, challenges.active(Period.DAILY, DAY).size());
        assertEquals(3, challenges.active(Period.WEEKLY, DAY).size());
        for (StreakType type : StreakType.values()) {
            assertTrue(streaks.isEnabled(type));
            for (int day = 1; day <= 7; day++) assertFalse(streaks.getRewards(type, day).isEmpty(), type + " day " + day);
        }
    }

    /* ------------------------------------------ challenges -------------------------------------- */

    @Test
    void activitiesCountTowardsTheMatchingChallenges() throws Exception {
        ChallengeConfig config = config(FIXED);
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        List<Challenge> daily = config.active(Period.DAILY, DAY);
        List<Challenge> weekly = config.active(Period.WEEKLY, DAY);

        ChallengeTracker.Result first = ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 4.0, 2), DAY);
        assertEquals(Collections.singletonList("math"), ids(first.getCompleted()));
        assertEquals(1, ChallengeTracker.progress(player, config, daily.get(0), DAY));
        assertEquals(0, ChallengeTracker.progress(player, config, daily.get(1), DAY), "4 seconds isn't fast enough");
        assertEquals(2, ChallengeTracker.progress(player, config, daily.get(2), DAY));

        ChallengeTracker.Result race = ChallengeTracker.record(player, config, Activity.win(GameType.MINE, 1.0, 1), DAY);
        assertEquals(Collections.singletonList("win_2"), ids(race.getCompleted()));
        assertEquals(0, ChallengeTracker.progress(player, config, daily.get(1), DAY), "races are never a fast chat-game win");
        assertEquals(1, ChallengeTracker.progress(player, config, weekly.get(0), DAY));

        ChallengeTracker.Result fast = ChallengeTracker.record(player, config, Activity.win(GameType.UNSCRAMBLE, 2.5, 5), DAY);
        assertEquals(Arrays.asList("fast", "points"), ids(fast.getCompleted()));
        assertTrue(fast.isAllDailyCompleted());
        assertEquals(5, ChallengeTracker.progress(player, config, daily.get(2), DAY), "never above the amount");
        assertEquals(3, ChallengeTracker.completed(player, config, Period.DAILY, DAY));
        assertEquals(4, player.getStat(Stat.CHALLENGES_COMPLETED));

        ChallengeTracker.Result more = ChallengeTracker.record(player, config, Activity.win(GameType.UNSCRAMBLE, 1, 1), DAY);
        assertTrue(more.getCompleted().isEmpty(), "finished challenges don't complete twice");
        assertFalse(more.isAllDailyCompleted(), "only the activity that finished the last one reports it");
    }

    @Test
    void globalGamesAndEventsCountForTheirOwnTypes() throws Exception {
        ChallengeConfig config = config("daily:\n  challenges:\n"
                + "    play:\n      type: PLAY_GLOBAL\n      amount: 5\n"
                + "    win:\n      type: WIN_GLOBAL\n      amount: 5\n"
                + "    games:\n      type: WIN_GAMES\n      amount: 5\n"
                + "    event:\n      type: PLAY_EVENT\n      amount: 5\n");
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        ChallengeTracker.record(player, config, Activity.global(GameType.REACTION, 0, false), DAY);
        ChallengeTracker.record(player, config, Activity.global(GameType.REACTION, 1, true), DAY);
        ChallengeTracker.record(player, config, Activity.event(), DAY);
        List<Challenge> active = config.active(Period.DAILY, DAY);
        assertEquals(2, ChallengeTracker.progress(player, config, active.get(0), DAY));
        assertEquals(1, ChallengeTracker.progress(player, config, active.get(1), DAY));
        assertEquals(0, ChallengeTracker.progress(player, config, active.get(2), DAY), "global games aren't regular wins");
        assertEquals(1, ChallengeTracker.progress(player, config, active.get(3), DAY));
    }

    @Test
    void rewardsAreClaimedOnceAndResetWithThePeriod() throws Exception {
        ChallengeConfig config = config(FIXED);
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        Challenge win2 = config.active(Period.DAILY, DAY).get(0);

        assertTrue(ChallengeTracker.claimAll(player, config, DAY).isEmpty());
        ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 9, 1), DAY);
        ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 9, 1), DAY);
        assertEquals(Arrays.asList("win_2", "math"), ids(ChallengeTracker.claimAll(player, config, DAY)));
        assertTrue(ChallengeTracker.isClaimed(player, config, win2, DAY));
        assertTrue(ChallengeTracker.claimAll(player, config, DAY).isEmpty(), "only once");
        assertEquals("&eWin 2", win2.getName());
        assertEquals("[playermsg] &eWin 2", win2.fill(win2.getRewards().get(0)));

        LocalDate tomorrow = DAY.plusDays(1);
        assertEquals(0, ChallengeTracker.progress(player, config, win2, tomorrow), "a new day starts from zero");
        assertFalse(ChallengeTracker.isClaimed(player, config, win2, tomorrow));
        ChallengeTracker.record(player, config, Activity.win(GameType.TRIVIA, 9, 0), tomorrow);
        assertEquals(1, ChallengeTracker.progress(player, config, win2, tomorrow));
        assertEquals(0, player.getStat("challenge.daily.win_2.claimed"), "the claim was cleared on rollover");
        assertTrue(ChallengeTracker.isClaimed(player, config, config.active(Period.WEEKLY, DAY).get(1), tomorrow),
                "the week (from Friday) hasn't changed");

        ChallengeTracker.reset(player);
        assertEquals(0, ChallengeTracker.progress(player, config, win2, tomorrow));
        assertFalse(ChallengeTracker.isClaimed(player, config, config.active(Period.WEEKLY, DAY).get(1), tomorrow));
    }

    @Test
    void progressFromManyThreadsIsNeverLostAndCompletesOnce() throws Exception {
        ChallengeConfig config = config("daily:\n  challenges:\n    many:\n      type: WIN_GAMES\n      amount: 4000\n");
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> completions = Collections.synchronizedList(new ArrayList<>());
        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int i = 0; i < 600; i++) {
                    completions.add(ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 1, 1), DAY).getCompleted().size());
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(4000, ChallengeTracker.progress(player, config, config.active(Period.DAILY, DAY).get(0), DAY));
        assertEquals(1, completions.stream().mapToInt(Integer::intValue).sum());
    }

    /* ------------------------------------------ streaks ----------------------------------------- */

    @Test
    void streaksGrowOncePerDayAndBreakAfterAMissedDay() {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        assertEquals(1, StreakTracker.advance(player, StreakType.DAILY, DAY));
        assertEquals(0, StreakTracker.advance(player, StreakType.DAILY, DAY), "already counted today");
        assertEquals(2, StreakTracker.advance(player, StreakType.DAILY, DAY.plusDays(1)));
        assertEquals(3, StreakTracker.advance(player, StreakType.DAILY, DAY.plusDays(2)));
        assertEquals(3, StreakTracker.current(player, StreakType.DAILY, DAY.plusDays(3)), "alive until the day ends");
        assertEquals(0, StreakTracker.current(player, StreakType.DAILY, DAY.plusDays(4)), "a whole day missed");
        assertEquals(1, StreakTracker.advance(player, StreakType.DAILY, DAY.plusDays(4)));
        assertEquals(3, StreakTracker.longest(player, StreakType.DAILY));
        assertEquals(0, StreakTracker.current(player, StreakType.GLOBAL, DAY), "streaks are separate");
    }

    @Test
    void everyUnclaimedDayOfALivingStreakCanBeClaimed() {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        StreakTracker.advance(player, StreakType.GLOBAL, DAY);
        StreakTracker.advance(player, StreakType.GLOBAL, DAY.plusDays(1));
        assertEquals(2, StreakTracker.unclaimed(player, StreakType.GLOBAL, DAY.plusDays(1)));
        assertArrayEquals(new int[]{1, 2}, StreakTracker.claim(player, StreakType.GLOBAL, DAY.plusDays(1)));
        assertNull(StreakTracker.claim(player, StreakType.GLOBAL, DAY.plusDays(1)));
        StreakTracker.advance(player, StreakType.GLOBAL, DAY.plusDays(2));
        assertArrayEquals(new int[]{3, 3}, StreakTracker.claim(player, StreakType.GLOBAL, DAY.plusDays(3)), "claimable the next day too");

        StreakTracker.advance(player, StreakType.GLOBAL, DAY.plusDays(3));
        assertEquals(0, StreakTracker.unclaimed(player, StreakType.GLOBAL, DAY.plusDays(5)), "lost once the streak broke");
        StreakTracker.advance(player, StreakType.GLOBAL, DAY.plusDays(5));
        assertArrayEquals(new int[]{1, 1}, StreakTracker.claim(player, StreakType.GLOBAL, DAY.plusDays(5)), "a new streak pays day 1 again");
    }

    @Test
    void settingAStreakPaysNothingUntilTheNextDay() {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        StreakTracker.set(player, StreakType.CHALLENGE, 10, DAY);
        assertEquals(10, StreakTracker.current(player, StreakType.CHALLENGE, DAY));
        assertEquals(10, StreakTracker.longest(player, StreakType.CHALLENGE));
        assertEquals(0, StreakTracker.unclaimed(player, StreakType.CHALLENGE, DAY));
        assertEquals(11, StreakTracker.advance(player, StreakType.CHALLENGE, DAY.plusDays(1)));
        assertArrayEquals(new int[]{11, 11}, StreakTracker.claim(player, StreakType.CHALLENGE, DAY.plusDays(1)));
        StreakTracker.set(player, StreakType.CHALLENGE, 0, DAY.plusDays(1));
        assertEquals(0, StreakTracker.current(player, StreakType.CHALLENGE, DAY.plusDays(1)));
        assertEquals(11, StreakTracker.longest(player, StreakType.CHALLENGE), "longest is kept");
    }

    @Test
    void pastDaySevenTheLadderRepeatsOrLoops() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString("daily:\n  rewards:\n    1: [one]\n    7: [seven]\n");
        StreakConfig repeat = StreakConfig.load(file, problem -> { });
        assertEquals(Collections.singletonList("one"), repeat.getRewards(StreakType.DAILY, 1));
        assertTrue(repeat.getRewards(StreakType.DAILY, 2).isEmpty());
        assertEquals(Collections.singletonList("seven"), repeat.getRewards(StreakType.DAILY, 30));
        assertFalse(repeat.isEnabled(StreakType.GLOBAL), "no section, no streak");

        file.set("after-day-7", "loop");
        StreakConfig loop = StreakConfig.load(file, problem -> { });
        assertEquals(1, loop.ladderDay(8));
        assertEquals(7, loop.ladderDay(14));
        assertEquals(Collections.singletonList("one"), loop.getRewards(StreakType.DAILY, 15));
    }

    /* ------------------------------------------ placeholders ------------------------------------ */

    @Test
    void placeholdersShowChallengesAndStreaks() throws Exception {
        ChallengeConfig config = config(FIXED);
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 9, 1), DAY);
        StreakTracker.advance(player, StreakType.DAILY, DAY.minusDays(1));
        StreakTracker.advance(player, StreakType.DAILY, DAY);

        PlaceholderResolver resolver = new PlaceholderResolver(LeaderboardSnapshot.EMPTY, player,
                new PlaceholderResolver.Status(null, -1), "", config, DAY);
        assertEquals("0", resolver.resolve("daily_completed"));
        assertEquals("3", resolver.resolve("daily_total"));
        assertEquals("1", resolver.resolve("weekly_completed"));
        assertEquals("2", resolver.resolve("weekly_total"));
        assertEquals("1", resolver.resolve("challenges_completed"));
        assertEquals("2", resolver.resolve("streak_daily"));
        assertEquals("2", resolver.resolve("streak_daily_longest"));
        assertEquals("0", resolver.resolve("streak_global"));
        assertNull(resolver.resolve("streak_weird"));

        PlaceholderResolver nobody = new PlaceholderResolver(LeaderboardSnapshot.EMPTY, null,
                new PlaceholderResolver.Status(null, -1), "", config, DAY);
        assertEquals("0", nobody.resolve("daily_completed"));
        assertEquals("0", nobody.resolve("streak_challenge_longest"));
    }

    @Test
    void oneChallengeOrOneStreakCanBeClaimedAlone() throws Exception {
        ChallengeConfig config = config(FIXED);
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        Challenge win2 = config.active(Period.DAILY, DAY).get(0);
        assertFalse(ChallengeTracker.claim(player, config, win2, DAY), "not finished yet");
        ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 9, 1), DAY);
        ChallengeTracker.record(player, config, Activity.win(GameType.MATH, 9, 1), DAY);
        assertTrue(ChallengeTracker.claim(player, config, win2, DAY));
        assertFalse(ChallengeTracker.claim(player, config, win2, DAY), "only once");
        assertEquals(Collections.singletonList("math"), ids(ChallengeTracker.claimAll(player, config, DAY)),
                "claim all skips the one already claimed");
        // A menu opened before a reload holds the old object; it is matched by id
        Challenge reloaded = config(FIXED).active(Period.DAILY, DAY).get(0);
        assertFalse(ChallengeTracker.claim(player, config, reloaded, DAY));

        StreakTracker.advance(player, StreakType.DAILY, DAY);
        StreakTracker.advance(player, StreakType.DAILY, DAY.plusDays(1));
        assertEquals(0, StreakTracker.claimed(player, StreakType.DAILY, DAY.plusDays(1)));
        StreakTracker.claim(player, StreakType.DAILY, DAY.plusDays(1));
        assertEquals(2, StreakTracker.claimed(player, StreakType.DAILY, DAY.plusDays(1)));
        assertEquals(0, StreakTracker.claimed(player, StreakType.DAILY, DAY.plusDays(5)), "a dead streak has nothing claimed");
    }
}
