package org.nsider.chatgames.database.queries;

import org.bukkit.command.CommandSender;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.annotations.Nonnull;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class TopPlayerQueries {
    private final ChatGames plugin; // Reference to the main plugin class
    private final String tableName; // The name of the database table

    /**
     * Constructor for TopManagerHandler.
     *
     * @param plugin The plugin instance
     * @param tableName The name of the database table
     */
    public TopPlayerQueries(@Nonnull ChatGames plugin, @Nonnull String tableName) {
        this.plugin = plugin;
        this.tableName = tableName;
    }

    /**
     * Sends the top player rankings to the provided sender based on the top type.
     *
     * @param sender The command sender who will receive the message.
     * @param top The type of top ranking (Points or Typers).
     */
    public void sendTopMessage(@Nonnull CommandSender sender, @Nonnull TopManager.TopType top) {
        YamlConfig messages = plugin.getConfigManager().getMessages();
        String value = "";
        String order = "";

        // Set the value and order based on the top type
        if (top == TopManager.TopType.top) {
            value = "POINTS";
            order = "DESC";
        } else if (top == TopManager.TopType.TypersTop) {
            value = "fastest";
            order = "ASC";
        }

        String sql = "SELECT UUID, " + value + " FROM " + tableName + " WHERE " + value + " > 0 ORDER BY " + value + " " + order + " LIMIT 10";

        // Query the database asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    QueryUtils.executeQuery(connection, sql)
                            .thenAccept(rs -> {
                                try {
                                    if (rs != null) {
                                        displayTopMessage(sender, messages, top, value, rs);
                                    }
                                } catch (SQLException e) {
                                    plugin.getLogger().log(Level.SEVERE, "Error displaying top message", e);
                                }
                            });
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
    private void displayTopMessage(@Nonnull CommandSender sender, @Nonnull YamlConfig messages, @Nonnull TopManager.TopType top, @Nonnull String value, @Nonnull ResultSet rs) throws SQLException {
        int spot = 0;
        plugin.getMessageUtils().sendConfigMessage(sender, messages.getString(top + ".header"));
        sender.sendMessage(plugin.getMessageUtils().color(messages.getString(top + ".separators")));

        while (rs.next()) {
            spot++;
            String amount = (top == TopManager.TopType.top) ?
                    String.valueOf(rs.getInt(value)) :
                    String.valueOf(rs.getDouble(value));

            UUID playerUUID = UUID.fromString(rs.getString("UUID"));
            String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();

            plugin.getMessageUtils().sendConfigMessage(sender, messages.getString(top + ".format")
                    .replaceAll("%spot%", "" + spot)
                    .replaceAll("%player%", playerName)
                    .replaceAll("%time%", amount)
                    .replaceAll("%points%", amount));
        }

        sender.sendMessage(plugin.getMessageUtils().color(messages.getString(top + ".separators")));

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (top == TopManager.TopType.top) {
                int playerAmount = this.getPlayerPoints(player);
                plugin.getMessageUtils().sendConfigMessage(
                        sender,
                        messages.getString(top.toString() + ".player-stat-format")
                                .replaceAll("%player_points%", String.valueOf(playerAmount))
                );
            } else if (top == TopManager.TopType.TypersTop) {
                this.getPlayerFastest(player).thenAccept(playerTime -> {
                    plugin.getMessageUtils().sendConfigMessage(sender, messages.getString(top.toString() + ".player-stat-format")
                            .replaceAll("%fastest_time%", String.valueOf(playerTime)));
                });
            }
        }

        this.getTotalPoints(total -> {
            if (top == TopManager.TopType.top && !messages.getString("top.total-stat-format").equalsIgnoreCase("")) {
                plugin.getMessageUtils().sendConfigMessage(sender, messages.getString("top.total-stat-format").replaceAll("%total_wins%", "" + total));
            }
        });

        if (!messages.getString(top.toString() + ".footer").equalsIgnoreCase("")) {
            plugin.getMessageUtils().sendConfigMessage(sender, messages.getString(top.toString() + ".footer"));
        }
    }

    /**
     * Retrieves the total points asynchronously.
     *
     * @param callback The callback to handle the total points value.
     */
    public void getTotalPoints(@Nonnull java.util.function.Consumer<Integer> callback) {
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
