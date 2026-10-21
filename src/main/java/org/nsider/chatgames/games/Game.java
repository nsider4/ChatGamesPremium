package org.nsider.chatgames.games;

import lombok.Getter;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.round.GameSettings;
import org.nsider.chatgames.games.round.Round;
import org.nsider.chatgames.games.utils.PlayerResult;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One running chat game: what it is, its answer, its clock and its winners.
 *
 * <p>Answers arrive on many threads at once (chat is async everywhere, and Folia
 * also runs regions in parallel), so places are handed out by
 * {@link #claimPlace(UUID)}, which is atomic. Two players answering in the same
 * millisecond get places 1 and 2, never both 1, and never more than
 * {@link #getMaxWinners()} places.</p>
 */
@Getter
public class Game {

    /**
     * The type of the game, defined by {@link GameType}.
     */
    private final GameType gameType;

    /**
     * The answer shown when the game ends.
     */
    private final String answer;

    /**
     * How many players can win this round.
     */
    private final int maxWinners;

    /** The question being played; null only for games built directly in tests. */
    @Nullable
    private final Round round;
    @Nullable
    private final GameSettings settings;

    private final long startedAtNanos = System.nanoTime();

    @Getter(lombok.AccessLevel.NONE)
    private final Map<UUID, PlayerResult> results = new ConcurrentHashMap<>();

    @Getter(lombok.AccessLevel.NONE)
    private final AtomicBoolean finished = new AtomicBoolean();

    /**
     * Constructs a new {@code Game} instance with the specified type and answer.
     *
     * @param gameType   The type of the game.
     * @param answer     The initial answer for the game.
     * @param maxWinners How many players can win; at least 1.
     */
    public Game(GameType gameType, String answer, int maxWinners) {
        this(gameType, answer, maxWinners, null, null);
    }

    /**
     * A game for a generated round, played with the given settings.
     */
    public Game(@Nonnull Round round, @Nonnull GameSettings settings) {
        this(round.getType(), round.getMainAnswer(), settings.getWinners(), round, settings);
    }

    private Game(GameType gameType, String answer, int maxWinners, Round round, GameSettings settings) {
        this.gameType = gameType;
        this.answer = answer;
        this.maxWinners = Math.max(1, maxWinners);
        this.round = round;
        this.settings = settings;
    }

    /**
     * Retrieves the configuration name corresponding to the game type.
     *
     * @return the section name of this game in the config files
     */
    public String getGameConfigName() {
        return gameType.getConfigKey();
    }

    /**
     * @return seconds since the game started, as measured when called
     */
    public double getElapsedSeconds() {
        return (System.nanoTime() - startedAtNanos) / 1_000_000_000.0;
    }

    public long getElapsedMillis() {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    /**
     * Gives the player the next free place, timed from the start of the game.
     *
     * @return the player's result, or null if they already placed, all places
     *         are taken, or the game has finished
     */
    @Nullable
    public synchronized PlayerResult claimPlace(UUID uuid) {
        if (finished.get() || results.containsKey(uuid) || results.size() >= maxWinners) {
            return null;
        }
        PlayerResult result = new PlayerResult(uuid, getElapsedSeconds(), results.size() + 1);
        results.put(uuid, result);
        return result;
    }

    /**
     * @return true once every place has been claimed
     */
    public boolean isFull() {
        return results.size() >= maxWinners;
    }

    public boolean hasResult(UUID uuid) {
        return results.containsKey(uuid);
    }

    @Nullable
    public PlayerResult getResult(UUID uuid) {
        return results.get(uuid);
    }

    public boolean hasWinners() {
        return !results.isEmpty();
    }

    /**
     * @return a snapshot of the results ordered by place (1st first)
     */
    public List<PlayerResult> getResultsByPlace() {
        List<PlayerResult> sorted = new ArrayList<>(results.values());
        sorted.sort(Comparator.comparingInt(PlayerResult::getPlace));
        return sorted;
    }

    /**
     * Marks the game finished. Both the last winner and the time limit try to end
     * a game, and they can race; only the first caller gets true and should end it.
     *
     * @return true if this call finished the game
     */
    public synchronized boolean finish() {
        return finished.compareAndSet(false, true);
    }

    public boolean isFinished() {
        return finished.get();
    }
}
