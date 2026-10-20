package org.nsider.chatgames.database.queries;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.enums.TopType;
import org.nsider.chatgames.model.player.ChatGamesPlayer;
import org.nsider.chatgames.model.player.ChatGamesPlayerManager;
import org.nsider.chatgames.model.top.TopData;
import org.nsider.chatgames.model.top.TopPresenter;
import org.nsider.chatgames.utils.database.DatabaseUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class TopPlayerQueries {

    private static ChatGames plugin;
    private static String tableName;

    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = ConfigValue.getString(ConfigPaths.TABLE_NAME);
    }

    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    public static void sendTopMessage(CommandSender sender, TopType top, GameType... game) {
        final YamlConfig messages = ConfigManager.getMessagesConfig();

        String sql;
        switch (top) {
            case POINTS:
                sql = "SELECT UUID, POINTS AS val FROM " + tableName + " WHERE POINTS > 0 ORDER BY POINTS DESC LIMIT 10";
                break;
            case TypersTop:
                sql = "SELECT UUID, MIN(fastest_time) AS val FROM game_fastest_times GROUP BY UUID ORDER BY val ASC LIMIT 10";
                break;
            case GameTypersTop:
                sql = "SELECT UUID, fastest_time AS val FROM game_fastest_times WHERE game_name = ? ORDER BY val ASC LIMIT 10";
                break;
            default:
                throw new IllegalArgumentException("Unsupported top type: " + top);
        }

        plugin.getScheduler().async(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (top == TopType.GameTypersTop && game.length == 1) {
                    ps.setString(1, game[0].name().toLowerCase());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    List<TopData> rows = new ArrayList<>();
                    int rank = 0;

                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("UUID"));
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        double value = rs.getDouble("val");
                        rows.add(new TopData(++rank, name, value));
                    }

                    // Everything below the list also comes from the database, so it is read here, off the main thread
                    int totalPoints = (top == TopType.POINTS) ? ValueQueries.getTotalPoints() : 0;
                    GameType gameType = game.length == 0 ? null : game[0];
                    double personal = sender instanceof Player
                            ? (gameType == null
                                ? getPersonalValue((Player) sender, top)
                                : getPersonalValue((Player) sender, top, gameType))
                            : 0;

                    plugin.getScheduler().global(() -> {
                        TopPresenter presenter = gameType == null
                                ? new TopPresenter(sender, messages, top)
                                : new TopPresenter(sender, messages, top, gameType);
                        presenter.present(rows, personal, totalPoints);
                    });
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading leaderboard for " + top, e);
            }
        });
    }

    private static double getPersonalValue(Player player, TopType top, GameType... game) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());

        switch (top) {
            case POINTS:
                return cgPlayer.getPoints();
            case TypersTop:
                return cgPlayer.getFastestData().getBestOverall();
            case GameTypersTop:
                return cgPlayer.getFastestData().getTime(game[0]);
            default:
                return 0;
        }
    }
}