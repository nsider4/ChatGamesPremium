package org.nsider.chatgames.globalgames;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The Count game's rules: the server counts from {@code start} to {@code target} in steps
 * of {@code step} (negative to count down), one number per message.
 *
 * <p>A number that is ahead of the next one, behind it, the one just said again, or said
 * by the player who said the last one (with {@code no-double-count}) is a mistake. Chat
 * arrives on async threads, so {@link #submit} is synchronized: two players typing the
 * same number at once can't both count it.</p>
 */
public final class Counter {

    public enum Result {
        /** The right number: it counted. */
        COUNTED,
        /** The right number, and the last one. */
        FINISHED,
        /** Further than the next number. */
        AHEAD,
        /** Before the next number (but not the one just said). */
        BEHIND,
        /** The number just said, again. */
        REPEATED,
        /** The right number, but the same player said the last one. */
        TWICE;

        public boolean isMistake() {
            return this != COUNTED && this != FINISHED;
        }
    }

    private static final Pattern NUMBER = Pattern.compile("[+-]?\\d{1,9}");

    private final int start;
    private final int step;
    private final int goal;
    private final boolean noDoubleCount;

    private int next;
    private int counted;
    @Nullable private UUID last;

    /**
     * @param target the last number; it is lowered (or raised, counting down) to the last
     *               number the step lands on
     */
    public Counter(int start, int target, int step, boolean noDoubleCount) {
        this.start = start;
        this.step = step == 0 ? 1 : step;
        int span = (target - start) / this.step;
        this.goal = Math.max(1, span + 1);
        this.noDoubleCount = noDoubleCount;
        this.next = start;
    }

    /**
     * @return the number a chat message says, or null if it says anything else (which is
     *         just chat, not a try)
     */
    @Nullable
    public static Integer parse(@Nonnull String message) {
        String trimmed = message.trim();
        if (!NUMBER.matcher(trimmed).matches()) return null;
        return Integer.valueOf(trimmed.startsWith("+") ? trimmed.substring(1) : trimmed);
    }

    /** A player says a number. */
    @Nonnull
    public synchronized Result submit(@Nonnull UUID player, int number) {
        if (number == next) {
            if (noDoubleCount && player.equals(last)) return Result.TWICE;
            counted++;
            last = player;
            next += step;
            return counted >= goal ? Result.FINISHED : Result.COUNTED;
        }
        if (counted > 0 && number == next - step) return Result.REPEATED;
        // Positive when the number is further along the count than the next one
        long distance = ((long) number - next) * Integer.signum(step);
        return distance > 0 ? Result.AHEAD : Result.BEHIND;
    }

    /** Starts the count again from the start. */
    public synchronized void reset() {
        next = start;
        counted = 0;
        last = null;
    }

    /** @return the number to say next */
    public synchronized int getNext() {
        return next;
    }

    /** @return how many numbers have been counted since the start (or the last reset) */
    public synchronized int getCounted() {
        return counted;
    }

    /** @return how many numbers the count has */
    public int getGoal() {
        return goal;
    }

    public int getStart() {
        return start;
    }

    public int getStep() {
        return step;
    }

    /** @return the last number of the count */
    public int getTarget() {
        return start + (goal - 1) * step;
    }
}
