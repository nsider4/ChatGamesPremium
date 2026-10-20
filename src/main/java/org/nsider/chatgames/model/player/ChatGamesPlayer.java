package org.nsider.chatgames.model.player;

import org.nsider.chatgames.enums.GameType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
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

    // Daily challenge v1 fields, kept until the challenge system is rebuilt in step 9
    private int winsToday;
    private boolean rewardClaimed;
    private LocalDate lastUpdated = LocalDate.now();
    private int challengeStreak;
    private int dailyStreak;
    private int globalStreak;
    private int weeklyWins;

    private final Map<String, Long> stats = new HashMap<>();
    private final Set<String> dirtyStats = new HashSet<>();

    private final EnumMap<GameType, Double> fastest = new EnumMap<>(GameType.class);
    private final Set<GameType> dirtyFastest = new HashSet<>();

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
     * @return the best time across every game in seconds, or 0 if never won
     */
    public synchronized double getBestOverall() {
        double best = 0;
        for (double time : fastest.values()) {
            if (best == 0 || time < best) best = time;
        }
        return best;
    }

    public synchronized Map<GameType, Double> getFastestTimes() {
        return Collections.unmodifiableMap(new EnumMap<>(fastest));
    }

    /* ------------------------------------------ daily challenge v1 ------------------------------ */

    public synchronized int getWinsToday() { return winsToday; }
    public synchronized void setWinsToday(int winsToday) { this.winsToday = winsToday; }
    public synchronized boolean isRewardClaimed() { return rewardClaimed; }
    public synchronized void setRewardClaimed(boolean rewardClaimed) { this.rewardClaimed = rewardClaimed; }
    public synchronized LocalDate getLastUpdated() { return lastUpdated; }
    public synchronized void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }
    public synchronized int getChallengeStreak() { return challengeStreak; }
    public synchronized void setChallengeStreak(int challengeStreak) { this.challengeStreak = challengeStreak; }
    public synchronized int getDailyStreak() { return dailyStreak; }
    public synchronized void setDailyStreak(int dailyStreak) { this.dailyStreak = dailyStreak; }
    public synchronized int getGlobalStreak() { return globalStreak; }
    public synchronized void setGlobalStreak(int globalStreak) { this.globalStreak = globalStreak; }
    public synchronized int getWeeklyWins() { return weeklyWins; }
    public synchronized void setWeeklyWins(int weeklyWins) { this.weeklyWins = weeklyWins; }

    public synchronized boolean isDailyComplete() {
        return winsToday >= 3;
    }

    public synchronized boolean canClaimReward() {
        return isDailyComplete() && !rewardClaimed;
    }

    public synchronized void markClaimed() {
        if (canClaimReward()) {
            rewardClaimed = true;
        }
    }

    public synchronized void resetDailyIfNeeded() {
        if (!LocalDate.now().equals(lastUpdated)) {
            winsToday = 0;
            rewardClaimed = false;
            lastUpdated = LocalDate.now();
        }
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

    /**
     * Copies everything that needs saving and clears the dirty marks.
     * If the save fails, hand the snapshot back to {@link #restoreDirty}.
     */
    synchronized Snapshot snapshot() {
        Map<String, Long> changedStats = new HashMap<>();
        for (String name : dirtyStats) changedStats.put(name, stats.get(name));
        Map<GameType, Double> changedTimes = new EnumMap<>(GameType.class);
        for (GameType game : dirtyFastest) changedTimes.put(game, fastest.get(game));
        dirtyStats.clear();
        dirtyFastest.clear();

        return new Snapshot(uuid, points, toggled, soundEnabled,
                winsToday, rewardClaimed, lastUpdated, challengeStreak, dailyStreak, globalStreak, weeklyWins,
                changedStats, changedTimes);
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
        final int winsToday;
        final boolean rewardClaimed;
        final LocalDate lastUpdated;
        final int challengeStreak;
        final int dailyStreak;
        final int globalStreak;
        final int weeklyWins;
        final Map<String, Long> stats;
        final Map<GameType, Double> fastest;

        Snapshot(UUID uuid, int points, boolean toggled, boolean soundEnabled, int winsToday, boolean rewardClaimed,
                 LocalDate lastUpdated, int challengeStreak, int dailyStreak, int globalStreak, int weeklyWins,
                 Map<String, Long> stats, Map<GameType, Double> fastest) {
            this.uuid = uuid;
            this.points = points;
            this.toggled = toggled;
            this.soundEnabled = soundEnabled;
            this.winsToday = winsToday;
            this.rewardClaimed = rewardClaimed;
            this.lastUpdated = lastUpdated;
            this.challengeStreak = challengeStreak;
            this.dailyStreak = dailyStreak;
            this.globalStreak = globalStreak;
            this.weeklyWins = weeklyWins;
            this.stats = stats;
            this.fastest = fastest;
        }
    }
}
