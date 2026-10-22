package org.nsider.chatgames.model.player;

import org.nsider.chatgames.api.GameType;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.chatgames.utils.database.QueryUtils;
import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    /** The fastest-times "game" that holds a 1.9.9 player's one overall best time. */
    public static final String LEGACY_TIMES = "legacy";

    private final ConnectionSource connections;
    private final String mainTable;
    private final String fastestTable;
    private final String statsTable;

    public PlayerRepository(@Nonnull ConnectionSource connections, @Nonnull String mainTable,
                            @Nonnull String fastestTable, @Nonnull String statsTable) {
        this.connections = connections;
        this.mainTable = mainTable;
        this.fastestTable = fastestTable;
        this.statsTable = statsTable;
    }

    /**
     * Creates any missing tables, upgrading a 1.9.9 main table first.
     *
     * @return how many players a 1.9.9 table was upgraded with, or -1 if there was nothing to upgrade
     */
    public int createTables() throws SQLException {
        try (Connection connection = connections.get(); Statement statement = connection.createStatement()) {
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
            int upgraded = upgradeFrom199(connection, statement);
            statement.executeUpdate(createMainTable(mainTable, true));
            return upgraded;
        }
    }

    private static String createMainTable(String table, boolean ifMissing) {
        return "CREATE TABLE " + (ifMissing ? "IF NOT EXISTS " : "") + table + " ("
                + "UUID VARCHAR(36) NOT NULL,"
                + "POINTS INT NOT NULL,"
                + "toggled BOOLEAN NOT NULL,"
                + "sound BOOLEAN NOT NULL,"
                + "PRIMARY KEY (UUID))";
    }

    /**
     * 1.9.9 kept one overall best time in a {@code fastest DOUBLE NOT NULL} column of the main
     * table, which the 2.0.0 insert doesn't fill. The rows are copied into a new main table, each
     * time above 0 (0 meant "never won") goes to the fastest-times table as {@value #LEGACY_TIMES},
     * and the old table is kept, renamed to {@code <table>_1_9_9}.
     *
     * <p>Every step can run again: a crash part-way leaves either the old table (and a half-built
     * copy, rebuilt next time) or a finished copy waiting for its rename.</p>
     */
    private int upgradeFrom199(Connection connection, Statement statement) throws SQLException {
        String copy = mainTable + "_upgrading";
        Set<String> columns = columns(connection, mainTable);
        if (columns == null) {
            // Stopped between the two renames: only the finished copy's rename is left
            if (columns(connection, copy) != null) statement.executeUpdate("ALTER TABLE " + copy + " RENAME TO " + mainTable);
            return -1;
        }
        if (!columns.contains("fastest")) return -1;

        if (columns(connection, copy) != null) statement.executeUpdate("DROP TABLE " + copy);
        statement.executeUpdate(createMainTable(copy, false));
        int players = statement.executeUpdate("INSERT INTO " + copy + " (UUID, POINTS, toggled, sound)"
                + " SELECT UUID, POINTS, toggled, sound FROM " + mainTable);
        statement.executeUpdate("INSERT INTO " + fastestTable + " (UUID, game_name, fastest_time)"
                + " SELECT UUID, '" + LEGACY_TIMES + "', fastest FROM " + mainTable + " WHERE fastest > 0"
                + " AND UUID NOT IN (SELECT UUID FROM " + fastestTable + " WHERE game_name = '" + LEGACY_TIMES + "')");

        String backup = mainTable + "_1_9_9";
        for (int i = 2; columns(connection, backup) != null; i++) backup = mainTable + "_1_9_9_" + i;
        statement.executeUpdate("ALTER TABLE " + mainTable + " RENAME TO " + backup);
        statement.executeUpdate("ALTER TABLE " + copy + " RENAME TO " + mainTable);
        return players;
    }

    /** A table's column names in lower case, or null if the table doesn't exist. */
    @Nullable
    private static Set<String> columns(Connection connection, String table) {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            Set<String> names = new HashSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) names.add(meta.getColumnName(i).toLowerCase(Locale.ROOT));
            return names;
        } catch (SQLException e) {
            return null;
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
                    String name = rs.getString("game_name");
                    GameType game = GameType.fromName(name);
                    // Times for games that no longer exist are kept in the table but ignored
                    if (game != null) player.loadFastest(game, rs.getDouble("fastest_time"));
                    else if (LEGACY_TIMES.equals(name)) player.loadLegacyBest(rs.getDouble("fastest_time"));
                }
                return null;
            }, id);
        }

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
            for (String table : new String[]{mainTable, fastestTable, statsTable}) {
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
                            String name = rs.getString("game_name");
                            GameType game = GameType.fromName(name);
                            if (uuid != null && (game != null || LEGACY_TIMES.equals(name))) {
                                rows.add(new LeaderboardSnapshot.TimeRow(uuid, game, rs.getDouble("fastest_time")));
                            }
                        }
                        return rows;
                    });
            return LeaderboardSnapshot.build(points, times, names);
        }
    }

    /**
     * @return the players with the highest value of a stat, highest first
     */
    @Nonnull
    public List<Map.Entry<UUID, Long>> topStat(@Nonnull String stat, int limit) throws SQLException {
        try (Connection connection = connections.get()) {
            return QueryUtils.query(connection, "SELECT UUID, stat_value FROM " + statsTable
                    + " WHERE stat = ? AND stat_value > 0 ORDER BY stat_value DESC LIMIT " + Math.max(1, limit), rs -> {
                List<Map.Entry<UUID, Long>> rows = new ArrayList<>();
                while (rs.next()) {
                    UUID uuid = parseUuid(rs.getString("UUID"));
                    if (uuid != null) rows.add(new AbstractMap.SimpleImmutableEntry<>(uuid, rs.getLong("stat_value")));
                }
                return rows;
            }, stat);
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
