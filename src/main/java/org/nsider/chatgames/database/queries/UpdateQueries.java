package org.nsider.chatgames.database.queries;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * A class to manage database operations related to updating player data.
 * Handles updating player data in the database.
 */
public class UpdateQueries {

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
     * Updates the points for a given player asynchronously.
     *
     * @param player The player whose points are to be updated.
     * @param points The new points value to be set.
     */
    public static void updatePlayerPoints(OfflinePlayer player, int points) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE " + tableName + " SET POINTS = ? WHERE UUID = ?";

            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    QueryUtils.executeUpdate(ps, points, player.getUniqueId().toString());
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player points", e);
            }
        });
    }

    /**
     * Updates the fastest time for a given player asynchronously.
     *
     * @param player The player whose fastest time is to be updated.
     * @param fastest The new fastest time value to be set.
     */
    public static void updatePlayerFastest(Player player, double fastest) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE " + tableName + " SET fastest = ? WHERE UUID = ?";

            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    QueryUtils.executeUpdate(ps, fastest, player.getUniqueId().toString());
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player fastest time", e);
            }
        });
    }

    /**
     * Updates the toggle state for a given player asynchronously.
     *
     * @param player The player whose toggle state is to be updated.
     * @param toggle The new toggle state value to be set.
     */
    public static void updatePlayerToggle(Player player, boolean toggle) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE " + tableName + " SET toggled = ? WHERE UUID = ?";

            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    QueryUtils.executeUpdate(ps, toggle, player.getUniqueId().toString());
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player toggle", e);
            }
        });
    }

    /**
     * Updates the sound toggle state for a given player asynchronously.
     *
     * @param player The player whose sound toggle state is to be updated.
     * @param toggle The new sound toggle state value to be set.
     */
    public static void updatePlayerToggleSound(Player player, boolean toggle) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE " + tableName + " SET sound = ? WHERE UUID = ?";

            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    QueryUtils.executeUpdate(ps, toggle, player.getUniqueId().toString());
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player toggle sound", e);
            }
        });
    }
}
