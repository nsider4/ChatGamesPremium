package org.nsider.chatgames.model.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRepositoryTest {

    @TempDir
    Path folder;

    private String url;
    private PlayerRepository repository;

    @BeforeEach
    void createDatabase() throws SQLException {
        url = "jdbc:sqlite:" + folder.resolve("test.db");
        repository = new PlayerRepository(() -> DriverManager.getConnection(url),
                "chatgames", "chatgames_fastest_times", "chatgames_stats", "chatgames_daily");
        repository.createTables();
        // Running it twice must be harmless: it runs on every startup
        repository.createTables();
    }

    @Test
    void newPlayersGetDefaultRowsOnFirstLoad() throws SQLException {
        UUID uuid = UUID.randomUUID();
        ChatGamesPlayer player = repository.load(uuid);

        assertEquals(0, player.getPoints());
        assertTrue(player.isToggled());
        assertTrue(player.isSoundEnabled());
        assertEquals(1, count("chatgames", uuid), "the main row is written immediately");
    }

    @Test
    void leaderboardsReadEveryPlayersPointsAndTimes() throws SQLException {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID noPoints = UUID.randomUUID();
        ChatGamesPlayer a = repository.load(first);
        a.addPoints(5);
        a.recordTime(GameType.GUESS_THE_NUMBER, 3.5);
        repository.save(a.snapshot());
        ChatGamesPlayer b = repository.load(second);
        b.addPoints(9);
        b.recordTime(GameType.GUESS_THE_NUMBER, 1.5);
        b.recordTime(GameType.MATH, 2.0);
        repository.save(b.snapshot());
        repository.load(noPoints);

        org.nsider.chatgames.placeholders.LeaderboardSnapshot board =
                repository.readLeaderboards(uuid -> uuid.equals(first) ? "First" : "Second");
        assertEquals("Second", board.points(1).getName());
        assertEquals(9, (int) board.points(1).getValue());
        assertEquals("First", board.points(2).getName());
        assertEquals(null, board.points(3), "players with 0 points aren't ranked");
        assertEquals(14, board.getTotalPoints());
        assertEquals("Second", board.fastest(GameType.GUESS_THE_NUMBER, 1).getName());
        assertEquals(3.5, board.fastest(GameType.GUESS_THE_NUMBER, 2).getValue());
        assertEquals(2, board.fastestRank(first));
    }

    @Test
    void everythingSurvivesASaveAndReload() throws SQLException {
        UUID uuid = UUID.randomUUID();
        ChatGamesPlayer player = repository.load(uuid);
        player.addPoints(12);
        player.setToggled(false);
        player.setSoundEnabled(false);
        player.incrementStat(Stat.WINS, 3);
        player.incrementStat(Stat.wins(GameType.MATH), 2);
        player.recordTime(GameType.MATH, 2.5);
        player.recordTime(GameType.UNSCRAMBLE, 4.25);
        player.setWinsToday(2);
        player.setDailyStreak(5);
        repository.save(player.snapshot());

        ChatGamesPlayer loaded = repository.load(uuid);
        assertEquals(12, loaded.getPoints());
        assertFalse(loaded.isToggled());
        assertFalse(loaded.isSoundEnabled());
        assertEquals(3, loaded.getStat(Stat.WINS));
        assertEquals(2, loaded.getStat(Stat.wins(GameType.MATH)));
        assertEquals(2.5, loaded.getFastest(GameType.MATH));
        assertEquals(4.25, loaded.getFastest(GameType.UNSCRAMBLE));
        assertEquals(2.5, loaded.getBestOverall());
        assertEquals(2, loaded.getWinsToday());
        assertEquals(5, loaded.getDailyStreak());
        assertEquals(LocalDate.now(), loaded.getLastUpdated());
    }

    @Test
    void onlyChangedStatsAreWritten() throws SQLException {
        UUID uuid = UUID.randomUUID();
        ChatGamesPlayer player = repository.load(uuid);
        player.incrementStat(Stat.WINS, 1);
        ChatGamesPlayer.Snapshot first = player.snapshot();
        assertEquals(1, first.stats.size());

        ChatGamesPlayer.Snapshot second = player.snapshot();
        assertTrue(second.stats.isEmpty(), "nothing changed since the last snapshot");
        assertTrue(second.fastest.isEmpty());
    }

    @Test
    void aFailedSaveIsRetriedByTheNextOne() {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        player.incrementStat(Stat.GLOBAL_PLAYED, 1);
        player.recordTime(GameType.REACTION, 1.0);
        ChatGamesPlayer.Snapshot failed = player.snapshot();

        player.restoreDirty(failed);
        ChatGamesPlayer.Snapshot retry = player.snapshot();
        assertEquals(1L, retry.stats.get(Stat.GLOBAL_PLAYED));
        assertEquals(1.0, retry.fastest.get(GameType.REACTION));
    }

    @Test
    void loadedValuesAreNotMarkedAsChanged() throws SQLException {
        UUID uuid = UUID.randomUUID();
        ChatGamesPlayer player = repository.load(uuid);
        player.incrementStat(Stat.WINS, 4);
        repository.save(player.snapshot());

        ChatGamesPlayer loaded = repository.load(uuid);
        assertTrue(loaded.snapshot().stats.isEmpty());
    }

    @Test
    void slowerTimesDoNotReplaceTheBest() {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        assertTrue(player.recordTime(GameType.MATH, 3.0));
        assertFalse(player.recordTime(GameType.MATH, 3.5));
        assertFalse(player.recordTime(GameType.MATH, 3.0));
        assertTrue(player.recordTime(GameType.MATH, 2.0));
        assertEquals(2.0, player.getFastest(GameType.MATH));
        assertEquals(0, player.getFastest(GameType.TRIVIA), "0 means never won");
    }

    @Test
    void pointsNeverGoNegative() {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        player.addPoints(5);
        assertEquals(0, player.addPoints(-20));
        player.setPoints(-3);
        assertEquals(0, player.getPoints());
    }

    @Test
    void deleteRemovesEveryRow() throws SQLException {
        UUID uuid = UUID.randomUUID();
        ChatGamesPlayer player = repository.load(uuid);
        player.incrementStat(Stat.WINS, 1);
        player.recordTime(GameType.MATH, 1.0);
        repository.save(player.snapshot());

        assertTrue(repository.delete(uuid));
        for (String table : new String[]{"chatgames", "chatgames_fastest_times", "chatgames_stats", "chatgames_daily"}) {
            assertEquals(0, count(table, uuid), table);
        }
        assertFalse(repository.delete(uuid), "nothing left to delete");
    }

    @Test
    void timesForRemovedGamesAreIgnoredOnLoad() throws SQLException {
        UUID uuid = UUID.randomUUID();
        repository.load(uuid);
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.createStatement().executeUpdate("INSERT INTO chatgames_fastest_times VALUES ('"
                    + uuid + "', 'some_old_game', 1.0)");
        }

        ChatGamesPlayer loaded = repository.load(uuid);
        assertEquals(0, loaded.getBestOverall());
    }

    /**
     * Wins arrive on many threads at once; no increment may be lost.
     */
    @Test
    void concurrentWinsAreAllCounted() throws InterruptedException {
        ChatGamesPlayer player = new ChatGamesPlayer(UUID.randomUUID());
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 1000; i++) {
            pool.execute(() -> {
                player.incrementStat(Stat.WINS, 1);
                player.addPoints(1);
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1000, player.getStat(Stat.WINS));
        assertEquals(1000, player.getPoints());
    }

    private int count(String table, UUID uuid) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url)) {
            return QueryUtils.query(connection, "SELECT COUNT(*) FROM " + table + " WHERE UUID = ?",
                    rs -> rs.next() ? rs.getInt(1) : 0, uuid.toString());
        }
    }
}
