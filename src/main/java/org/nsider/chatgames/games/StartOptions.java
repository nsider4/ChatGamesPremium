package org.nsider.chatgames.games;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How to start a game beyond its games.yml/races.yml settings: as a global game, and/or
 * with some settings replaced (an event step's time limit, winners, difficulty...).
 * Unchanging.
 */
public final class StartOptions {

    /** A race's time limit key in races.yml, also accepted as an override. */
    public static final String RACE_TIME = "timeToComplete_seconds";
    /** A chat game's time limit key in games.yml. */
    public static final String GAME_TIME = "timeToGuess_seconds";

    public static final StartOptions NORMAL = new StartOptions(null, Collections.<String, Object>emptyMap());

    @Nullable private final GlobalGameConfig.Entry global;
    private final Map<String, Object> overrides;

    private StartOptions(@Nullable GlobalGameConfig.Entry global, Map<String, Object> overrides) {
        this.global = global;
        this.overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
    }

    @Nonnull
    public static StartOptions global(@Nonnull GlobalGameConfig.Entry global) {
        return new StartOptions(global, Collections.<String, Object>emptyMap());
    }

    /**
     * @param global    the global settings, or null for a normal game
     * @param overrides settings to replace, by their games.yml key (and {@link #RACE_TIME} for races)
     */
    @Nonnull
    public static StartOptions of(@Nullable GlobalGameConfig.Entry global, @Nonnull Map<String, Object> overrides) {
        return new StartOptions(global, overrides);
    }

    public boolean isGlobal() {
        return global != null;
    }

    @Nullable
    public GlobalGameConfig.Entry getGlobal() {
        return global;
    }

    /** @return true if a game started this way can just use its games.yml settings */
    public boolean isPlain() {
        return global == null && overrides.isEmpty();
    }

    /**
     * @return a copy of a games.yml section with the global time limit and points, then the
     *         overrides, applied (the section itself is untouched)
     */
    @Nonnull
    public ConfigurationSection apply(@Nullable ConfigurationSection game) {
        MemoryConfiguration copy = new MemoryConfiguration();
        if (game != null) {
            for (Map.Entry<String, Object> value : game.getValues(true).entrySet()) {
                if (!(value.getValue() instanceof ConfigurationSection)) copy.set(value.getKey(), value.getValue());
            }
        }
        if (global != null) {
            copy.set(GAME_TIME, global.getDurationSeconds());
            copy.set("points", global.getPoints());
        }
        for (Map.Entry<String, Object> value : overrides.entrySet()) copy.set(value.getKey(), value.getValue());
        return copy;
    }

    /**
     * @return a race's time limit in seconds from these options, or 0 to use races.yml's
     */
    public int raceTimeSeconds() {
        Object time = overrides.get(RACE_TIME);
        if (time instanceof Number) return ((Number) time).intValue();
        return global != null ? global.getDurationSeconds() : 0;
    }
}
