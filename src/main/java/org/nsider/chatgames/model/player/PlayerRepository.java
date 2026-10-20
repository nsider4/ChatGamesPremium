package org.nsider.chatgames.model.player;

import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.chatgames.utils.database.QueryUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Reads and writes one player's data across the ChatGames tables.
 *
 * <p>Every method blocks on the database. They are only called from the
 * player-data thread in {@link ChatGamesPlayerManager}, which runs them one at a
 * time and in order, so a save on quit always lands before the next load on join.</p>
 *
 * <p>The SQL sticks to what SQLite and MySQL both accept ({@code REPLACE INTO},
 * {@code CREATE TABLE IF NOT EXISTS}), so there is one code path for both.</p>
 */
public final class PlayerRepository {

    /** Opens a database connection; closing it returns it to the pool. */
    @FunctionalInterface
    public interface ConnectionSource {
        Connection get() throws SQLException;
    }

    private final ConnectionSource connections;
    private final String mainTable;
    private final String fastestTable;
    private final String statsTable;
    private final String dailyTable;

    public PlayerRepository(@Nonnull ConnectionSource connections, @Nonnull String mainTable,
                            @Nonnull String fastestTable, @Nonnull String statsTable, @Nonnull String dailyTable) {
        this.connections = connections;
        this.mainTable = mainTable;
        this.fastestTable = fastestTable;
        this.statsTable = statsTable;
        this.dailyTable = dailyTable;
    }

    /**
     * Creates any missing tables.
     */
    public void createTables() throws SQLException {
        try (Connection connection = connections.get(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + mainTable + " ("
                    + "UUID VARCHAR(36) NOT NULL,"
                    + "POINTS INT NOT NULL,"
                    + "toggled BOOLEAN NOT NULL,"
                    + "sound BOOLEAN NOT NULL,"
                    + "PRIMARY KEY (UUID))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + fastestTable + " ("
                    + "UUID VARCHAR(36) NOT NULL,"
                    + "game_name VARCHAR(50) NOT NULL,"
                    + "fastest_time DOUBLE NOT NULL,"
                    + "PRIMARY KEY (UUID, game_name))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + statsTable + " ("
                    + "UUID VARCHAR(36) NOT NULL,"
                    + "stat VARCHAR(64) NOT NULL,"
                    + "stat_value BIGINT NOT NULL,"
                    + "PRIMARY KEY (UUID, stat))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + dailyTable + " ("
                    + "UUID VARCHAR(36) NOT NULL,"
                    + "WinsToday INT NOT NULL,"
                    + "RewardClaimed BOOLEAN NOT NULL,"
                    + "LastUpdated DATE NOT NULL,"
                    + "ChallengeStreak INT NOT NULL,"
                    + "DailyStreak INT NOT NULL,"
                    + "GlobalStreak INT NOT NULL,"
                    + "WeeklyWins INT NOT NULL,"
                    + "PRIMARY KEY (UUID))");
        }
    }

    /**
     * Loads a player, creating their rows first if they have never played.
     */
    @Nonnull
    public ChatGamesPlayer load(@Nonnull UUID uuid) throws SQLException {
        String id = uuid.toString();
        ChatGamesPlayer player = new ChatGamesPlayer(uuid);

        try (Connection connection = connections.get()) {
            boolean exists = QueryUtils.query(connection,
                    "SELECT POINTS, toggled, sound FROM " + mainTable + " WHERE UUID = ?", rs -> {
                        if (!rs.next()) return false;
                        player.setPoints(rs.getInt("POINTS"));
                        player.setToggled(rs.getBoolean("toggled"));
                        player.setSoundEnabled(rs.getBoolean("sound"));
                        return true;
                    }, id);

            if (!exists) {
                // New player: write the defaults now so leaderboards and /remove see them
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO " + mainTable + " (UUID, POINTS, toggled, sound) VALUES (?, ?, ?, ?)")) {
                    QueryUtils.executeUpdate(insert, id, 0, true, true);
                }
            }

            QueryUtils.query(connection, "SELECT stat, stat_value FROM " + statsTable + " WHERE UUID = ?", rs -> {
                while (rs.next()) player.loadStat(rs.getString("stat"), rs.getLong("stat_value"));
                return null;
            }, id);

            QueryUtils.query(connection, "SELECT game_name, fastest_time FROM " + fastestTable + " WHERE UUID = ?", rs -> {
                while (rs.next()) {
                    GameType game = GameType.fromName(rs.getString("game_name"));
                    // Times for games that no longer exist are kept in the table but ignored
                    if (game != null) player.loadFastest(game, rs.getDouble("fastest_time"));
                }
                return null;
            }, id);

            QueryUtils.query(connection, "SELECT * FROM " + dailyTable + " WHERE UUID = ?", rs -> {
                if (!rs.next()) return null;
                Date lastUpdated = rs.getDate("LastUpdated");
                if (lastUpdated != null) player.setLastUpdated(lastUpdated.toLocalDate());
                player.setWinsToday(rs.getInt("WinsToday"));
                player.setRewardClaimed(rs.getBoolean("RewardClaimed"));
                player.setChallengeStreak(rs.getInt("ChallengeStreak"));
                player.setDailyStreak(rs.getInt("DailyStreak"));
                player.setGlobalStreak(rs.getInt("GlobalStreak"));
                player.setWeeklyWins(rs.getInt("WeeklyWins"));
                return null;
            }, id);
        }

        player.resetDailyIfNeeded();
        // Loading isn't a change; only what happens from here on needs saving
        player.snapshot();
        return player;
    }

    /**
     * Writes a snapshot in one transaction, so a crash never leaves half a save.
     */
    public void save(@Nonnull ChatGamesPlayer.Snapshot data) throws SQLException {
        String id = data.uuid.toString();

        try (Connection connection = connections.get()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement main = connection.prepareStatement(
                        "REPLACE INTO " + mainTable + " (UUID, POINTS, toggled, sound) VALUES (?, ?, ?, ?)")) {
                    QueryUtils.executeUpdate(main, id, data.points, data.toggled, data.soundEnabled);
                }

                if (!data.stats.isEmpty()) {
                    try (PreparedStatement stat = connection.prepareStatement(
                            "REPLACE INTO " + statsTable + " (UUID, stat, stat_value) VALUES (?, ?, ?)")) {
                        for (Map.Entry<String, Long> entry : data.stats.entrySet()) {
                            stat.setString(1, id);
                            stat.setString(2, entry.getKey());
                            stat.setLong(3, entry.getValue());
                            stat.addBatch();
                        }
                        stat.executeBatch();
                    }
                }

                if (!data.fastest.isEmpty()) {
                    try (PreparedStatement time = connection.prepareStatement(
                            "REPLACE INTO " + fastestTable + " (UUID, game_name, fastest_time) VALUES (?, ?, ?)")) {
                        for (Map.Entry<GameType, Double> entry : data.fastest.entrySet()) {
                            time.setString(1, id);
                            time.setString(2, entry.getKey().getConfigKey());
                            time.setDouble(3, entry.getValue());
                            time.addBatch();
                        }
                        time.executeBatch();
                    }
                }

                try (PreparedStatement daily = connection.prepareStatement("REPLACE INTO " + dailyTable
                        + " (UUID, WinsToday, RewardClaimed, LastUpdated, ChallengeStreak, DailyStreak, GlobalStreak, WeeklyWins)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    QueryUtils.executeUpdate(daily, id, data.winsToday, data.rewardClaimed, Date.valueOf(data.lastUpdated),
                            data.challengeStreak, data.dailyStreak, data.globalStreak, data.weeklyWins);
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    /**
     * Removes every row belonging to a player.
     *
     * @return true if the player had data
     */
    public boolean delete(@Nonnull UUID uuid) throws SQLException {
        String id = uuid.toString();
        try (Connection connection = connections.get()) {
            int removed = 0;
            for (String table : new String[]{mainTable, fastestTable, statsTable, dailyTable}) {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM " + table + " WHERE UUID = ?")) {
                    removed += QueryUtils.executeUpdate(delete, id);
                }
            }
            return removed > 0;
        }
    }

    /**
     * Reads every player's points and every fastest time, for the leaderboards behind
     * the placeholders. Rows for unknown games or malformed UUIDs are skipped.
     */
    @Nonnull
    public LeaderboardSnapshot readLeaderboards(@Nonnull Function<UUID, String> names) throws SQLException {
        try (Connection connection = connections.get()) {
            List<LeaderboardSnapshot.Row> points = QueryUtils.query(connection,
                    "SELECT UUID, POINTS FROM " + mainTable + " WHERE POINTS > 0", rs -> {
                        List<LeaderboardSnapshot.Row> rows = new ArrayList<>();
                        while (rs.next()) {
                            UUID uuid = parseUuid(rs.getString("UUID"));
                            if (uuid != null) rows.add(new LeaderboardSnapshot.Row(uuid, rs.getInt("POINTS")));
                        }
                        return rows;
                    });
            List<LeaderboardSnapshot.TimeRow> times = QueryUtils.query(connection,
                    "SELECT UUID, game_name, fastest_time FROM " + fastestTable + " WHERE fastest_time > 0", rs -> {
                        List<LeaderboardSnapshot.TimeRow> rows = new ArrayList<>();
                        while (rs.next()) {
                            UUID uuid = parseUuid(rs.getString("UUID"));
                            GameType game = GameType.fromName(rs.getString("game_name"));
                            if (uuid != null && game != null) {
                                rows.add(new LeaderboardSnapshot.TimeRow(uuid, game, rs.getDouble("fastest_time")));
                            }
                        }
                        return rows;
                    });
            return LeaderboardSnapshot.build(points, times, names);
        }
    }

    private static UUID parseUuid(String text) {
        try {
            return text == null ? null : UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
