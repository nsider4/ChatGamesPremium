package org.nsider.chatgames.database.queries;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.model.top.FastestData;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;
import org.nsider.chatgames.utils.management.TaskUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class FastestTimeQueries {

    private static ChatGames plugin;
    @Getter private static String SQLiteCreateFastestTimesTable;
    private static String tableName;

    /**
     * Initializes the FastestTimeQueries class.
     *
     * @param instance The ChatGames plugin instance.
     */
    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = ConfigValue.getString(ConfigPaths.FASTEST_TIMES_TABLE);
        SQLiteCreateFastestTimesTable = generateCreateTableStatement();
    }

    /**
     * Generates the SQL statement for creating the fastest times table.
     *
     * @return The SQL create table statement.
     */
    private static String generateCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "UUID VARCHAR(36) NOT NULL," +
                "game_name VARCHAR(50) NOT NULL," +
                "fastest_time DOUBLE NOT NULL," +
                "PRIMARY KEY (UUID, game_name)" +
                ");";
    }

    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    /**
     * Updates the fastest time for a specific player and game asynchronously.
     *
     * @param player    The player to update.
     * @param gameName  The game name.
     * @param time      The new time to attempt to set.
     */
    public static void updateFastestTime(Player player, String gameName, double time) {
        plugin.getTaskUtils().runAsyncTask(() -> {
            try (Connection connection = getConnection()) {
                String checkSql = "SELECT fastest_time FROM " + tableName + " WHERE UUID = ? AND game_name = ?";
                Double current = QueryUtils.query(connection, checkSql,
                        rs -> rs.next() ? rs.getDouble("fastest_time") : null,
                        player.getUniqueId().toString(), gameName);
                if (current != null && time >= current) return; //Not faster

                String upsert = "REPLACE INTO " + tableName + " (UUID, game_name, fastest_time) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                    QueryUtils.executeUpdate(ps, player.getUniqueId().toString(), gameName, time);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating fastest time", e);
            }
        }, 0L);
    }

    public static FastestData getFastestData(OfflinePlayer player) {
        EnumMap<GameType, Double> perGame = new EnumMap<>(GameType.class);
        double bestOverall = Double.MAX_VALUE;

        try (Connection conn = getConnection()) {
            String sql = "SELECT game_name, fastest_time FROM " + tableName + " WHERE UUID = ?";
            bestOverall = QueryUtils.query(conn, sql, rs -> {
                double best = Double.MAX_VALUE;
                while (rs.next()) {
                    GameType game = GameType.fromName(rs.getString("game_name"));
                    // Rows for games that no longer exist are skipped, not fatal
                    if (game == null) continue;
                    double time = rs.getDouble("fastest_time");
                    perGame.put(game, time);
                    best = Math.min(best, time);
                }
                return best;
            }, player.getUniqueId().toString());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error retrieving fastest data for player", e);
        }

        if (bestOverall == Double.MAX_VALUE) {
            bestOverall = 0;
        }

        return new FastestData(perGame, bestOverall);
    }

}