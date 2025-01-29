package org.nsider.chatgames.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.database.queries.UpdateQueries;
import org.nsider.chatgames.database.queries.ValueQueries;
import org.nsider.chatgames.enums.TopType;

/**
 * Manages player statistics and top rankings in the ChatGames plugin.
 *
 * <p>This class provides functionality to set, retrieve, and update player points,
 * fastest times, and rankings. It acts as a bridge between the plugin logic and
 * database query modules.</p>
 */
public class TopManager {

    /**
     * Sets the points for a specific player.
     *
     * @param player The {@link OfflinePlayer} whose points to update.
     * @param points The new points value to set.
     */
    public static void setPoints(OfflinePlayer player, int points) {
        UpdateQueries.updatePlayerPoints(player, points);
    }

    /**
     * Retrieves the current points of a specific player.
     *
     * @param player The {@link OfflinePlayer} whose points to retrieve.
     * @return The number of points the player currently has.
     */
    public static int getPoints(OfflinePlayer player) {
        return ValueQueries.getPlayerPoints(player);
    }

    /**
     * Adds a specific amount of points to a player's current points.
     *
     * @param player The {@link OfflinePlayer} whose points to increase.
     * @param amount The amount of points to add.
     */
    public static void addPoint(OfflinePlayer player, int amount) {
        int points = getPoints(player);
        int newPoints = points + amount;
        setPoints(player, newPoints);
    }

    /**
     * Removes a specific amount of points from a player's current points.
     *
     * @param player The {@link OfflinePlayer} whose points to decrease.
     * @param amount The amount of points to remove.
     *               If the resulting points are negative, they are set to zero.
     */
    public static void removePoints(OfflinePlayer player, int amount) {
        int points = getPoints(player);
        int newPoints = points - amount;
        if (newPoints < 0) {
            newPoints = 0;
        }
        setPoints(player, newPoints);
    }

    /**
     * Sets the fastest time for a specific player.
     *
     * @param player The {@link Player} whose fastest time to update.
     * @param amount The new fastest time value to set.
     */
    public static void setFastestTime(Player player, double amount) {
        UpdateQueries.updatePlayerFastest(player, amount);
    }

    /**
     * Retrieves the fastest time of a specific player.
     *
     * @param player The {@link Player} whose fastest time to retrieve.
     * @return The fastest time of the player.
     */
    public static double getFastestTime(Player player) {
        return ValueQueries.getPlayerFastest(player);
    }

    /**
     * Determines if a new time is faster than the player's current fastest time.
     *
     * @param player  The {@link Player} to compare the time for.
     * @param newTime The new time to compare against the player's current fastest time.
     * @return {@code true} if the new time is faster; {@code false} otherwise.
     */
    public static boolean isFasterThan(Player player, double newTime) {
        double fastest = getFastestTime(player);
        return fastest == 0.0 || fastest > newTime;
    }

    /**
     * Retrieves the name of the player at a specific position in the leaderboard.
     *
     * @param type The {@link TopType} (POINTS or TIME) to query.
     * @param spot The ranking position to retrieve.
     * @return The name of the player at the specified position, or "Unknown" if not found.
     */
    public static String getSpecific(TopType type, int spot) {
        switch (type) {
            case POINTS:
                return ValueQueries.getPlayerAtPosition(spot);
            case TIME:
                return ValueQueries.getPlayerAtPositionFastest(spot);
            default:
                return "Unknown";
        }
    }

    /**
     * Retrieves the value (points or fastest time) at a specific position in the leaderboard.
     *
     * @param type The {@link TopType} (POINTS or TIME) to query.
     * @param spot The ranking position to retrieve.
     * @return The value at the specified position, or "Unknown" if not found.
     */
    public static String getSpecificValue(TopType type, int spot) {
        switch (type) {
            case POINTS:
                return ValueQueries.getPointsAtPosition(spot);
            case TIME:
                return ValueQueries.getPlayerAtPositionFastest(spot);
            default:
                return "Unknown";
        }
    }
}