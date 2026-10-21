package org.nsider.chatgames.model.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.placeholders.LeaderboardSnapshot;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Upgrading a database written by 1.9.9, whose main table has a {@code fastest NOT NULL} column. */
class LegacyDatabaseUpgradeTest {

    private static final UUID FAST = new UUID(0, 1);
    private static final UUID NEVER_WON = new UUID(0, 2);

    @TempDir
    Path folder;

    private String url;
    private PlayerRepository repository;

    @BeforeEach
    void createOldDatabase() throws SQLException {
        url = "jdbc:sqlite:" + folder.resolve("old.db");
        try (Connection connection = DriverManager.getConnection(url); Statement statement = connection.createStatement()) {
            // 1.9.9's exact table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS chatgames (UUID varchar(100) NOT NULL,POINTS int(11) NOT NULL,"
                    + "toggled BOOLEAN NOT NULL,sound BOOLEAN NOT NULL,fastest DOUBLE NOT NULL,PRIMARY KEY (UUID));");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO chatgames (UUID, POINTS, toggled, sound, fastest) VALUES (?, ?, ?, ?, ?)")) {
                QueryUtils.executeUpdate(insert, FAST.toString(), 12, true, false, 1.75);
                QueryUtils.executeUpdate(insert, NEVER_WON.toString(), 0, false, true, 0.0);
            }
        }
        repository = new PlayerRepository(() -> DriverManager.getConnection(url),
                "chatgames", "chatgames_fastest_times", "chatgames_stats");
    }

    @Test
    void oldRowsKeepTheirValuesAndTheOldTableIsKept() throws SQLException {
        assertEquals(2, repository.createTables());
        assertEquals(-1, repository.createTables(), "already upgraded: nothing to do");

        ChatGamesPlayer fast = repository.load(FAST);
        assertEquals(12, fast.getPoints());
        assertTrue(fast.isToggled());
        assertFalse(fast.isSoundEnabled());
        assertEquals(1.75, fast.getBestOverall(), "the 1.9.9 record counts overall");
        assertEquals(0, fast.getFastest(GameType.MATH), "but for no game in particular");

        ChatGamesPlayer neverWon = repository.load(NEVER_WON);
        assertFalse(neverWon.isToggled());
        assertEquals(0, neverWon.getBestOverall(), "1.9.9 stored 0 for players who never won");

        assertEquals(2, count("SELECT COUNT(*) FROM chatgames_1_9_9"));
        assertEquals(0, columnCount("chatgames", "fastest"));
    }

    @Test
    void newPlayersCanBeInsertedAfterTheUpgrade() throws SQLException {
        repository.createTables();
        UUID newcomer = UUID.randomUUID();
        repository.load(newcomer);
        assertEquals(1, count("SELECT COUNT(*) FROM chatgames WHERE UUID = '" + newcomer + "'"));
    }

    @Test
    void aNewTimeOnlyReplacesTheOldRecordOverallIfItIsFaster() throws SQLException {
        repository.createTables();
        ChatGamesPlayer fast = repository.load(FAST);
        fast.recordTime(GameType.MATH, 3.0);
        assertEquals(1.75, fast.getBestOverall());
        fast.recordTime(GameType.UNSCRAMBLE, 1.5);
        repository.save(fast.snapshot());

        LeaderboardSnapshot board = repository.readLeaderboards(uuid -> "p");
        assertEquals(1.5, board.fastest(1).getValue());
        assertEquals(3.0, board.fastest(GameType.MATH, 1).getValue());
        assertEquals(1.5, repository.load(FAST).getBestOverall(), "reloaded, the faster new time wins");
    }

    @Test
    void theOldRecordIsOnTheOverallLeaderboard() throws SQLException {
        repository.createTables();
        LeaderboardSnapshot board = repository.readLeaderboards(uuid -> uuid.equals(FAST) ? "Fast" : "?");
        assertNotNull(board.fastest(1));
        assertEquals("Fast", board.fastest(1).getName());
        assertEquals(1.75, board.fastest(1).getValue());
    }

    @Test
    void anInterruptedUpgradeFinishesOnTheNextStart() throws SQLException {
        // Stopped part-way: a half-built copy exists next to the untouched old table
        try (Connection connection = DriverManager.getConnection(url); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE chatgames_upgrading (UUID VARCHAR(36) NOT NULL, POINTS INT NOT NULL,"
                    + " toggled BOOLEAN NOT NULL, sound BOOLEAN NOT NULL, PRIMARY KEY (UUID))");
            statement.executeUpdate("INSERT INTO chatgames_upgrading VALUES ('" + FAST + "', 12, 1, 0)");
        }
        assertEquals(2, repository.createTables());
        assertEquals(12, repository.load(FAST).getPoints());

        // Stopped between the renames: the old table is already renamed, the copy isn't yet
        try (Connection connection = DriverManager.getConnection(url); Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE chatgames RENAME TO chatgames_upgrading");
        }
        assertEquals(-1, repository.createTables());
        assertEquals(12, repository.load(FAST).getPoints(), "the finished copy became the main table");
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            return QueryUtils.query(connection, sql, rs -> rs.next() ? rs.getInt(1) : 0);
        }
    }

    private int columnCount(String table, String column) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            return QueryUtils.query(connection, "PRAGMA table_info(" + table + ")", rs -> {
                int found = 0;
                while (rs.next()) if (rs.getString("name").equalsIgnoreCase(column)) found++;
                return found;
            });
        }
    }
}
