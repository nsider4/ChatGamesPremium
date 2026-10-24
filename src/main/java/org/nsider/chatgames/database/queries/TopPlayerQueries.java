package org.nsider.chatgames.database.queries;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.ConfigManager;
import org.nsider.chatgames.config.YamlConfig;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.api.GameType;
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

/**
 * The {@code /chatgames top} leaderboards, read from the database and sent as chat messages.
 */
public class TopPlayerQueries {

    private static ChatGames plugin;
    private static String tableName;
    private static String fastestTable;

    /**
     * @return every race's id as a quoted SQL list, e.g. {@code 'hunt','place'}. Ids are only
     *         lower-case letters, digits and underscores (GameType checks), so safe to put in a query.
     */
    static String raceKeysForSql() {
        StringBuilder keys = new StringBuilder();
        for (GameType type : GameType.values()) {
            if (!type.isRace()) continue;
            if (keys.length() > 0) keys.append(',');
            keys.append('\'').append(type.getConfigKey()).append('\'');
        }
        return keys.toString();
    }

    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = ConfigValue.getString(ConfigPaths.TABLE_NAME);
        fastestTable = ConfigValue.getString(ConfigPaths.FASTEST_TIMES_TABLE);
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
                // Races are timed too, but the overall list is for chat games ("fastest typers")
                sql = "SELECT UUID, MIN(fastest_time) AS val FROM " + fastestTable + " WHERE fastest_time > 0 AND game_name NOT IN ("
                        + raceKeysForSql() + ") GROUP BY UUID ORDER BY val ASC LIMIT 10";
                break;
            case GameTypersTop:
                sql = "SELECT UUID, fastest_time AS val FROM " + fastestTable + " WHERE game_name = ? AND fastest_time > 0 ORDER BY val ASC LIMIT 10";
                break;
            default:
                throw new IllegalArgumentException("Unsupported top type: " + top);
        }

        // Online players' points and times live in the cache: save them first, and read on the
        // player-data thread so the query runs after those saves
        ChatGamesPlayerManager.saveAll();
        ChatGamesPlayerManager.runAfterSaves(() -> {
            List<TopData> rows = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (top == TopType.GameTypersTop && game.length == 1) {
                    ps.setString(1, game[0].getConfigKey());
                }

                try (ResultSet rs = ps.executeQuery()) {
                    int rank = 0;

                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("UUID"));
                        String name = Bukkit.getOfflinePlayer(uuid).getName();
                        if (name == null) name = "?";
                        double value = rs.getDouble("val");
                        rows.add(new TopData(++rank, name, value));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading leaderboard for " + top, e);
                return;
            }

            // Opens its own connection, so only once the one above is closed: SQLite's pool holds one
            int totalPoints = (top == TopType.POINTS) ? ValueQueries.getTotalPoints() : 0;
            GameType gameType = game.length == 0 ? null : game[0];
            double personal = sender instanceof Player
                    ? (gameType == null
                        ? getPersonalValue((Player) sender, top)
                        : getPersonalValue((Player) sender, top, gameType))
                    : 0;

            plugin.getScheduler().global(() ->
                    new TopPresenter(sender, messages, top, gameType).present(rows, personal, totalPoints));
        });
    }

    private static double getPersonalValue(Player player, TopType top, GameType... game) {
        ChatGamesPlayer cgPlayer = ChatGamesPlayerManager.get(player.getUniqueId());
        if (cgPlayer == null) return 0;

        switch (top) {
            case POINTS:
                return cgPlayer.getPoints();
            case TypersTop:
                return cgPlayer.getBestOverall();
            case GameTypersTop:
                return cgPlayer.getFastest(game[0]);
            default:
                return 0;
        }
    }
}