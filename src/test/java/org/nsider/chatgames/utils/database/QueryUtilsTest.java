package org.nsider.chatgames.utils.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QueryUtilsTest {

    private Connection connection;

    @BeforeEach
    void openDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE players (UUID TEXT PRIMARY KEY, POINTS INTEGER)");
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO players VALUES (?, ?)")) {
            assertEquals(1, QueryUtils.executeUpdate(insert, "a", 10));
            assertEquals(1, QueryUtils.executeUpdate(insert, "b", 30));
            assertEquals(1, QueryUtils.executeUpdate(insert, "c", 20));
        }
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void readsRowsWhileTheStatementIsOpen() throws SQLException {
        // The 1.9.9-era helper returned a ResultSet from a closed statement, so this read failed
        int points = QueryUtils.query(connection, "SELECT POINTS FROM players WHERE UUID = ?",
                rs -> rs.next() ? rs.getInt("POINTS") : -1, "b");

        assertEquals(30, points);
    }

    @Test
    void mapsEveryRow() throws SQLException {
        List<String> order = QueryUtils.query(connection, "SELECT UUID FROM players ORDER BY POINTS DESC", rs -> {
            List<String> rows = new ArrayList<>();
            while (rs.next()) rows.add(rs.getString(1));
            return rows;
        });

        assertEquals(Arrays.asList("b", "c", "a"), order);
    }

    @Test
    void returnsTheHandlersValueWhenNothingMatches() throws SQLException {
        String uuid = QueryUtils.query(connection, "SELECT UUID FROM players WHERE POINTS > ?",
                rs -> rs.next() ? rs.getString(1) : null, 1000);

        assertNull(uuid);
    }

    @Test
    void bindsPositionalParameters() throws SQLException {
        int count = QueryUtils.query(connection, "SELECT COUNT(*) FROM players WHERE POINTS BETWEEN ? AND ?",
                rs -> rs.next() ? rs.getInt(1) : 0, 15, 30);

        assertEquals(2, count);
    }
}
