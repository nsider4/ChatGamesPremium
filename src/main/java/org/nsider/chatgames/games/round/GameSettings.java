package org.nsider.chatgames.games.round;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.nsider.chatgames.api.GameType;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

/**
 * One chat game's settings from games.yml, with a default for every value a
 * 1.9.9 file may not have. Read fresh for each round, so /chatgames reload
 * applies to the next round.
 */
@Getter
public final class GameSettings {

    /** Answers faster than this are treated as macros unless games.yml says otherwise. */
    public static final int DEFAULT_MIN_ANSWER_MILLIS = 400;

    private final GameType type;
    private final ConfigurationSection section;
    private final boolean enabled;
    /** Relative chance of being picked by the rotation. */
    private final int weight;
    private final int winners;
    /** Points per win. */
    private final int points;
    private final boolean caseSensitive;
    private final int timeSeconds;
    private final String title;
    private final String subtitle;
    /** Answers quicker than this (after the round starts) are rejected; 0 turns the check off. */
    private final int minAnswerMillis;
    private final boolean hintsEnabled;
    /** Percentage of the time limit after which the first hint appears. */
    private final int hintsAfterPercent;
    private final int hintsMax;
    private final boolean countdownEnabled;
    /** The action bar countdown shows during the last N seconds; 0 means the whole round. */
    private final int countdownLastSeconds;

    public GameSettings(@Nonnull GameType type, @Nullable ConfigurationSection section) {
        this.type = type;
        this.section = section != null ? section : new MemoryConfiguration();
        ConfigurationSection s = this.section;
        this.enabled = s.getBoolean("enable", false);
        this.weight = Math.max(0, s.getInt("weight", 1));
        this.winners = Math.max(1, s.getInt("winners", 1));
        this.points = Math.max(0, s.getInt("points", 1));
        this.caseSensitive = s.getBoolean("case-sensitive", false);
        this.timeSeconds = Math.max(1, s.getInt("timeToGuess_seconds", 20));
        this.title = s.getString("titleMessage", "");
        this.subtitle = s.getString("subtitleMessage", "");
        this.minAnswerMillis = Math.max(0, s.getInt("min-answer-ms", DEFAULT_MIN_ANSWER_MILLIS));
        this.hintsEnabled = s.getBoolean("hints.enable", false);
        this.hintsAfterPercent = Math.max(1, Math.min(99, s.getInt("hints.after-percent", 50)));
        this.hintsMax = Math.max(0, Math.min(Hints.PREPARED, s.getInt("hints.max", 1)));
        this.countdownEnabled = s.getBoolean("countdown.enable", false);
        this.countdownLastSeconds = Math.max(0, s.getInt("countdown.last-seconds", 10));
    }

    /**
     * When each hint shows, in ticks after the round starts. The first comes at
     * after-percent of the time limit; the rest are spread evenly over what remains.
     */
    public long[] hintDelaysTicks(int available) {
        int count = Math.min(hintsMax, available);
        long total = timeSeconds * 20L;
        long[] delays = new long[Math.max(0, count)];
        for (int i = 0; i < delays.length; i++) {
            double percent = hintsAfterPercent + (100.0 - hintsAfterPercent) * i / count;
            delays[i] = Math.max(1L, (long) (total * percent / 100.0));
        }
        return delays;
    }
}
