package org.nsider.chatgames.database.queries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import lombok.Getter;
import org.nsider.chatgames.database.Errors;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;
import org.nsider.chatgames.ChatGames;

import java.util.logging.Level;

/**
 * A class to manage database operations related to player management.
 * Handles checking if players exist, creating new players, and deleting players.
 */
public class ManagementQueries {

    private static ChatGames plugin;
    @Getter private static String SQLiteCreateChatGamesTable;
    private static String tableName;

    /**
     * Initializes the ManagementQueries class.
     *
     * @param instance The ChatGames plugin instance.
     */
    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = plugin.getConfig().getString("table_name", "chatgames");
        SQLiteCreateChatGamesTable = generateCreateTableStatement();
    }

    /**
     * Generates the SQL statement for creating the table in the database.
     *
     * @return The SQL query to create the table.
     */
    private static String generateCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "UUID varchar(100) NOT NULL," +
                "POINTS int(11) NOT NULL," +
                "toggled BOOLEAN NOT NULL," +
                "sound BOOLEAN NOT NULL," +
                "fastest DOUBLE NOT NULL," +
                "PRIMARY KEY (UUID)" +
                ");";
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
     * Checks if the player exists in the database.
     *
     * @param player The OfflinePlayer to check.
     * @return True if the player exists, false otherwise.
     */
    public static boolean playerExists(OfflinePlayer player) {
        boolean exists = false;
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE UUID = ?")) {
            preparedStatement.setString(1, player.getUniqueId().toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                exists = resultSet != null && resultSet.next() && resultSet.getString("UUID") != null;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking if player exists", e);
        }
        return exists;
    }

    /**
     * Creates a new player in the database.
     *
     * @param player        The OfflinePlayer to create.
     * @param startingPoints The starting points to assign to the player.
     * @param callback      A callback function to notify success or failure.
     */
    public static void createPlayer(OfflinePlayer player, int startingPoints, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String insertStatement = buildInsertStatement();
                try (Connection connection = getConnection();
                     PreparedStatement ps = connection.prepareStatement(insertStatement)) {

                    QueryUtils.executeUpdate(ps,
                            player.getUniqueId().toString(),
                            startingPoints,
                            true,
                            true,
                            0
                    );

                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    /**
     * Builds the SQL insert statement based on the storage type.
     *
     * @return The SQL insert statement.
     */
    private static String buildInsertStatement() {
        if (plugin.getConfig().getString("Storage").equalsIgnoreCase("MySQL")) {
            return "INSERT IGNORE INTO " + tableName + " (UUID, POINTS, toggled, sound, fastest) VALUES (?, ?, ?, ?, ?);";
        } else {
            return "INSERT INTO " + tableName + " (UUID, POINTS, toggled, sound, fastest) VALUES (?, ?, ?, ?, ?);";
        }
    }

    /**
     * Creates a new player if they do not already exist in the database.
     *
     * @param player The OfflinePlayer to check and create if necessary.
     */
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

    /**
     * Deletes a player from the database.
     *
     * @param player  The OfflinePlayer to delete.
     * @param callback A callback function to notify success or failure.
     */
    public static void deletePlayer(OfflinePlayer player, Consumer<Boolean> callback) {
        if (playerExists(player)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection connection = getConnection();
                     PreparedStatement ps = connection.prepareStatement("DELETE FROM " + tableName + " WHERE UUID = ?")) {
                    QueryUtils.executeUpdate(ps, player.getUniqueId().toString());

                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                }
            });
        } else {
            callback.accept(true);
        }
    }
}
