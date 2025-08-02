package org.nsider.chatgames.database.queries;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.enums.TopType;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TopPlayerQueries {
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
     * Sends the top player rankings to the provided sender based on the top type.
     *
     * @param sender The command sender who will receive the message.
     * @param top The type of top ranking (Points or Typers).
     */
    public static void sendTopMessage(@Nonnull CommandSender sender, @Nonnull TopType top) {
        YamlConfig messages = ConfigManager.getMessagesConfig();
        String value;
        String order = "";

        // Set the value and order based on the top type
        if (top == TopType.POINTS) {
            value = "POINTS";
            order = "DESC";
        } else if (top == TopType.TIME) {
            value = "fastest";
            order = "ASC";
        } else {
            value = "";
        }

        String sql = "SELECT UUID, " + value + " FROM " + tableName + " WHERE " + value + " > 0 ORDER BY " + value + " " + order + " LIMIT 10";

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection()) {
                try (ResultSet rs = QueryUtils.executeQuery(connection, sql)) {
                    try {
                        if (rs != null) {
                            displayTopMessage(sender, messages, top, value, rs);
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.SEVERE, "Error displaying top message", e);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error executing SQL query", ex);
            }
        });
    }

    /**
     * Displays the top rankings message to the sender.
     *
     * @param sender The command sender who will receive the message.
     * @param messages The message configuration.
     * @param top The top type (Points or Typers).
     * @param value The column value (POINTS or fastest).
     * @param rs The result set containing the top players.
     * @throws SQLException If a database error occurs.
     */
    private static void displayTopMessage(@Nonnull CommandSender sender, @Nonnull YamlConfig messages, @Nonnull TopType top, @Nonnull String value, @Nonnull ResultSet rs) throws SQLException {
        int spot = 0;
        MessageUtils.sendMessage(sender, messages.getString(top + ".header"));
        sender.sendMessage(MessageUtils.color(messages.getString(top + ".separators")));

        while (rs.next()) {
            spot++;
            String amount = (top == TopType.POINTS) ?
                    String.valueOf(rs.getInt(value)) :
                    String.valueOf(rs.getDouble(value));

            UUID playerUUID = UUID.fromString(rs.getString("UUID"));
            String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();

            MessageUtils.sendMessage(sender, messages.getString(top + ".format")
                    .replaceAll("%spot%", "" + spot)
                    .replaceAll("%player%", playerName)
                    .replaceAll("%time%", amount)
                    .replaceAll("%points%", amount));
        }

        MessageUtils.sendMessage(sender, messages.getString(top + ".separators"));

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (top == TopType.POINTS) {
                int playerAmount = ValueQueries.getPlayerPoints(player);
                MessageUtils.sendMessage(sender,
                        messages.getString(top.toString() + ".player-stat-format")
                                .replaceAll("%player_points%", String.valueOf(playerAmount))
                );
            } else if (top == TopType.TIME) {
                double fastest = ValueQueries.getPlayerFastest(player);
                MessageUtils.sendMessage(sender,
                        messages.getString(top.toString() + ".player-stat-format")
                            .replaceAll("%fastest_time%", String.valueOf(fastest)));

            }
        }

        getTotalPoints(total -> {
            if (top == TopType.POINTS && !messages.getString("top.total-stat-format").equalsIgnoreCase("")) {
                MessageUtils.sendMessage(sender,
                        messages.getString("top.total-stat-format")
                                .replaceAll("%total_wins%", "" + total)
                );
            }
        });

        if (!messages.getString(top.toString() + ".footer").equalsIgnoreCase("")) {
            MessageUtils.sendMessage(sender, messages.getString(top.toString() + ".footer"));
        }
    }

    /**
     * Retrieves the total points asynchronously.
     *
     * @param callback The callback to handle the total points value.
     */
    private static void getTotalPoints(@Nonnull Consumer<Integer> callback) {
        String sql = "SELECT SUM(POINTS) FROM " + tableName;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            callback.accept(rs.getInt(1));
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error getting total points", ex);
            }
        });
    }
}
