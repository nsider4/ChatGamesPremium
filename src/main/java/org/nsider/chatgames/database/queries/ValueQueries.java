package org.nsider.chatgames.database.queries;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.Errors;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Leaderboard queries against the main ChatGames table. Per-player values come from
 * the player cache instead. These block on the database, so call them off the main thread.
 */
public final class ValueQueries {

    private static ChatGames plugin;
    private static String tableName;

    private ValueQueries() {}

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
}
