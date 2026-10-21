package org.nsider.chatgames.globalgames;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalGameTest {

    private static final UUID ANN = new UUID(1, 1);
    private static final UUID BOB = new UUID(1, 2);
    private static final UUID CY = new UUID(1, 3);

    private static GlobalGameConfig config(String yaml, List<String> warnings) throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return GlobalGameConfig.load(file, warnings::add);
    }

    @Test
    void theDefaultFileLoadsCleanly() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/global_games.yml"), StandardCharsets.UTF_8)) {
            file.load(reader);
        }
        List<String> warnings = new ArrayList<>();
        GlobalGameConfig config = GlobalGameConfig.load(file, warnings::add);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals(10, config.getChanceAfterGame());
        assertEquals(Arrays.asList(25, 50, 75), config.getAnnouncedPercents());
        assertTrue(config.getEnabled().contains(GameType.REACTION));
        assertTrue(config.getEnabled().contains(GameType.MINE), "global race");
        assertTrue(config.getEnabled().contains(GameType.COUNT));
        assertNull(config.get(GameType.CLICKABLE), "listed but turned off");
        assertEquals(15, config.get(GameType.MATH).getGoal());
        assertEquals(3, config.get(GameType.MATH).getAnswersPerQuestion(), "from the defaults");

        GlobalGameConfig.Count count = config.get(GameType.COUNT).getCount();
        assertNotNull(count);
        assertEquals(-1, count.getModes().get("down").getStep(), "counting down goes in steps of -1");
        assertEquals(5, count.getMaxMistakes());
        assertNull(config.get(GameType.MATH).getCount());

        for (String key : new String[]{"start", "race_start", "count_start", "question", "completed", "failed",
                "count_broken", "count_broken_final"}) {
            assertFalse(file.getStringList("messages." + key).isEmpty(), key);
        }
        for (String key : new String[]{"answered", "milestone", "counted", "rewarded", "too_little", "bar",
                "count_reasons.ahead", "count_reasons.behind", "count_reasons.repeated", "count_reasons.twice"}) {
            assertNotNull(file.getString("messages." + key), key);
        }
        assertNotNull(file.getConfigurationSection("rewards.completed"));
        assertNotNull(file.getConfigurationSection("rewards.top-contributors.1"));
    }

    @Test
    void gamesThatCantBeSharedAreRefused() throws Exception {
        List<String> warnings = new ArrayList<>();
        GlobalGameConfig config = config("games:\n  memorize: {}\n  guess_the_number: {}\n  chess: {}\n"
                + "  math:\n    duration-seconds: 1\n    points: -4\n    goal: 0\n", warnings);
        assertEquals(3, warnings.size(), warnings.toString());
        assertEquals(Collections.singletonList(GameType.MATH), config.getEnabled());
        assertEquals(5, config.get(GameType.MATH).getDurationSeconds(), "at least 5 seconds");
        assertEquals(0, config.get(GameType.MATH).getPoints());
        assertEquals(1, config.get(GameType.MATH).getGoal());
        assertFalse(GlobalGameConfig.supports(GameType.MEMORIZE));
        assertTrue(GlobalGameConfig.supports(GameType.HUNT));
        assertTrue(GlobalGameConfig.supports(GameType.COUNT));
    }

    @Test
    void gamesFallBackToTheDefaultsAndTheOldTimeKey() throws Exception {
        GlobalGameConfig config = config("defaults:\n  goal: 7\n  points: 4\n  question-seconds: 9\n"
                + "games:\n  math:\n    duration_seconds: 45\n  trivia:\n    goal: 3\n", new ArrayList<>());
        GlobalGameConfig.Entry math = config.get(GameType.MATH);
        assertEquals(45, math.getDurationSeconds(), "1.x's duration_seconds still works");
        assertEquals(7, math.getGoal());
        assertEquals(9, math.getQuestionSeconds());
        assertEquals(3, config.get(GameType.TRIVIA).getGoal());
        assertEquals(4, config.find(GameType.SEQUENCE).getPoints(), "an unlisted game uses the defaults");
        assertEquals(64, config("{}", new ArrayList<>()).find(GameType.MINE).getGoal(), "races default to more");
        assertEquals(30, math.withDuration(30).getDurationSeconds());
        assertEquals(500, math.withGoal(500).getGoal());
    }

    @Test
    void pickOnlyChoosesUsableGames() throws Exception {
        GlobalGameConfig config = config("games:\n  math: {}\n  mine: {}\n", new ArrayList<>());
        Random random = new Random(3);
        Set<GameType> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) seen.add(config.pick(random, type -> true));
        assertEquals(2, seen.size());
        for (int i = 0; i < 20; i++) assertEquals(GameType.MATH, config.pick(random, type -> !type.isRace()));
        assertNull(config.pick(random, type -> false));
    }

    @Test
    void pointsNeedTheMinimumContribution() throws Exception {
        GlobalGameConfig.Entry entry = config("games:\n  math:\n    min-contribution: 2\n    points: 3\n"
                + "    points-per-contribution: 2\n", new ArrayList<>()).get(GameType.MATH);
        assertEquals(0, entry.pointsFor(1));
        assertEquals(3 + 2 * 2, entry.pointsFor(2));
        assertEquals(3 + 2 * 10, entry.pointsFor(10));
    }

    @Test
    void contributionsAddUpToTheGoalOnce() {
        CoopProgress progress = new CoopProgress(5);
        assertEquals(2, progress.add(ANN, 2).getAfter());
        assertEquals(1, progress.add(BOB, 1).getContribution());
        CoopProgress.Step last = progress.add(BOB, 9);
        assertTrue(last.isCompleted());
        assertEquals(5, last.getAfter(), "never past the goal");
        assertEquals(CoopProgress.Outcome.COMPLETED, progress.getOutcome());
        assertNull(progress.add(CY, 1), "nothing counts once it's over");
        assertFalse(progress.close(CoopProgress.Outcome.FAILED), "it ends once");
        assertEquals(0, progress.contribution(CY));
        assertEquals(100, progress.getPercent());
    }

    @Test
    void theRankingPutsTheBiggestContributorsFirstAndKeepsTiesInOrder() {
        CoopProgress progress = new CoopProgress(100);
        progress.add(ANN, 3);
        progress.add(BOB, 5);
        progress.add(CY, 3);
        List<CoopProgress.Contributor> ranking = progress.ranking();
        assertEquals(Arrays.asList(BOB, ANN, CY), Arrays.asList(ranking.get(0).getUuid(), ranking.get(1).getUuid(),
                ranking.get(2).getUuid()));
        assertEquals(Arrays.asList(1, 2, 3), Arrays.asList(ranking.get(0).getPlace(), ranking.get(1).getPlace(),
                ranking.get(2).getPlace()));
        assertEquals(3, progress.contributors());
    }

    @Test
    void aResetKeepsContributionsButNotProgress() {
        CoopProgress progress = new CoopProgress(10);
        progress.add(ANN, 4);
        assertTrue(progress.resetProgress());
        assertEquals(0, progress.getProgress());
        assertEquals(4, progress.contribution(ANN));
        assertTrue(progress.close(CoopProgress.Outcome.FAILED));
        assertFalse(progress.resetProgress());
    }

    @Test
    void milestonesAreAnnouncedWhenCrossed() {
        List<Integer> at = Arrays.asList(25, 50, 75);
        assertEquals(Collections.singletonList(25), CoopProgress.crossed(2, 3, 10, at), "25% of 10 is reached at 3");
        assertEquals(Collections.<Integer>emptyList(), CoopProgress.crossed(3, 4, 10, at));
        assertEquals(Arrays.asList(25, 50, 75), CoopProgress.crossed(0, 8, 10, at), "a big jump crosses several");
        assertEquals(Collections.singletonList(50), CoopProgress.crossed(249, 250, 500, at));
        assertEquals(Collections.<Integer>emptyList(), CoopProgress.crossed(0, 10, 10, Arrays.asList(0, 100)),
                "0% and 100% are never announced");
    }

    @Test
    void everyContributionCountsWhenManyArriveAtOnce() throws Exception {
        CoopProgress progress = new CoopProgress(1000);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger completions = new AtomicInteger();
        for (int t = 0; t < threads; t++) {
            UUID player = new UUID(9, t);
            pool.execute(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int i = 0; i < 200; i++) {
                    CoopProgress.Step step = progress.add(player, 1);
                    if (step != null && step.isCompleted()) completions.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, completions.get(), "exactly one contribution completes it");
        assertEquals(1000, progress.getProgress());
        int total = 0;
        for (CoopProgress.Contributor contributor : progress.ranking()) total += contributor.getAmount();
        assertEquals(1000, total, "nothing counted after the goal");
    }

    @Test
    void countingInOrderFinishesTheCount() {
        Counter counter = new Counter(1, 3, 1, true);
        assertEquals(3, counter.getGoal());
        assertEquals(Counter.Result.COUNTED, counter.submit(ANN, 1));
        assertEquals(Counter.Result.COUNTED, counter.submit(BOB, 2));
        assertEquals(Counter.Result.FINISHED, counter.submit(ANN, 3));
    }

    @Test
    void countMistakes() {
        Counter counter = new Counter(1, 10, 1, true);
        counter.submit(ANN, 1);
        counter.submit(BOB, 2);
        assertEquals(Counter.Result.AHEAD, counter.submit(CY, 5));
        assertEquals(Counter.Result.REPEATED, counter.submit(CY, 2));
        assertEquals(Counter.Result.BEHIND, counter.submit(CY, 1));
        assertEquals(Counter.Result.TWICE, counter.submit(BOB, 3), "Bob said the last number");
        assertTrue(Counter.Result.TWICE.isMistake());
        assertFalse(Counter.Result.FINISHED.isMistake());
        assertEquals(3, counter.getNext(), "a mistake doesn't move the count");

        counter.reset();
        assertEquals(1, counter.getNext());
        assertEquals(0, counter.getCounted());
        assertEquals(Counter.Result.COUNTED, counter.submit(BOB, 1), "after a reset anyone can start");
        assertEquals(Counter.Result.BEHIND, new Counter(1, 10, 1, true).submit(ANN, 0), "nothing said yet: not a repeat");
    }

    @Test
    void theSamePlayerMayCountTwiceWhenAllowed() {
        Counter counter = new Counter(1, 10, 1, false);
        counter.submit(ANN, 1);
        assertEquals(Counter.Result.COUNTED, counter.submit(ANN, 2));
    }

    @Test
    void countingDownAndInSteps() {
        GlobalGameConfig.Mode down = GlobalGameConfig.Mode.of("down", 10, 1, 1);
        assertEquals(-1, down.getStep(), "the direction follows start and target");
        Counter counter = new Counter(down.getStart(), down.getTarget(), down.getStep(), true);
        assertEquals(10, counter.getGoal());
        assertEquals(Counter.Result.COUNTED, counter.submit(ANN, 10));
        assertEquals(Counter.Result.AHEAD, counter.submit(BOB, 7), "lower is further along when counting down");
        assertEquals(Counter.Result.BEHIND, counter.submit(BOB, 12));

        Counter fives = new Counter(5, 23, 5, true);
        assertEquals(4, fives.getGoal(), "5, 10, 15, 20: the target is lowered to where the steps land");
        assertEquals(20, fives.getTarget());
        assertEquals(Counter.Result.COUNTED, fives.submit(ANN, 5));
        assertEquals(Counter.Result.AHEAD, fives.submit(BOB, 11));
        assertEquals(Counter.Result.BEHIND, fives.submit(BOB, 9));
    }

    @Test
    void onlyNumbersAreTries() {
        assertEquals(Integer.valueOf(12), Counter.parse(" 12 "));
        assertEquals(Integer.valueOf(-3), Counter.parse("-3"));
        assertEquals(Integer.valueOf(4), Counter.parse("+4"));
        assertNull(Counter.parse("12!"));
        assertNull(Counter.parse("hello 3"));
        assertNull(Counter.parse("1.5"));
        assertNull(Counter.parse("99999999999"), "too long to be a count");
    }

    @Test
    void twoPlayersSayingTheSameNumberAtOnceCountItOnce() throws Exception {
        for (int round = 0; round < 50; round++) {
            Counter counter = new Counter(1, 100, 1, false);
            int threads = 6;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger counted = new AtomicInteger();
            for (int t = 0; t < threads; t++) {
                UUID player = new UUID(5, t);
                pool.execute(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (!counter.submit(player, 1).isMistake()) counted.incrementAndGet();
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            assertEquals(1, counted.get());
            assertEquals(2, counter.getNext());
        }
    }

    @Test
    void countModesFromTheStartValues() throws Exception {
        GlobalGameConfig.Count count = config("games:\n  count:\n    modes:\n      up: {start: 1, target: 5}\n",
                new ArrayList<>()).get(GameType.COUNT).getCount();
        Random random = new Random(1);
        assertEquals("up", CountSession.mode(count, new String[0], random).getName());
        assertEquals("up", CountSession.mode(count, new String[]{"UP"}, random).getName());
        GlobalGameConfig.Mode custom = CountSession.mode(count, new String[]{"20", "0", "4"}, random);
        assertEquals(-4, custom.getStep());
        assertEquals(1, CountSession.mode(count, new String[]{"3", "9"}, random).getStep(), "step defaults to 1");
        assertNull(CountSession.mode(count, new String[]{"sideways"}, random));
        assertNull(CountSession.mode(count, new String[]{"4", "4"}, random));
        assertNull(CountSession.mode(count, new String[]{"4"}, random));
    }

    @Test
    void countSettingsHaveSafeDefaults() throws Exception {
        List<String> warnings = new ArrayList<>();
        GlobalGameConfig.Count count = config("games:\n  count:\n    on-mistake: explode\n", warnings)
                .get(GameType.COUNT).getCount();
        assertEquals(1, warnings.size());
        assertFalse(count.isFailOnMistake());
        assertTrue(count.isNoDoubleCount());
        assertEquals(Collections.singleton("up"), count.getModes().keySet());
    }
}
