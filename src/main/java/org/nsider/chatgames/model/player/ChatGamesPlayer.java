package org.nsider.chatgames.model.player;

import org.nsider.chatgames.api.GameType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything ChatGames stores about one player, kept in memory while they are online.
 *
 * <p>Wins arrive on chat threads (and on region threads under Folia) while saves
 * run on the player-data thread, so every method is synchronized on this object.
 * Changes are tracked as dirty and written by the next save; nothing here touches
 * the database or the server.</p>
 */
public final class ChatGamesPlayer {

    private final UUID uuid;

    private int points;
    private boolean toggled = true;
    private boolean soundEnabled = true;

    private final Map<String, Long> stats = new HashMap<>();
    private final Set<String> dirtyStats = new HashSet<>();

    private final Map<GameType, Double> fastest = new HashMap<>();
    private final Set<GameType> dirtyFastest = new HashSet<>();
    // The one overall best a 1.9.9 database kept; read-only, so never saved
    private double legacyBest;

    public ChatGamesPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    /* ------------------------------------------ points ------------------------------------------ */

    public synchronized int getPoints() {
        return points;
    }

    /**
     * @return the new total; never below zero
     */
    public synchronized int addPoints(int amount) {
        points = Math.max(0, points + amount);
        return points;
    }

    public synchronized void setPoints(int points) {
        this.points = Math.max(0, points);
    }

    /* ------------------------------------------ toggles ----------------------------------------- */

    public synchronized boolean isToggled() {
        return toggled;
    }

    public synchronized void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public synchronized boolean isSoundEnabled() {
        return soundEnabled;
    }

    public synchronized void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    /* ------------------------------------------ stats ------------------------------------------- */

    /**
     * @param name one of the {@link Stat} names
     * @return the counter's value, 0 if never set
     */
    public synchronized long getStat(String name) {
        Long value = stats.get(name);
        return value == null ? 0 : value;
    }

    /**
     * Adds to a counter.
     *
     * @return the new value
     */
    public synchronized long incrementStat(String name, long amount) {
        long value = getStat(name) + amount;
        stats.put(name, value);
        dirtyStats.add(name);
        return value;
    }

    public synchronized void setStat(String name, long value) {
        stats.put(name, value);
        dirtyStats.add(name);
    }

    /**
     * @return a copy of the names of every stat starting with the prefix
     */
    public synchronized Set<String> getStatNames(String prefix) {
        Set<String> names = new HashSet<>();
        for (String name : stats.keySet()) {
            if (name.startsWith(prefix)) names.add(name);
        }
        return names;
    }

    /* ------------------------------------------ fastest times ----------------------------------- */

    /**
     * Records a time if it beats the player's best for that game.
     *
     * @return true if it was a new personal best
     */
    public synchronized boolean recordTime(GameType game, double seconds) {
        Double best = fastest.get(game);
        if (best != null && best <= seconds) {
            return false;
        }
        fastest.put(game, seconds);
        dirtyFastest.add(game);
        return true;
    }

    /**
     * @return the best time for a game in seconds, or 0 if never won
     */
    public synchronized double getFastest(GameType game) {
        Double best = fastest.get(game);
        return best == null ? 0 : best;
    }

    /**
     * @return the best time across every chat game (races aren't typing) and the 1.9.9 record,
     *         in seconds, or 0 if never won
     */
    public synchronized double getBestOverall() {
        double best = legacyBest;
        for (Map.Entry<GameType, Double> entry : fastest.entrySet()) {
            double time = entry.getValue();
            if (!entry.getKey().isRace() && (best == 0 || time < best)) best = time;
        }
        return best;
    }

    public synchronized Map<GameType, Double> getFastestTimes() {
        return Collections.unmodifiableMap(new HashMap<>(fastest));
    }

    /* ------------------------------------------ persistence ------------------------------------- */

    /**
     * Fills in values read from the database without marking them as changed.
     */
    synchronized void loadStat(String name, long value) {
        stats.put(name, value);
    }

    synchronized void loadFastest(GameType game, double seconds) {
        fastest.put(game, seconds);
    }

    synchronized void loadLegacyBest(double seconds) {
        legacyBest = seconds;
    }

    /**
     * Copies everything that needs saving and clears the dirty marks.
     * If the save fails, hand the snapshot back to {@link #restoreDirty}.
     */
    synchronized Snapshot snapshot() {
        Map<String, Long> changedStats = new HashMap<>();
        for (String name : dirtyStats) changedStats.put(name, stats.get(name));
        Map<GameType, Double> changedTimes = new HashMap<>();
        for (GameType game : dirtyFastest) changedTimes.put(game, fastest.get(game));
        dirtyStats.clear();
        dirtyFastest.clear();

        return new Snapshot(uuid, points, toggled, soundEnabled, changedStats, changedTimes);
    }

    /**
     * Marks a failed save's values as changed again so the next save retries them.
     */
    synchronized void restoreDirty(Snapshot failed) {
        dirtyStats.addAll(failed.stats.keySet());
        dirtyFastest.addAll(failed.fastest.keySet());
    }

    /**
     * An unchanging copy of a player's data, safe to write from another thread.
     * Stats and fastest times only include what changed since the last save.
     */
    static final class Snapshot {
        final UUID uuid;
        final int points;
        final boolean toggled;
        final boolean soundEnabled;
        final Map<String, Long> stats;
        final Map<GameType, Double> fastest;

        Snapshot(UUID uuid, int points, boolean toggled, boolean soundEnabled,
                 Map<String, Long> stats, Map<GameType, Double> fastest) {
            this.uuid = uuid;
            this.points = points;
            this.toggled = toggled;
            this.soundEnabled = soundEnabled;
            this.stats = stats;
            this.fastest = fastest;
        }
    }
}
