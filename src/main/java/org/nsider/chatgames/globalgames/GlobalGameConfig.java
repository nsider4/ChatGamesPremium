package org.nsider.chatgames.globalgames;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.registry.GameRegistry;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * global_games.yml: when global games start, the progress bar, and each game's goal,
 * time and payout. Unchanging; a reload builds a new one.
 */
public final class GlobalGameConfig {

    private static final List<Integer> DEFAULT_ANNOUNCED = Collections.unmodifiableList(Arrays.asList(25, 50, 75));

    public static final GlobalGameConfig EMPTY = new GlobalGameConfig(0, 5, 0, 1, Bar.DEFAULT,
            DEFAULT_ANNOUNCED, Collections.<GameType, Entry>emptyMap(), new MemoryConfiguration());

    private final double chanceAfterGame;
    private final int delaySeconds;
    private final int intervalMinutes;
    private final int minimumPlayers;
    private final Bar bar;
    private final List<Integer> announcedPercents;
    private final Map<GameType, Entry> games;
    /** The {@code defaults} section, for games played globally that aren't listed. */
    private final ConfigurationSection defaults;

    private GlobalGameConfig(double chanceAfterGame, int delaySeconds, int intervalMinutes, int minimumPlayers, Bar bar,
                             List<Integer> announcedPercents, Map<GameType, Entry> games, ConfigurationSection defaults) {
        this.chanceAfterGame = chanceAfterGame;
        this.delaySeconds = delaySeconds;
        this.intervalMinutes = intervalMinutes;
        this.minimumPlayers = minimumPlayers;
        this.bar = bar;
        this.announcedPercents = announcedPercents;
        this.games = games;
        this.defaults = defaults;
    }

    /**
     * @return false for games that can't be played by everyone at once: Memorize (the list
     *         would have to stay hidden from everyone) and Guess the Number (everyone would
     *         get there by guessing)
     */
    public static boolean supports(@Nonnull GameType type) {
        return type != GameType.MEMORIZE && type != GameType.GUESS_THE_NUMBER;
    }

    @Nonnull
    public static GlobalGameConfig load(@Nonnull ConfigurationSection file, @Nonnull Consumer<String> warn) {
        ConfigurationSection defaults = file.getConfigurationSection("defaults");
        if (defaults == null) defaults = new MemoryConfiguration();
        Map<GameType, Entry> games = new LinkedHashMap<>();
        ConfigurationSection section = file.getConfigurationSection("games");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                GameType type = GameType.fromName(key);
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (type == null || entry == null) {
                    GameRegistry.unknownName(warn, "games." + key + " isn't a game or race; skipped");
                    continue;
                }
                if (!supports(type)) {
                    warn.accept("games." + key + " can't be played as a global game; skipped");
                    continue;
                }
                games.put(type, Entry.read(type, entry, defaults, warn));
            }
        }
        List<Integer> announced = file.isList("progress-bar.announce-at")
                ? file.getIntegerList("progress-bar.announce-at") : DEFAULT_ANNOUNCED;
        return new GlobalGameConfig(
                Math.max(0, Math.min(100, file.getDouble("triggers.chance-after-game", 0))),
                Math.max(1, file.getInt("triggers.delay-seconds", 5)),
                Math.max(0, file.getInt("triggers.interval-minutes", 0)),
                Math.max(1, file.getInt("triggers.minimum-players", 1)),
                Bar.read(file.getConfigurationSection("progress-bar")),
                Collections.unmodifiableList(new ArrayList<>(announced)),
                games, defaults);
    }

    /** @return the percent chance that a global game follows a regular one */
    public double getChanceAfterGame() {
        return chanceAfterGame;
    }

    /** @return seconds between the regular game ending and the global one starting */
    public int getDelaySeconds() {
        return delaySeconds;
    }

    /** @return minutes between global games started on a timer, 0 for none */
    public int getIntervalMinutes() {
        return intervalMinutes;
    }

    /** @return the players needed online for a global game to start by itself */
    public int getMinimumPlayers() {
        return minimumPlayers;
    }

    @Nonnull
    public Bar getBar() {
        return bar;
    }

    /** @return the percentages of the goal announced in chat, e.g. 25, 50 and 75 */
    @Nonnull
    public List<Integer> getAnnouncedPercents() {
        return announcedPercents;
    }

    /** @return the game's settings if it is enabled as a global game, else null */
    @Nullable
    public Entry get(@Nonnull GameType type) {
        Entry entry = games.get(type);
        return entry != null && entry.enabled ? entry : null;
    }

    /**
     * @return the game's settings whether or not it is enabled (an event can still play it
     *         globally), or the {@code defaults} if it isn't listed
     */
    @Nonnull
    public Entry find(@Nonnull GameType type) {
        Entry entry = games.get(type);
        return entry != null ? entry : Entry.read(type, new MemoryConfiguration(), defaults, problem -> { });
    }

    /** @return every enabled global game */
    @Nonnull
    public List<GameType> getEnabled() {
        List<GameType> enabled = new ArrayList<>();
        for (Entry entry : games.values()) {
            if (entry.enabled) enabled.add(entry.type);
        }
        return enabled;
    }

    /**
     * @param usable whether a game can run right now (e.g. races turned off)
     * @return a random enabled game, or null if there is none
     */
    @Nullable
    public GameType pick(@Nonnull Random random, @Nonnull Predicate<GameType> usable) {
        List<GameType> candidates = new ArrayList<>();
        for (GameType type : getEnabled()) {
            if (usable.test(type)) candidates.add(type);
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    /** How the progress bar looks: a boss bar from 1.9, the action bar on 1.8. */
    public static final class Bar {
        static final Bar DEFAULT = new Bar(true, "YELLOW", "SEGMENTED_10");

        private final boolean enabled;
        private final String color;
        private final String style;

        private Bar(boolean enabled, String color, String style) {
            this.enabled = enabled;
            this.color = color;
            this.style = style;
        }

        static Bar read(@Nullable ConfigurationSection section) {
            if (section == null) return DEFAULT;
            return new Bar(section.getBoolean("enabled", true),
                    section.getString("color", DEFAULT.color).toUpperCase(Locale.ROOT),
                    section.getString("style", DEFAULT.style).toUpperCase(Locale.ROOT));
        }

        public boolean isEnabled() {
            return enabled;
        }

        /** @return a BarColor name, e.g. YELLOW */
        @Nonnull
        public String getColor() {
            return color;
        }

        /** @return a BarStyle name, e.g. SEGMENTED_10 */
        @Nonnull
        public String getStyle() {
            return style;
        }
    }

    /** One game's global settings. */
    public static final class Entry {
        private final GameType type;
        private final boolean enabled;
        private final int durationSeconds;
        private final int goal;
        private final int minContribution;
        private final int points;
        private final int pointsPerContribution;
        private final int answersPerQuestion;
        private final int questionSeconds;
        @Nullable private final Count count;

        private Entry(GameType type, boolean enabled, int durationSeconds, int goal, int minContribution, int points,
                      int pointsPerContribution, int answersPerQuestion, int questionSeconds, @Nullable Count count) {
            this.type = type;
            this.enabled = enabled;
            this.durationSeconds = durationSeconds;
            this.goal = goal;
            this.minContribution = minContribution;
            this.points = points;
            this.pointsPerContribution = pointsPerContribution;
            this.answersPerQuestion = answersPerQuestion;
            this.questionSeconds = questionSeconds;
            this.count = count;
        }

        /** A game's own value, else the defaults' (1.x's {@code duration_seconds} is still read). */
        static Entry read(GameType type, ConfigurationSection own, ConfigurationSection defaults, Consumer<String> warn) {
            int duration = own.contains("duration-seconds") ? own.getInt("duration-seconds")
                    : own.contains("duration_seconds") ? own.getInt("duration_seconds")
                    : defaults.getInt("duration-seconds", 120);
            return new Entry(type, own.getBoolean("enabled", true),
                    Math.max(5, duration),
                    Math.max(1, number(own, defaults, "goal", type.isRace() ? 64 : 10)),
                    Math.max(1, number(own, defaults, "min-contribution", 1)),
                    Math.max(0, number(own, defaults, "points", 1)),
                    Math.max(0, number(own, defaults, "points-per-contribution", 0)),
                    Math.max(1, number(own, defaults, "answers-per-question", 3)),
                    Math.max(3, number(own, defaults, "question-seconds", 15)),
                    type.isGlobalOnly() ? Count.read(own, warn) : null);
        }

        private static int number(ConfigurationSection own, ConfigurationSection defaults, String key, int fallback) {
            return own.contains(key) ? own.getInt(key) : defaults.getInt(key, fallback);
        }

        @Nonnull
        public GameType getType() {
            return type;
        }

        /** @return these settings with another time limit (at least 5 seconds) */
        @Nonnull
        public Entry withDuration(int seconds) {
            return new Entry(type, enabled, Math.max(5, seconds), goal, minContribution, points, pointsPerContribution,
                    answersPerQuestion, questionSeconds, count);
        }

        /** @return these settings with another goal (at least 1) */
        @Nonnull
        public Entry withGoal(int goal) {
            return new Entry(type, enabled, durationSeconds, Math.max(1, goal), minContribution, points,
                    pointsPerContribution, answersPerQuestion, questionSeconds, count);
        }

        /** @return how long the server has to reach the goal */
        public int getDurationSeconds() {
            return durationSeconds;
        }

        /** @return the correct answers (or race units) the server needs; Count's comes from its mode */
        public int getGoal() {
            return goal;
        }

        /** @return the least a player must contribute to be rewarded */
        public int getMinContribution() {
            return minContribution;
        }

        /** @return the points every rewarded player gets */
        public int getPoints() {
            return points;
        }

        /** @return the extra points per contribution */
        public int getPointsPerContribution() {
            return pointsPerContribution;
        }

        /** @return the points a player who contributed this much gets once the goal is reached */
        public int pointsFor(int contribution) {
            return contribution < minContribution ? 0 : points + pointsPerContribution * contribution;
        }

        /** @return how many players can answer one question before the next one */
        public int getAnswersPerQuestion() {
            return answersPerQuestion;
        }

        /** @return the longest one question stays up */
        public int getQuestionSeconds() {
            return questionSeconds;
        }

        /** @return the Count settings, or null for every other game */
        @Nullable
        public Count getCount() {
            return count;
        }
    }

    /** The Count game's settings. */
    public static final class Count {
        private final Map<String, Mode> modes;
        private final boolean noDoubleCount;
        private final boolean failOnMistake;
        private final int maxMistakes;

        private Count(Map<String, Mode> modes, boolean noDoubleCount, boolean failOnMistake, int maxMistakes) {
            this.modes = modes;
            this.noDoubleCount = noDoubleCount;
            this.failOnMistake = failOnMistake;
            this.maxMistakes = maxMistakes;
        }

        static Count read(ConfigurationSection section, Consumer<String> warn) {
            Map<String, Mode> modes = new LinkedHashMap<>();
            ConfigurationSection list = section.getConfigurationSection("modes");
            if (list != null) {
                for (String name : list.getKeys(false)) {
                    ConfigurationSection mode = list.getConfigurationSection(name);
                    if (mode == null) continue;
                    modes.put(name.toLowerCase(Locale.ROOT), Mode.of(name.toLowerCase(Locale.ROOT),
                            mode.getInt("start", 1), mode.getInt("target", 10), mode.getInt("step", 1)));
                }
            }
            if (modes.isEmpty()) modes.put("up", Mode.of("up", 1, 10, 1));
            String onMistake = section.getString("on-mistake", "reset").toLowerCase(Locale.ROOT);
            if (!onMistake.equals("reset") && !onMistake.equals("fail")) {
                warn.accept("count.on-mistake must be reset or fail, not " + onMistake + "; using reset");
            }
            return new Count(Collections.unmodifiableMap(modes), section.getBoolean("no-double-count", true),
                    onMistake.equals("fail"), Math.max(0, section.getInt("max-mistakes", 0)));
        }

        /** @return the modes by name */
        @Nonnull
        public Map<String, Mode> getModes() {
            return modes;
        }

        /** @return true if nobody may say two numbers in a row */
        public boolean isNoDoubleCount() {
            return noDoubleCount;
        }

        /** @return true if a mistake ends the game, false if the count starts over */
        public boolean isFailOnMistake() {
            return failOnMistake;
        }

        /** @return the mistakes that end the game (when counting starts over), 0 for no limit */
        public int getMaxMistakes() {
            return maxMistakes;
        }
    }

    /** One way to count: from {@code start} to {@code target} in steps of {@code step}. */
    public static final class Mode {
        private final String name;
        private final int start;
        private final int target;
        private final int step;

        private Mode(String name, int start, int target, int step) {
            this.name = name;
            this.start = start;
            this.target = target;
            this.step = step;
        }

        /** The step's sign follows the direction from start to target. */
        @Nonnull
        public static Mode of(@Nonnull String name, int start, int target, int step) {
            int size = Math.max(1, Math.abs(step));
            return new Mode(name, start, target, target < start ? -size : size);
        }

        @Nonnull
        public String getName() {
            return name;
        }

        public int getStart() {
            return start;
        }

        public int getTarget() {
            return target;
        }

        public int getStep() {
            return step;
        }
    }
}
