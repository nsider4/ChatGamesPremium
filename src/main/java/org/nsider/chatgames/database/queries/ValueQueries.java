package org.nsider.chatgames.database.queries;

import org.bukkit.OfflinePlayer;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.Errors;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Leaderboard queries against the main ChatGames table. Per-player values come from
 * the player cache instead. These block on the database, so call them off the main thread.
 */
public class ValueQueries {

    private static ChatGames plugin;
    private static String tableName;

    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = ConfigValue.getString(ConfigPaths.TABLE_NAME);
    }

    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    public static int getTotalPoints() {
        String sql = "SELECT SUM(POINTS) FROM " + tableName;
        try (Connection conn = getConnection()) {
            return QueryUtils.query(conn, sql, rs -> rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        }
        return 0;
    }

    public static String getPlayerAtPosition(int position) {
        String sql = "SELECT UUID FROM " + tableName + " ORDER BY POINTS DESC LIMIT 1 OFFSET ?";
        try (Connection conn = getConnection()) {
            String uuid = QueryUtils.query(conn, sql, rs -> rs.next() ? rs.getString("UUID") : null, position - 1);
            if (uuid != null) {
                String name = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid)).getName();
                return name == null ? "" : name;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error retrieving player at position", e);
        }
        return "";
    }

    public static int getPointsAtPosition(int position) {
        String sql = "SELECT POINTS FROM " + tableName + " ORDER BY POINTS DESC LIMIT 1 OFFSET ?";
        try (Connection conn = getConnection()) {
            return QueryUtils.query(conn, sql, rs -> rs.next() ? rs.getInt("POINTS") : 0, position - 1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error retrieving points at position", e);
        }
        return 0;
    }

    public static int getPlayerPointsPosition(OfflinePlayer player) {
        String sql = "SELECT COUNT(*) AS pos FROM " + tableName + " WHERE POINTS > (SELECT POINTS FROM "
                + tableName + " WHERE UUID = ?)";
        try (Connection conn = getConnection()) {
            return QueryUtils.query(conn, sql, rs -> rs.next() ? rs.getInt("pos") + 1 : -1,
                    player.getUniqueId().toString());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error determining player rank by points", e);
        }
        return -1;
    }
}
