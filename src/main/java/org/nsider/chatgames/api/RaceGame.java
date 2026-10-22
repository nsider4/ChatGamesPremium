package org.nsider.chatgames.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A custom race, for {@link ChatGamesAPI#registerRace}. ChatGames runs it like the built-in
 * races (announcements, time limit, winner, rewards); your plugin reports what players do with
 * {@link ChatGamesAPI#addRaceProgress}.
 *
 * <pre>{@code
 * RaceGame bosses = RaceGame.builder("boss_hunt")
 *         .displayName("Boss Hunt")
 *         .target("dragon", 1)
 *         .target("wither", 1, 2)
 *         .targetName(target -> target.equals("DRAGON") ? "Ender Dragon" : "Wither")
 *         .build();
 * GameType type = ChatGamesAPI.get().registerRace(plugin, bosses);
 * // later, in your own listener:
 * ChatGamesAPI.get().addRaceProgress(player, type, "wither", 1);
 * }</pre>
 *
 * <p>Like the built-in races, each race picks one of its targets and an amount from races.yml.
 * The targets given here are the defaults written there the first time. Target names are
 * compared without case.</p>
 */
public final class RaceGame {

    private final String id;
    private final String displayName;
    private final String icon;
    private final Map<String, List<Integer>> targets;
    private final Function<String, String> targetName;
    private final Predicate<String> targetFilter;
    private final Map<String, Object> settings;
    private final List<String> startMessage;
    private final List<String> winMessage;
    private final List<String> expiredMessage;

    private RaceGame(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.icon = builder.icon;
        this.targets = Collections.unmodifiableMap(new LinkedHashMap<>(builder.targets));
        this.targetName = builder.targetName;
        this.targetFilter = builder.targetFilter;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<>(builder.settings));
        this.startMessage = builder.startMessage;
        this.winMessage = builder.winMessage;
        this.expiredMessage = builder.expiredMessage;
    }

    /** @param id the race's id (see {@link GameType#isValidId}) */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    /** @return the default targets (upper-case) and amounts for races.yml */
    public Map<String, List<Integer>> getTargets() {
        return targets;
    }

    /** @return the name shown for a target (given upper-case), as {@code %value%} in messages */
    public String targetName(String target) {
        String name = targetName == null ? null : targetName.apply(target);
        return name == null || name.trim().isEmpty() ? ChatGame.titleCase(target.toLowerCase(Locale.ROOT)) : name;
    }

    /** @return whether a target (given upper-case) can be raced for, e.g. in {@code /chatgames <id> <target>} */
    public boolean isValidTarget(String target) {
        return targetFilter == null || targetFilter.test(target);
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public List<String> getStartMessage() {
        return startMessage;
    }

    public List<String> getWinMessage() {
        return winMessage;
    }

    public List<String> getExpiredMessage() {
        return expiredMessage;
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private String icon = "compass";
        private final Map<String, List<Integer>> targets = new LinkedHashMap<>();
        private Function<String, String> targetName;
        private Predicate<String> targetFilter;
        private final Map<String, Object> settings = new LinkedHashMap<>();
        private List<String> startMessage;
        private List<String> winMessage;
        private List<String> expiredMessage;

        private Builder(String id) {
            this.id = id;
        }

        /** The name shown in menus and in {@code %game%}. Defaults to the id in title case. */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /** The menu icon: any material name from 1.8 to 26.x. Defaults to a compass. */
        public Builder icon(String material) {
            this.icon = material;
            return this;
        }

        /**
         * A default target and the amounts it can ask for, e.g. {@code target("wither", 1, 2)}.
         * A race without targets of its own can use one, like {@code target("any", 5, 10)}, and
         * report progress with a null target.
         */
        public Builder target(String target, int... amounts) {
            List<Integer> list = new ArrayList<>();
            for (int amount : amounts) {
                if (amount > 0) list.add(amount);
            }
            if (!list.isEmpty()) targets.put(target.trim().toUpperCase(Locale.ROOT), Collections.unmodifiableList(list));
            return this;
        }

        /** How a target is shown to players. Gets the target upper-case; null keeps the title-cased name. */
        public Builder targetName(Function<String, String> targetName) {
            this.targetName = targetName;
            return this;
        }

        /** Which target names are valid (gets them upper-case). Defaults to any name. */
        public Builder targetFilter(Predicate<String> targetFilter) {
            this.targetFilter = targetFilter;
            return this;
        }

        /** A default races.yml value, e.g. {@code setting("timeToComplete_seconds", 300)}. */
        public Builder setting(String path, Object value) {
            settings.put(path, value);
            return this;
        }

        /**
         * The default messages.yml lines. They may use {@code %value%} (the target), {@code %amount%},
         * {@code %timeToComplete%}, {@code %winner%}, {@code %time%} and {@code %game%}. A null list
         * keeps the standard lines.
         */
        public Builder messages(List<String> start, List<String> win, List<String> expired) {
            this.startMessage = start == null ? null : Collections.unmodifiableList(new ArrayList<>(start));
            this.winMessage = win == null ? null : Collections.unmodifiableList(new ArrayList<>(win));
            this.expiredMessage = expired == null ? null : Collections.unmodifiableList(new ArrayList<>(expired));
            return this;
        }

        /** @throws IllegalStateException if the id is malformed or there are no targets */
        public RaceGame build() {
            if (!GameType.isValidId(id)) {
                throw new IllegalStateException("Race id '" + id + "' must be 2-32 lower-case letters, digits or _ and start with a letter");
            }
            if (targets.isEmpty()) throw new IllegalStateException("Race '" + id + "' needs at least one target with an amount");
            if (displayName == null || displayName.trim().isEmpty()) displayName = ChatGame.titleCase(id);
            return new RaceGame(this);
        }
    }
}
