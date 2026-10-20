package org.nsider.chatgames.database.queries;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class UpdateQueries {

    private static ChatGames plugin;
    private static String tableName;

    /**
     * Initializes the UpdateQueries class.
     *
     * @param instance The ChatGames plugin instance.
     */
    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = ConfigValue.getString(ConfigPaths.TABLE_NAME);
    }

    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    public static void updatePlayerPoints(OfflinePlayer player, int points) {
        String sql = "UPDATE " + tableName + " SET POINTS = ? WHERE UUID = ?";
        plugin.getScheduler().async(() -> executeUpdate(sql, points, player.getUniqueId().toString()));
    }

    public static void updatePlayerToggle(Player player, boolean toggle) {
        String sql = "UPDATE " + tableName + " SET toggled = ? WHERE UUID = ?";
        plugin.getScheduler().async(() -> executeUpdate(sql, toggle, player.getUniqueId().toString()));
    }

    public static void updatePlayerToggleSound(Player player, boolean toggle) {
        String sql = "UPDATE " + tableName + " SET sound = ? WHERE UUID = ?";
        plugin.getScheduler().async(() -> executeUpdate(sql, toggle, player.getUniqueId().toString()));
    }

    private static void executeUpdate(String sql, Object... params) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            QueryUtils.executeUpdate(ps, params);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing update query", e);
        }
    }
}