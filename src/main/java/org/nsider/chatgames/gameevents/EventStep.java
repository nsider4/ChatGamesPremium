package org.nsider.chatgames.gameevents;

import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.StartOptions;
import org.nsider.chatgames.globalgames.GlobalGameConfig;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One game in an event: which game, whether everyone can win it (global), and the
 * settings it replaces for this event. Unchanging.
 */
public final class EventStep {

    private final GameType type;
    private final boolean global;
    /** Seconds to answer (or to finish a race); 0 keeps the game's own. */
    private final int timeSeconds;
    /** Winners of a regular chat game; 0 keeps the game's own. */
    private final int winners;
    private final Map<String, Object> settings;
    private final String[] args;

    EventStep(@Nonnull GameType type, boolean global, int timeSeconds, int winners,
              @Nonnull Map<String, Object> settings, @Nonnull String[] args) {
        this.type = type;
        this.global = global;
        this.timeSeconds = timeSeconds;
        this.winners = winners;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<>(settings));
        this.args = args.clone();
    }

    @Nonnull
    public GameType getType() {
        return type;
    }

    /** @return true if it is played as a global game (Count always is) */
    public boolean isGlobal() {
        return global || type.isGlobalOnly();
    }

    public int getTimeSeconds() {
        return timeSeconds;
    }

    public int getWinners() {
        return winners;
    }

    @Nonnull
    public Map<String, Object> getSettings() {
        return settings;
    }

    /** @return the values passed to the round or race, as after {@code /chatgames <game>} */
    @Nonnull
    public String[] getArgs() {
        return args.clone();
    }

    /**
     * @param globals global_games.yml, for a global step's points (it plays even if the
     *                game isn't enabled there)
     * @return how to start this step's game
     */
    @Nonnull
    public StartOptions toOptions(@Nonnull GlobalGameConfig globals) {
        Map<String, Object> overrides = new LinkedHashMap<>(settings);
        if (winners > 0 && !type.isRace()) overrides.put("winners", winners);
        if (timeSeconds > 0) overrides.put(type.isRace() ? StartOptions.RACE_TIME : StartOptions.GAME_TIME, timeSeconds);
        if (!global && !type.isGlobalOnly()) return StartOptions.of(null, overrides);

        GlobalGameConfig.Entry entry = globals.find(type);
        if (timeSeconds > 0) entry = entry.withDuration(timeSeconds);
        return StartOptions.of(entry, overrides);
    }
}
