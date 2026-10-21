package org.nsider.chatgames.globalgames;

import org.bukkit.configuration.ConfigurationSection;
import org.nsider.chatgames.enums.GameType;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * global_games.yml: when global games start, and which games can be played globally.
 * Unchanging; a reload builds a new one.
 */
public final class GlobalGameConfig {

    public static final GlobalGameConfig EMPTY = new GlobalGameConfig(0, 5, 0, 1, 3,
            Collections.<GameType, Entry>emptyMap());

    private final double chanceAfterGame;
    private final int delaySeconds;
    private final int intervalMinutes;
    private final int minimumPlayers;
    private final int rewardedPlaces;
    private final Map<GameType, Entry> games;

    private GlobalGameConfig(double chanceAfterGame, int delaySeconds, int intervalMinutes, int minimumPlayers,
                             int rewardedPlaces, Map<GameType, Entry> games) {
        this.chanceAfterGame = chanceAfterGame;
        this.delaySeconds = delaySeconds;
        this.intervalMinutes = intervalMinutes;
        this.minimumPlayers = minimumPlayers;
        this.rewardedPlaces = rewardedPlaces;
        this.games = games;
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
        Map<GameType, Entry> games = new EnumMap<>(GameType.class);
        ConfigurationSection section = file.getConfigurationSection("games");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                GameType type = GameType.fromName(key);
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (type == null || entry == null) {
                    warn.accept("games." + key + " isn't a game or race; skipped");
                    continue;
                }
                if (!supports(type)) {
                    warn.accept("games." + key + " can't be played as a global game; skipped");
                    continue;
                }
                games.put(type, new Entry(type, entry.getBoolean("enabled", true),
                        Math.max(5, entry.getInt("duration_seconds", 30)), Math.max(0, entry.getInt("points", 1))));
            }
        }
        return new GlobalGameConfig(
                Math.max(0, Math.min(100, file.getDouble("triggers.chance-after-game", 0))),
                Math.max(1, file.getInt("triggers.delay-seconds", 5)),
                Math.max(0, file.getInt("triggers.interval-minutes", 0)),
                Math.max(1, file.getInt("triggers.minimum-players", 1)),
                Math.max(0, file.getInt("rewarded-places", 3)),
                games);
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

    /** @return how many of the first places count as winning (stats, challenges, streaks) */
    public int getRewardedPlaces() {
        return rewardedPlaces;
    }

    /** @return the game's settings if it is enabled as a global game, else null */
    @Nullable
    public Entry get(@Nonnull GameType type) {
        Entry entry = games.get(type);
        return entry != null && entry.enabled ? entry : null;
    }

    /**
     * @return the game's settings whether or not it is enabled (an event can still play it
     *         globally), or defaults of 30 seconds and 1 point if it isn't listed
     */
    @Nonnull
    public Entry find(@Nonnull GameType type) {
        Entry entry = games.get(type);
        return entry != null ? entry : new Entry(type, false, 30, 1);
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

    /** One game's global settings. */
    public static final class Entry {
        private final GameType type;
        private final boolean enabled;
        private final int durationSeconds;
        private final int points;

        Entry(GameType type, boolean enabled, int durationSeconds, int points) {
            this.type = type;
            this.enabled = enabled;
            this.durationSeconds = durationSeconds;
            this.points = points;
        }

        @Nonnull
        public GameType getType() {
            return type;
        }

        /** @return these settings with another time limit (at least 5 seconds) */
        @Nonnull
        public Entry withDuration(int seconds) {
            return new Entry(type, enabled, Math.max(5, seconds), points);
        }

        /** @return how long everyone has to answer */
        public int getDurationSeconds() {
            return durationSeconds;
        }

        /** @return the points every player who makes it gets */
        public int getPoints() {
            return points;
        }
    }
}
