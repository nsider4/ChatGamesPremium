package org.nsider.chatgames.managers;

import org.bukkit.entity.Player;
import org.nsider.chatgames.utils.management.TimeUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns for players to prevent repeated actions within a short time frame.
 */
public final class CooldownManager {

    // By UUID, and expired entries are dropped, so players who left aren't kept; commands run on region threads under Folia
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 5000;

    private CooldownManager() {}

    /**
     * Checks if a player is off cooldown and updates their cooldown if applicable.
     *
     * @param player The {@link Player} to check.
     * @return {@code true} if the cooldown has expired and the player can perform the action;
     *         {@code false} if the player is still on cooldown.
     */
    public static boolean checkCooldown(@Nonnull Player player) {
        long remainingCooldown = getRemainingCooldown(player);
        if (remainingCooldown <= 0) {
            long now = System.currentTimeMillis();
            cooldowns.values().removeIf(last -> now - last >= COOLDOWN_MILLIS);
            cooldowns.put(player.getUniqueId(), now);
            return true;
        }
        return false;
    }

    /**
     * Retrieves the remaining cooldown time for a player, in seconds.
     *
     * @param player The {@link Player} to check.
     * @return The remaining cooldown time, rounded to one decimal place, or 0 if no cooldown is active.
     */
    public static double getTimeLeft(@Nonnull Player player) {
        long remainingCooldown = getRemainingCooldown(player);
        return TimeUtils.roundTo1Decimal(remainingCooldown / 1000.0);
    }

    /**
     * Calculates the remaining cooldown time in milliseconds for a player.
     *
     * @param player The {@link Player} to check.
     * @return The remaining cooldown time in milliseconds, or 0 if no cooldown is active.
     */
    private static long getRemainingCooldown(@Nonnull Player player) {
        Long lastExecutionTime = cooldowns.get(player.getUniqueId());
        if (lastExecutionTime != null) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastExecutionTime;
            return Math.max(COOLDOWN_MILLIS - elapsedTime, 0);
        }
        return 0;
    }
}
