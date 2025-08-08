package org.nsider.chatgames.database.queries;

import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.Errors;
import org.nsider.chatgames.model.player.PlayerMainData;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class ManagementQueries {

    private static ChatGames plugin;

    @Getter
    private static String SQLiteCreateChatGamesTable;

    private static String tableName;

    /**
     * Initializes the ManagementQueries class.
     *
     * @param instance The ChatGames plugin instance.
     */
    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = ConfigValue.getString(ConfigPaths.TABLE_NAME);
        SQLiteCreateChatGamesTable = generateCreateTableStatement();
    }

    private static String generateCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "UUID VARCHAR(100) NOT NULL," +
                "POINTS INT NOT NULL," +
                "toggled BOOLEAN NOT NULL," +
                "sound BOOLEAN NOT NULL," +
                "PRIMARY KEY (UUID)" +
                ");";
    }

    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    public static boolean playerExists(OfflinePlayer player) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT UUID FROM " + tableName + " WHERE UUID = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking if player exists", e);
        }
        return false;
    }

    public static void createPlayer(OfflinePlayer player, int startingPoints, Consumer<Boolean> callback) {
        plugin.getTaskUtils().runAsyncTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(buildInsertStatement())) {

                QueryUtils.executeUpdate(ps,
                        player.getUniqueId().toString(),
                        startingPoints,
                        true,
                        true
                );

                plugin.getTaskUtils().runSyncTaskLater(() -> callback.accept(true), 0L);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
                plugin.getTaskUtils().runSyncTaskLater(() -> callback.accept(false), 0L);
            }
        }, 0L);
    }

    public static PlayerMainData getPlayerData(OfflinePlayer player) {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE UUID = ?")) {

            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int points = rs.getInt("POINTS");
                    boolean toggled = rs.getBoolean("toggled");
                    boolean sound = rs.getBoolean("sound");
                    return new PlayerMainData(points, toggled, sound);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error fetching player data", e);
        }
        return null;
    }

    public static void updatePlayerData(UUID uuid, int points, boolean toggled, boolean sound) {
        plugin.getTaskUtils().runAsyncTask(() -> {
            String sql = "UPDATE " + tableName + " SET POINTS = ?, toggled = ?, sound = ? WHERE UUID = ?";
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                QueryUtils.executeUpdate(ps, points, toggled, sound, uuid.toString());

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
            }
        }, 0L);
    }

    private static String buildInsertStatement() {
        boolean isMySQL = ConfigValue.getString(ConfigPaths.STORAGE_TYPE).equalsIgnoreCase("MySQL");
        return isMySQL
                ? "INSERT IGNORE INTO " + tableName + " (UUID, POINTS, toggled, sound) VALUES (?, ?, ?, ?);"
                : "INSERT INTO " + tableName + " (UUID, POINTS, toggled, sound) VALUES (?, ?, ?, ?);";
    }

    public static void createPlayerIfNotExists(OfflinePlayer player) {
        if (!playerExists(player)) {
            createPlayer(player, 0, success -> {
                if (!success) {
                    plugin.getLogger().warning("Error while trying to create player: " + player.getName());
                } else {
                    plugin.getLogger().info("Player did not exist! Created player: " + player.getName());
                }
            });
        }
    }

    public static void deletePlayer(OfflinePlayer player, Consumer<Boolean> callback) {
        if (!playerExists(player)) {
            callback.accept(true);
            return;
        }

        plugin.getTaskUtils().runAsyncTask(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement("DELETE FROM " + tableName + " WHERE UUID = ?")) {

                QueryUtils.executeUpdate(ps, player.getUniqueId().toString());
                plugin.getTaskUtils().runSyncTaskLater(() -> callback.accept(true), 0L);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
                plugin.getTaskUtils().runSyncTaskLater(() -> callback.accept(false), 0L);
            }
        }, 0L);
    }
}