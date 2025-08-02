package org.nsider.chatgames.database.queries;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.Errors;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class ValueQueries {

    private static ChatGames plugin;
    private static String tableName;

    /**
     * Initializes the ValueQueries class.
     *
     * @param instance The ChatGames plugin instance.
     */
    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = plugin.getConfig().getString("table_name", "chatgames");
    }

    /**
     * Retrieves the database connection from the plugin's data handler.
     *
     * @return The database connection.
     * @throws SQLException If there is an error obtaining the connection.
     */
    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    /**
     * Retrieves the points for a given player.
     *
     * @param paramPlayer The player whose points are to be retrieved. Must not be null.
     * @return The points of the player, or 0 if the player does not exist or an error occurs.
     */
    public static int getPlayerPoints(@Nonnull OfflinePlayer paramPlayer) {
        if (paramPlayer == null) {
            plugin.getLogger().warning("Player is null in getPlayerPoints. Unable to retrieve points.");
            return 0;
        }

        ManagementQueries.createPlayerIfNotExists(paramPlayer);

        String sql = "SELECT POINTS FROM " + tableName + " WHERE UUID = ?";
        int points = 0;

        try (Connection connection = getConnection();
             ResultSet resultSet = QueryUtils.executeQuery(connection, sql, paramPlayer.getUniqueId().toString())) {

            if (resultSet.next()) {
                points = resultSet.getInt("POINTS");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player points", e);
        }

        return points;
    }

    /**
     * Retrieves the fastest time of a player.
     * @param player The player whose fastest time is to be retrieved.
     * @return The fastest time of the player.
     */
    public static double getPlayerFastest(Player player) {
        ManagementQueries.createPlayerIfNotExists(player);

        double fastest = 0;
        String sql = "SELECT fastest FROM " + tableName + " WHERE UUID = ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, player.getUniqueId().toString())) {
            if (rs.next()) {
                fastest = rs.getDouble("fastest");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        }
        return fastest;
    }

    /**
     * Retrieves the toggle state for a player.
     * @param player The player whose toggle state is to be retrieved.
     * @return The toggle state of the player.
     */
    public static boolean getPlayerToggle(Player player) {
        boolean toggle = true;
        String sql = "SELECT toggled FROM " + tableName + " WHERE UUID = ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, player.getUniqueId().toString())) {
            if (rs.next()) {
                toggle = rs.getBoolean("toggled");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
            throw new RuntimeException("Error getting player toggle", e);
        }
        return toggle;
    }

    /**
     * Retrieves the sound toggle state for a player.
     * @param player The player whose sound toggle state is to be retrieved.
     * @return The sound toggle state of the player.
     */
    public static boolean getPlayerToggleSound(Player player) {
        boolean toggle = true;
        String sql = "SELECT sound FROM " + tableName + " WHERE UUID = ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, player.getUniqueId().toString())) {
            if (rs.next()) {
                toggle = rs.getBoolean("sound");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        }
        return toggle;
    }

    /**
     * Retrieves the total points from all players.
     * @return The total points of all players.
     */
    public static int getTotalPoints() {
        String sql = "SELECT SUM(POINTS) FROM " + tableName;
        int totalPoints = 0;

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql)) {
            if (rs.next()) {
                totalPoints = rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        }
        return totalPoints;
    }

    /**
     * Retrieves the player at a specific position based on points.
     * @param spot The position to check.
     * @return The player's name at that position.
     */
    public static String getPlayerAtPosition(int spot) {
        String playerName = "";
        String sql = "SELECT UUID FROM " + tableName + " ORDER BY POINTS DESC LIMIT 1 OFFSET ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, spot - 1)) {

            if (rs.next()) {
                String uuidString = rs.getString("UUID");
                UUID uuid = UUID.fromString(uuidString);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player != null) {
                    playerName = player.getName();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting player at specific position", e);
        }
        return playerName;
    }

    /**
     * Retrieves the points at a specific position based on points.
     * @param spot The position to check.
     * @return The points at that position.
     */
    public static String getPointsAtPosition(int spot) {
        String points = "";
        String sql = "SELECT POINTS FROM " + tableName + " ORDER BY POINTS DESC LIMIT 1 OFFSET ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, spot - 1)) {

            if (rs.next()) {
                points = String.valueOf(rs.getInt("POINTS"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting points of player at specific position", e);
        }
        return points;
    }

    /**
     * Retrieves the player at a specific position based on fastest time.
     * @param spot The position to check.
     * @return The player's name at that position.
     */
    public static String getPlayerAtPositionFastest(int spot) {
        String playerName = "";
        String sql = "SELECT UUID FROM " + tableName + " ORDER BY fastest ASC LIMIT 1 OFFSET ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, spot - 1)) {

            if (rs.next()) {
                String uuidString = rs.getString("UUID");
                UUID uuid = UUID.fromString(uuidString);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player != null) {
                    playerName = player.getName();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting player at specific position", e);
        }
        return playerName;
    }

    /**
     * Retrieves the fastest time at a specific position.
     * @param spot The position to check.
     * @return The fastest time at that position.
     */
    public static double getFastestAtPosition(int spot) {
        double fastest = 0;
        String sql = "SELECT fastest FROM " + tableName + " ORDER BY fastest ASC LIMIT 1 OFFSET ?";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, spot - 1)) {

            if (rs.next()) {
                fastest = rs.getDouble("fastest");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting fastest of player at specific position", e);
        }
        return fastest;
    }

    /**
     * Retrieves the position of a player based on their points.
     * @param player The player whose position is to be retrieved.
     * @return The player's position based on points.
     */
    public static int getPlayerPointsPosition(OfflinePlayer player) {
        int position = -1;
        String sql = "SELECT COUNT(*) AS position FROM " + tableName + " WHERE POINTS > (SELECT POINTS FROM " + tableName + " WHERE UUID = ?)";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, player.getUniqueId().toString())) {

            if (rs.next()) {
                position = rs.getInt("position") + 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player's points position", e);
        }
        return position;
    }

    /**
     * Retrieves the position of a player based on their fastest time.
     * @param player The player whose position is to be retrieved.
     * @return The player's position based on fastest time.
     */
    public static int getPlayerFastestPosition(OfflinePlayer player) {
        int position = -1;
        String sql = "SELECT COUNT(*) AS position FROM " + tableName + " WHERE fastest < (SELECT fastest FROM " + tableName + " WHERE UUID = ?)";

        try (Connection connection = getConnection();
             ResultSet rs = QueryUtils.executeQuery(connection, sql, player.getUniqueId().toString())) {

            if (rs.next()) {
                position = rs.getInt("position") + 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player's fastest position", e);
        }
        return position;
    }
}
