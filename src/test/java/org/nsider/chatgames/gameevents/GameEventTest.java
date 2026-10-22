package org.nsider.chatgames.gameevents;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.games.StartOptions;
import org.nsider.chatgames.globalgames.GlobalGameConfig;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEventTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static EventConfig config(String yaml, List<String> warnings) throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        file.loadFromString(yaml);
        return EventConfig.load(file, warnings::add);
    }

    private static ZonedDateTime at(int day, int hour, int minute) {
        // October 2026: the 17th is a Saturday
        return ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, UTC);
    }

    @Test
    void theDefaultFileLoadsCleanly() throws Exception {
        YamlConfiguration file = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/events.yml"), StandardCharsets.UTF_8)) {
            file.load(reader);
        }
        List<String> warnings = new ArrayList<>();
        EventConfig config = EventConfig.load(file, warnings::add);
        assertTrue(warnings.isEmpty(), warnings.toString());
        EventDefinition cup = config.get("weekend_cup");
        assertNotNull(cup);
        assertEquals(5, cup.getSteps().size());
        assertEquals(1, cup.getSchedule().size());
        assertArrayEquals(new int[]{5, 3, 1}, cup.getPlacePoints());
        assertTrue(cup.getSteps().get(4).isGlobal());
        assertArrayEquals(new String[]{"stone", "100"}, cup.getSteps().get(4).getArgs());
        assertEquals("hard", cup.getSteps().get(2).getSettings().get("difficulty"));
        assertNotNull(config.get("QUICK_QUIZ"), "ids ignore case");
        for (String key : new String[]{"start", "step", "end", "nobody", "stopped"}) {
            assertFalse(file.getStringList("messages." + key).isEmpty(), key);
        }
        assertNotNull(file.getString("messages.step_points"));
        assertNotNull(file.getString("messages.result"));
    }

    @Test
    void schedulesParseDaysAndDailyTimes() {
        assertEquals("SATURDAY 20:00", String.valueOf(EventSchedule.parse("saturday 20:00")));
        assertEquals("DAILY 18:30", String.valueOf(EventSchedule.parse("DAILY 18:30")));
        assertEquals("DAILY 07:05", String.valueOf(EventSchedule.parse("07:05")));
        assertNull(EventSchedule.parse("someday 20:00"));
        assertNull(EventSchedule.parse("SATURDAY 25:00"));
        assertNull(EventSchedule.parse("SATURDAY at 20:00"));
        assertNull(EventSchedule.parse(""));
    }

    @Test
    void aScheduleIsDueOnlyInsideTheCheckedWindow() {
        EventSchedule saturday = EventSchedule.parse("SATURDAY 20:00");
        assertTrue(saturday.isDue(at(17, 19, 59), at(17, 20, 0)), "the start time itself counts");
        assertFalse(saturday.isDue(at(17, 20, 0), at(17, 20, 1)), "but only once");
        assertFalse(saturday.isDue(at(16, 19, 59), at(16, 20, 1)), "Friday");
        assertTrue(saturday.isDue(at(16, 23, 0), at(18, 1, 0)), "a long gap across days");

        EventSchedule midnight = EventSchedule.parse("DAILY 00:00");
        assertTrue(midnight.isDue(at(17, 23, 59), at(18, 0, 0)), "across midnight");
        assertFalse(midnight.isDue(at(18, 0, 0), at(18, 0, 0)), "an empty window");
    }

    @Test
    void theNextStartIsTheFirstOneAfterNow() {
        EventSchedule saturday = EventSchedule.parse("SATURDAY 20:00");
        assertEquals(at(17, 20, 0), saturday.next(at(17, 10, 0)));
        assertEquals(at(24, 20, 0), saturday.next(at(17, 20, 0)), "not the one starting now");
        assertEquals(at(18, 9, 0), EventSchedule.parse("09:00").next(at(17, 9, 30)));
    }

    @Test
    void badStepsAndEventsAreSkippedWithAWarning() throws Exception {
        List<String> warnings = new ArrayList<>();
        EventConfig config = config(
                "events:\n"
                        + "  Bad Id!:\n"
                        + "    steps: [{game: math}]\n"
                        + "  empty:\n"
                        + "    steps: [{game: nonsense}]\n"
                        + "  ok:\n"
                        + "    schedule: ['FUNDAY 10:00', 'MONDAY 10:00']\n"
                        + "    steps:\n"
                        + "      - {game: memorize, global: true}\n"
                        + "      - {game: math, time: '20', winners: 2, args: [12, x, 3]}\n", warnings);
        assertNull(config.get("bad id!"));
        assertNull(config.get("empty"));
        EventDefinition ok = config.get("ok");
        assertNotNull(ok);
        assertEquals(1, ok.getSchedule().size());
        assertFalse(ok.getSteps().get(0).isGlobal(), "memorize can't be global");
        assertEquals(20, ok.getSteps().get(1).getTimeSeconds());
        assertArrayEquals(new String[]{"12", "x", "3"}, ok.getSteps().get(1).getArgs());
        assertEquals(5, warnings.size(), warnings.toString());
    }

    @Test
    void aStepReplacesItsGamesSettings() throws Exception {
        EventConfig config = config(
                "events:\n"
                        + "  e:\n"
                        + "    steps:\n"
                        + "      - {game: word_association, time: 15, winners: 2, settings: {difficulty: hard}}\n"
                        + "      - {game: trivia, global: true, time: 40}\n"
                        + "      - {game: mine, time: 90, winners: 4}\n", new ArrayList<>());
        List<EventStep> steps = config.get("e").getSteps();
        MemoryConfiguration games = new MemoryConfiguration();
        games.set("timeToGuess_seconds", 30);
        games.set("winners", 1);
        games.set("difficulty", "mixed");
        games.set("points", 2);

        StartOptions normal = steps.get(0).toOptions(GlobalGameConfig.EMPTY);
        assertFalse(normal.isGlobal());
        ConfigurationSection applied = normal.apply(games);
        assertEquals(15, applied.getInt("timeToGuess_seconds"));
        assertEquals(2, applied.getInt("winners"));
        assertEquals("hard", applied.getString("difficulty"));
        assertEquals(2, applied.getInt("points"));
        assertEquals("mixed", games.getString("difficulty"), "games.yml itself is untouched");

        StartOptions global = steps.get(1).toOptions(GlobalGameConfig.EMPTY);
        assertTrue(global.isGlobal());
        assertEquals(40, global.getGlobal().getDurationSeconds());
        assertEquals(40, global.apply(games).getInt("timeToGuess_seconds"));

        StartOptions race = steps.get(2).toOptions(GlobalGameConfig.EMPTY);
        assertEquals(90, race.raceTimeSeconds());
        assertFalse(race.apply(new MemoryConfiguration()).contains("winners"), "races have one winner");
        assertEquals(0, StartOptions.NORMAL.raceTimeSeconds());
    }

    @Test
    void scoresRankByPointsAndThenByWhoScoredFirst() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        EventScores scores = new EventScores();
        int[] points = {5, 3};

        Map<UUID, Integer> earned = scores.addStep(Arrays.asList(a, b, c), points, 1);
        assertEquals(5, (int) earned.get(a));
        assertEquals(1, (int) earned.get(c), "past the listed places: participation");
        scores.addStep(Arrays.asList(b, c), points, 1);
        scores.addStep(Arrays.asList(c), points, 1);
        // a 5, b 3+5 = 8, c 1+3+5 = 9
        List<EventScores.Standing> ranking = scores.ranking();
        assertEquals(c, ranking.get(0).getUuid());
        assertEquals(9, ranking.get(0).getPoints());
        assertEquals(2, scores.place(b));
        assertEquals(3, scores.place(a));
        assertEquals(0, scores.place(UUID.randomUUID()));

        EventScores tie = new EventScores();
        tie.addStep(Arrays.asList(b), points, 0);
        tie.addStep(Arrays.asList(a), points, 0);
        assertEquals(b, tie.ranking().get(0).getUuid(), "equal points: who scored first");
        tie.addStep(Arrays.asList(UUID.randomUUID()), new int[0], 0);
        assertEquals(3, tie.participants(), "zero points still took part");
    }

    @Test
    void rewardsFallBackToParticipation() throws Exception {
        EventDefinition event = config(
                "events:\n"
                        + "  e:\n"
                        + "    steps: [{game: math}]\n"
                        + "    rewards:\n"
                        + "      1: ['first']\n"
                        + "      participation: ['everyone']\n", new ArrayList<>()).get("e");
        assertEquals(Arrays.asList("first"), event.rewardsFor(1));
        assertEquals(Arrays.asList("everyone"), event.rewardsFor(2));
        EventDefinition bare = config("events:\n  e:\n    steps: [{game: math}]\n", new ArrayList<>()).get("e");
        assertTrue(bare.rewardsFor(1).isEmpty());
    }
}
