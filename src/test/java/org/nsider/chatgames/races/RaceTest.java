package org.nsider.chatgames.races;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.api.GameType;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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

class RaceTest {

    private static final UUID ALEX = new UUID(0, 1);
    private static final UUID STEVE = new UUID(0, 2);

    private static RaceData data(String yaml, List<String> warnings) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return RaceData.load(config.getConfigurationSection("mine.data"), warnings::add);
    }

    @Test
    void dataKeepsTargetsAmountsAndRewards() throws Exception {
        List<String> warnings = new ArrayList<>();
        RaceData data = data("mine:\n  data:\n    stone:\n      10:\n        rewards: [reward_1]\n      20:\n        rewards: [reward_1, reward_2]\n"
                + "    COAL_ORE:\n      x:\n        rewards: [reward_1]\n      -3: {}\n      5: {}\n    EMPTY: {}\n", warnings);
        assertEquals(Arrays.asList("STONE", "COAL_ORE"), new ArrayList<>(data.getTargets()), "upper-cased, in file order");
        assertEquals(Arrays.asList(10, 20), data.getAmounts("stone"));
        assertEquals(Arrays.asList("reward_1", "reward_2"), data.getRewards("Stone", 20));
        assertTrue(data.getRewards("STONE", 15).isEmpty(), "amounts not in the file have no rewards");
        assertEquals(Collections.singletonList(5), data.getAmounts("COAL_ORE"));
        assertEquals(2, warnings.size(), "x and -3 are reported: " + warnings);
    }

    @Test
    void pickOnlyChoosesTargetsThatExist() throws Exception {
        RaceData data = data("mine:\n  data:\n    STONE:\n      10: {}\n    NEW_BLOCK:\n      3: {}\n", new ArrayList<>());
        for (long seed = 0; seed < 30; seed++) {
            RaceData.Pick pick = data.pick(new Random(seed), name -> !name.equals("NEW_BLOCK"));
            assertNotNull(pick);
            assertEquals("STONE", pick.getTarget());
            assertEquals(10, pick.getAmount());
        }
        assertNull(data.pick(new Random(1), name -> false), "nothing exists on this version");
        assertNull(RaceData.EMPTY.pick(new Random(1), name -> true));
    }

    @Test
    void theDefaultRacesFileHasDataForEveryRace() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/races.yml"), StandardCharsets.UTF_8)) {
            file.load(reader);
        }
        for (GameType type : GameType.values()) {
            if (!type.isRace()) continue;
            List<String> warnings = new ArrayList<>();
            RaceData data = RaceData.load(file.getConfigurationSection(type.getConfigKey() + ".data"), warnings::add);
            assertFalse(data.isEmpty(), type + " has targets");
            assertTrue(warnings.isEmpty(), type + ": " + warnings);
        }
    }

    @Test
    void firstToTheGoalWinsAndCountsAddUp() {
        RaceProgress progress = new RaceProgress(5);
        assertFalse(progress.add(ALEX, 3));
        assertFalse(progress.add(STEVE, 4));
        assertFalse(progress.add(ALEX, 0), "nothing crafted counts as nothing");
        assertTrue(progress.add(ALEX, 2), "1.9.9 counted a player's first action as 1 whatever the amount");
        assertEquals(5, progress.getCount(ALEX));
        assertEquals(ALEX, progress.getWinner());
        assertFalse(progress.add(STEVE, 10), "the race is over");
        assertFalse(progress.close(), "can't close a race someone won");
        assertEquals(ALEX, progress.getWinner());
    }

    @Test
    void aClosedRaceHasNoWinnerAndIgnoresLateActions() {
        RaceProgress progress = new RaceProgress(1);
        assertTrue(progress.close());
        assertFalse(progress.close(), "only closes once");
        assertTrue(progress.isOver());
        assertFalse(progress.add(ALEX, 1));
        assertNull(progress.getWinner());
    }

    @Test
    void exactlyOneWinnerWhenManyThreadsFinishTogether() throws Exception {
        for (int round = 0; round < 50; round++) {
            RaceProgress progress = new RaceProgress(100);
            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger winners = new AtomicInteger();
            for (int t = 0; t < threads; t++) {
                UUID player = new UUID(1, t);
                pool.execute(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    for (int i = 0; i < 100; i++) {
                        if (progress.add(player, 1)) winners.incrementAndGet();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            assertEquals(1, winners.get());
            assertNotNull(progress.getWinner());
        }
    }

    @Test
    void shiftClickCraftsWhatTheIngredientsAndRoomAllow() {
        assertEquals(64, RaceCounts.shiftCrafted(4, 16, 2304), "16 logs into planks, plenty of room");
        assertEquals(8, RaceCounts.shiftCrafted(4, 10, 5), "room for 5: the second craft only partly fits");
        assertEquals(0, RaceCounts.shiftCrafted(4, 10, 0), "full inventory");
        assertEquals(0, RaceCounts.shiftCrafted(1, 0, 64));
    }

    @Test
    void plainClicksCraftOnceIfTheCursorCanTakeIt() {
        assertEquals(4, RaceCounts.clickCrafted(4, true, false, 0, 64));
        assertEquals(4, RaceCounts.clickCrafted(4, false, true, 60, 64));
        assertEquals(0, RaceCounts.clickCrafted(4, false, true, 61, 64), "would overflow the cursor");
        assertEquals(0, RaceCounts.clickCrafted(4, false, false, 1, 64), "holding something else");
    }

    @Test
    void titleCaseForUntranslatedNames() {
        assertEquals("Oak Log", RaceTarget.titleCase("OAK_LOG"));
        assertEquals("Zombie", RaceTarget.titleCase("ZOMBIE"));
    }
}
