package org.nsider.chatgames.globalgames;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.Game;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.races.RaceProgress;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalGameTest {

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
        assertEquals(3, config.getRewardedPlaces());
        assertTrue(config.getEnabled().contains(GameType.REACTION));
        assertTrue(config.getEnabled().contains(GameType.MINE), "global race");
        assertNull(config.get(GameType.CLICKABLE), "listed but turned off");
        for (String key : new String[]{"start", "race_start", "end", "nobody"}) {
            assertFalse(file.getStringList("messages." + key).isEmpty(), key);
        }
        assertNotNull(file.getString("messages.solved"));
        assertNotNull(file.getConfigurationSection("rewards.participation"));
    }

    @Test
    void gamesThatCantBeSharedAreRefused() throws Exception {
        List<String> warnings = new ArrayList<>();
        GlobalGameConfig config = config("games:\n  memorize: {}\n  guess_the_number: {}\n  chess: {}\n"
                + "  math:\n    duration_seconds: 1\n    points: -4\n", warnings);
        assertEquals(3, warnings.size(), warnings.toString());
        assertEquals(Collections.singletonList(GameType.MATH), config.getEnabled());
        assertEquals(5, config.get(GameType.MATH).getDurationSeconds(), "at least 5 seconds");
        assertEquals(0, config.get(GameType.MATH).getPoints());
        assertFalse(GlobalGameConfig.supports(GameType.MEMORIZE));
        assertTrue(GlobalGameConfig.supports(GameType.HUNT));
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
    void globalSettingsKeepTheGameButUseTheGlobalTimeAndPoints() throws Exception {
        YamlConfiguration games = new YamlConfiguration();
        games.loadFromString("math:\n  enable: false\n  winners: 3\n  points: 5\n  timeToGuess_seconds: 60\n"
                + "  hints:\n    enable: true\n    max: 2\n");
        GlobalGameConfig config = config("games:\n  math:\n    duration_seconds: 15\n    points: 2\n", new ArrayList<>());
        ConfigurationSection merged = GlobalGameManager.settingsFor(games.getConfigurationSection("math"), config.get(GameType.MATH));
        GameSettings settings = new GameSettings(GameType.MATH, merged);
        assertEquals(15, settings.getTimeSeconds());
        assertEquals(2, settings.getPoints());
        assertTrue(settings.isHintsEnabled(), "nested values are copied too");
        assertEquals(2, settings.getHintsMax());
        assertEquals(5, games.getInt("math.points"), "the games.yml section is untouched");
    }

    @Test
    void aGlobalGameNeverRunsOutOfPlaces() {
        Round round = Round.builder(GameType.REACTION, "go").build();
        Game game = new Game(round, new GameSettings(GameType.REACTION, null), true);
        assertTrue(game.isGlobal());
        for (int i = 1; i <= 500; i++) assertEquals(i, game.claimPlace(new UUID(7, i)).getPlace());
        assertFalse(game.isFull());
        assertNull(game.claimPlace(new UUID(7, 1)), "one place each");
        assertFalse(new Game(round, new GameSettings(GameType.REACTION, null)).isGlobal());
    }

    @Test
    void everyoneWhoReachesTheGoalInAGlobalRaceFinishesOnce() throws Exception {
        RaceProgress progress = new RaceProgress(50, true);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> places = Collections.synchronizedList(new ArrayList<>());
        for (int t = 0; t < threads; t++) {
            UUID player = new UUID(9, t);
            pool.execute(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int i = 0; i < 80; i++) {
                    int place = progress.addAndPlace(player, 1);
                    if (place > 0) places.add(place);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        Collections.sort(places);
        List<Integer> expected = new ArrayList<>();
        for (int i = 1; i <= threads; i++) expected.add(i);
        assertEquals(expected, places, "places 1 to 8, each given once");
        assertNull(progress.getWinner(), "nobody wins a global race outright");
        assertEquals(threads, progress.getFinishers());

        assertTrue(progress.close());
        assertEquals(0, progress.addAndPlace(new UUID(9, 99), 100), "nothing counts once time is up");
    }
}
