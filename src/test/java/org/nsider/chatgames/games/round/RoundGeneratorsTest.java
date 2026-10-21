package org.nsider.chatgames.games.round;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;

import java.io.FileInputStream;
import java.io.IOException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generates many rounds of every chat game from the default config files, so a
 * broken default (or a YAML typo) fails the build instead of a live server.
 */
class RoundGeneratorsTest {

    private static final int ROUNDS = 300;

    private static YamlConfiguration games;
    private static YamlConfiguration words;
    private static YamlConfiguration messages;

    @BeforeAll
    static void loadDefaults() throws IOException {
        games = load("games.yml");
        words = load("words.yml");
        messages = load("messages.yml");
    }

    private static YamlConfiguration load(String name) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream("src/main/resources/" + name), StandardCharsets.UTF_8)) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            assertFalse(config.getKeys(false).isEmpty(), name + " didn't parse");
            return config;
        }
    }

    private static RoundContext context(GameType type, long seed) {
        return new RoundContext(games.getConfigurationSection(type.getConfigKey()), words,
                messages.getConfigurationSection(type.getConfigKey()), new Random(seed), key -> null);
    }

    private static Round generate(GameType type, long seed, String... args) {
        RoundGenerator generator = RoundGenerators.get(type);
        assertNotNull(generator, type + " has no generator");
        return generator.generate(context(type, seed), args);
    }

    @Test
    void everyChatGameHasDefaultsAndGeneratesPlayableRounds() {
        for (GameType type : GameType.values()) {
            if (type.isRace() || type.isGlobalOnly()) continue;
            assertNotNull(games.getConfigurationSection(type.getConfigKey()), type + " missing from games.yml");
            ConfigurationSection section = messages.getConfigurationSection(type.getConfigKey());
            assertNotNull(section, type + " missing from messages.yml");
            assertFalse(section.getStringList("correct_message").isEmpty(), type + " has no correct_message");
            assertFalse(section.getStringList("time_expired_message").isEmpty(), type + " has no time_expired_message");
            assertFalse(section.getStringList("gameStartAnnouncement").isEmpty(), type + " has no gameStartAnnouncement");

            for (int seed = 0; seed < ROUNDS; seed++) {
                Round round = generate(type, seed);
                String answer = round.getMainAnswer();
                assertFalse(answer.trim().isEmpty(), type + " gave an empty answer");
                assertEquals(AnswerChecker.Result.CORRECT, AnswerChecker.check(round, answer, true),
                        type + " doesn't accept its own answer " + answer);
                for (String hint : round.getHints()) {
                    assertNotEquals(answer, hint, type + " hint gives the whole answer away");
                }
            }
        }
    }

    @Test
    void memorizeHasItsOwnSectionsAndTheOldNameIsGone() {
        assertNotNull(messages.getConfigurationSection("memorize"));
        assertFalse(messages.getStringList("memorize.messageAfterMemorized").isEmpty());
        assertFalse(messages.isConfigurationSection("shoppinglist"), "stale shoppinglist section in messages.yml");
        assertFalse(games.isConfigurationSection("shoppinglist"), "stale shoppinglist section in games.yml");
        assertNotNull(messages.getString("hint_message"));
        assertNotNull(messages.getString("countdown_actionbar"));
    }

    @Test
    void solvedMathEvaluatesToItsAnswerAndMissingValuesFitTheEquation() {
        int missing = 0;
        for (int seed = 0; seed < ROUNDS; seed++) {
            Round round = generate(GameType.MATH, seed);
            String display = round.getDisplay();
            if (display.contains("?")) {
                missing++;
                String[] sides = display.split(" = ");
                String filled = sides[0].replace("?", round.getMainAnswer());
                assertEquals(Long.parseLong(sides[1]), evaluate(filled), display);
            } else {
                assertEquals(Long.parseLong(round.getMainAnswer()), evaluate(display), display);
            }
        }
        assertTrue(missing > 0 && missing < ROUNDS, "both modes are configured, both should appear");
    }

    @Test
    void wordAssociationsHaveThreeCluesAndHardOnesRevealThemOneByOne() {
        ConfigurationSection data = words.getConfigurationSection("word_association.data");
        assertNotNull(data);
        int[] counts = new int[3];
        for (String id : data.getKeys(false)) {
            assertEquals(3, data.getStringList(id + ".clues").size(), id);
            String difficulty = data.getString(id + ".difficulty");
            counts[Arrays.asList("easy", "medium", "hard").indexOf(difficulty)]++;
        }
        assertTrue(counts[0] >= 15 && counts[1] >= 15 && counts[2] >= 15, Arrays.toString(counts));

        Round easy = generate(GameType.WORD_ASSOCIATION, 1, "nether_portal");
        assertEquals("Obsidian &7• &bFlint and Steel &7• &bPurple", easy.getDisplay());
        assertEquals(AnswerChecker.Result.CORRECT, AnswerChecker.check(easy, "portal", false), "aliases count");
        assertEquals("easy", easy.getPlaceholders().get("%difficulty%"));

        Round hard = generate(GameType.WORD_ASSOCIATION, 1, "netherite");
        assertEquals("Ancient Debris &7• &b? &7• &b?", hard.getDisplay());
        assertEquals(Arrays.asList("Ancient Debris &7• &bGold &7• &b?", "Ancient Debris &7• &bGold &7• &bUpgrade"),
                hard.getHints());
    }

    @Test
    void wordAssociationDifficultyPicksOnlyThatLevel() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("difficulty", "hard");
        for (int seed = 0; seed < 50; seed++) {
            Round round = RoundGenerators.get(GameType.WORD_ASSOCIATION).generate(
                    new RoundContext(settings, words, null, new Random(seed), key -> null));
            assertEquals("hard", round.getPlaceholders().get("%difficulty%"));
        }
    }

    @Test
    void mathAcceptsAnExpressionFromTheCommand() {
        Round round = generate(GameType.MATH, 1, "12", "x", "3", "+", "1");
        assertEquals("37", round.getMainAnswer());
        assertEquals("12 x 3 + 1", round.getDisplay());
    }

    @Test
    void sequencesFollowTheirPattern() {
        for (int seed = 0; seed < ROUNDS; seed++) {
            Round round = generate(GameType.SEQUENCE, seed);
            String[] parts = round.getDisplay().split(", ");
            assertEquals("?", parts[parts.length - 1]);
            List<Long> terms = new ArrayList<>();
            for (int i = 0; i < parts.length - 1; i++) terms.add(Long.parseLong(parts[i]));
            terms.add(Long.parseLong(round.getMainAnswer()));
            String pattern = round.getPlaceholders().get("%pattern%");
            assertTrue(follows(pattern, terms), pattern + " broken: " + terms);
        }
    }

    private static boolean follows(String pattern, List<Long> t) {
        for (int i = 2; i < t.size(); i++) {
            switch (pattern) {
                case "add":
                    if (t.get(i) - t.get(i - 1) != t.get(1) - t.get(0)) return false;
                    break;
                case "multiply":
                    if (t.get(i) * t.get(0) != t.get(i - 1) * t.get(1)) return false;
                    break;
                case "squares":
                    long root = Math.round(Math.sqrt(t.get(i)));
                    if (root * root != t.get(i) || Math.round(Math.sqrt(t.get(i - 1))) != root - 1) return false;
                    break;
                case "alternating":
                    // Every step repeats the step two places earlier
                    if (i >= 3 && t.get(i) - t.get(i - 1) != t.get(i - 2) - t.get(i - 3)) return false;
                    break;
                case "fibonacci":
                    if (t.get(i) != t.get(i - 1) + t.get(i - 2)) return false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    @Test
    void unscrambleAndFilloutHideTheWordFairly() {
        for (int seed = 0; seed < ROUNDS; seed++) {
            Round scrambled = generate(GameType.UNSCRAMBLE, seed);
            char[] a = scrambled.getMainAnswer().toCharArray();
            char[] b = scrambled.getDisplay().toCharArray();
            Arrays.sort(a);
            Arrays.sort(b);
            assertTrue(Arrays.equals(a, b), "scramble must use the same letters");

            Round fillout = generate(GameType.FILLOUT, seed);
            String word = fillout.getMainAnswer();
            String puzzle = fillout.getDisplay();
            assertEquals(word.length(), puzzle.length());
            assertTrue(puzzle.contains("_"), "at least one letter is hidden: " + puzzle);
            for (int i = 0; i < word.length(); i++) {
                assertTrue(puzzle.charAt(i) == '_' || puzzle.charAt(i) == word.charAt(i));
            }
        }
    }

    @Test
    void memorizeDrawsDistinctItemsFromEveryPool() {
        Set<String> pools = new HashSet<>();
        for (int seed = 0; seed < ROUNDS; seed++) {
            Round round = generate(GameType.MEMORIZE, seed);
            pools.add(round.getPlaceholders().get("%pool%"));
            List<String> items = Arrays.asList(round.getMainAnswer().split(", "));
            assertEquals(games.getInt("memorize.listLength"), items.size());
            assertEquals(items.size(), new HashSet<>(items).size(), "items repeat: " + items);
            assertTrue(round.isListAnswer() && round.getMemorizeSeconds() > 0);
            if (items.size() > 1) {
                List<String> shuffled = new ArrayList<>(items);
                Collections.reverse(shuffled);
                assertEquals(AnswerChecker.Result.WRONG, AnswerChecker.check(round, String.join(", ", shuffled), false),
                        "order matters by default");
            }
        }
        assertEquals(new HashSet<>(games.getStringList("memorize.pools")), pools, "every configured pool gets used");
    }

    @Test
    void guessTheNumberRangesAndHintsAlwaysContainTheAnswer() {
        for (int seed = 0; seed < ROUNDS; seed++) {
            Round round = generate(GameType.GUESS_THE_NUMBER, seed);
            int answer = Integer.parseInt(round.getMainAnswer());
            List<String> ranges = new ArrayList<>(round.getHints());
            ranges.add(round.getDisplay());
            for (String range : ranges) {
                String[] bounds = range.split("-");
                assertTrue(Integer.parseInt(bounds[0]) <= answer && answer <= Integer.parseInt(bounds[1]), range + " / " + answer);
            }
        }
    }

    @Test
    void variableAnswersAreTheMissingDifference() {
        for (int seed = 0; seed < ROUNDS; seed++) {
            Round round = generate(GameType.VARIABLE, seed);
            assertTrue(Integer.parseInt(round.getMainAnswer()) > 0, "the unknown symbol is worth something");
            assertTrue(round.getPlaceholders().get("%line_3%").contains(round.getPlaceholders().get("%symbol%")));
        }
    }

    @Test
    void blacklistedWordsAreSkippedNotEverything() {
        YamlConfiguration custom = new YamlConfiguration();
        custom.set("unscramble.Words", Arrays.asList("ass", "creeper", "zombie"));
        custom.set("blacklistedWords", Arrays.asList("ASS"));
        RoundGenerator generator = RoundGenerators.get(GameType.UNSCRAMBLE);
        Set<String> seen = new HashSet<>();
        RoundContext context = new RoundContext(null, custom, null, new Random(3), key -> null);
        for (int i = 0; i < 100; i++) {
            seen.add(generator.generate(context).getMainAnswer());
        }
        assertEquals(new HashSet<>(Arrays.asList("creeper", "zombie")), seen);
    }

    @Test
    void hintDelaysSpreadOverTheRound() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("timeToGuess_seconds", 20);
        settings.set("hints.enable", true);
        settings.set("hints.after-percent", 50);
        settings.set("hints.max", 2);
        long[] delays = new GameSettings(GameType.UNSCRAMBLE, settings).hintDelaysTicks(3);
        assertTrue(Arrays.equals(new long[]{200, 300}, delays), Arrays.toString(delays));
        assertEquals(1, new GameSettings(GameType.UNSCRAMBLE, settings).hintDelaysTicks(1).length, "never more than prepared");
    }

    /** A plain evaluator, independent of the generator's own. */
    private static long evaluate(String expression) {
        String[] tokens = expression.split(" ");
        List<Long> terms = new ArrayList<>();
        List<String> signs = new ArrayList<>();
        long term = Long.parseLong(tokens[0]);
        for (int i = 1; i < tokens.length; i += 2) {
            long next = Long.parseLong(tokens[i + 1]);
            switch (tokens[i]) {
                case "x": term *= next; break;
                case "/":
                    assertEquals(0, term % next, "inexact division in " + expression);
                    term /= next;
                    break;
                default:
                    terms.add(term);
                    signs.add(tokens[i]);
                    term = next;
            }
        }
        terms.add(term);
        long result = terms.get(0);
        for (int i = 0; i < signs.size(); i++) {
            result += signs.get(i).equals("+") ? terms.get(i + 1) : -terms.get(i + 1);
        }
        return result;
    }
}
