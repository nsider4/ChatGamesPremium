package org.nsider.chatgames.progression;

import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.time.LocalDate;

/**
 * Streaks, kept in a player's stats as {@code streak.<type>.current}, {@code .longest},
 * {@code .last} (the epoch day it last grew) and {@code .claimed} (the last streak day
 * whose reward was claimed). Each call holds the player's lock throughout.
 * Holds no state of its own.
 */
public final class StreakTracker {

    private StreakTracker() {}

    /**
     * Counts today for a streak. Only the first call of a day does anything; missing a
     * day starts the streak again from 1, and rewards not claimed by then are lost.
     *
     * @return the streak's new length, or 0 if today was already counted
     */
    public static int advance(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, @Nonnull LocalDate today) {
        long day = today.toEpochDay();
        synchronized (player) {
            long last = player.getStat(key(type, "last"));
            if (last == day) return 0;
            long current = last == day - 1 ? player.getStat(key(type, "current")) + 1 : 1;
            if (current == 1) player.setStat(key(type, "claimed"), 0);
            player.setStat(key(type, "current"), current);
            player.setStat(key(type, "last"), day);
            if (current > player.getStat(key(type, "longest"))) player.setStat(key(type, "longest"), current);
            return (int) current;
        }
    }

    /**
     * @return the streak's length today: 0 once a whole day was missed
     */
    public static int current(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, @Nonnull LocalDate today) {
        synchronized (player) {
            return alive(player, type, today) ? (int) player.getStat(key(type, "current")) : 0;
        }
    }

    public static int longest(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type) {
        return (int) player.getStat(key(type, "longest"));
    }

    /**
     * @return true if the streak already grew today
     */
    public static boolean countedToday(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, @Nonnull LocalDate today) {
        return player.getStat(key(type, "last")) == today.toEpochDay();
    }

    /**
     * @return the number of streak days whose rewards can be claimed
     */
    public static int unclaimed(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, @Nonnull LocalDate today) {
        synchronized (player) {
            if (!alive(player, type, today)) return 0;
            return (int) Math.max(0, player.getStat(key(type, "current")) - player.getStat(key(type, "claimed")));
        }
    }

    /**
     * @return the last streak day whose reward was claimed, 0 if none (or the streak died)
     */
    public static int claimed(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, @Nonnull LocalDate today) {
        synchronized (player) {
            return alive(player, type, today) ? (int) player.getStat(key(type, "claimed")) : 0;
        }
    }

    /**
     * Marks every unclaimed day of a living streak as claimed.
     *
     * @return the first and last streak day claimed now, or null if there was nothing
     */
    @Nullable
    public static int[] claim(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, @Nonnull LocalDate today) {
        synchronized (player) {
            if (!alive(player, type, today)) return null;
            long current = player.getStat(key(type, "current"));
            long claimed = player.getStat(key(type, "claimed"));
            if (claimed >= current) return null;
            player.setStat(key(type, "claimed"), current);
            return new int[]{(int) claimed + 1, (int) current};
        }
    }

    /**
     * Sets a streak by hand, as if it grew today. The days set don't pay rewards; the
     * next day counted does.
     */
    public static void set(@Nonnull ChatGamesPlayer player, @Nonnull StreakType type, int days, @Nonnull LocalDate today) {
        synchronized (player) {
            player.setStat(key(type, "current"), days);
            player.setStat(key(type, "claimed"), days);
            player.setStat(key(type, "last"), days > 0 ? today.toEpochDay() : 0);
            if (days > player.getStat(key(type, "longest"))) player.setStat(key(type, "longest"), days);
        }
    }

    /** The streak is alive if it grew today or yesterday. */
    private static boolean alive(ChatGamesPlayer player, StreakType type, LocalDate today) {
        return player.getStat(key(type, "last")) >= today.toEpochDay() - 1;
    }

    static String key(StreakType type, String field) {
        return "streak." + type.getKey() + "." + field;
    }
}
