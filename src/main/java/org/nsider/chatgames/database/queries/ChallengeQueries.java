// Adjusted: ChallengeQueries.java
package org.nsider.chatgames.database.queries;

import org.bukkit.OfflinePlayer;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.Errors;
import org.nsider.chatgames.model.player.ChallengeData;
import org.nsider.chatgames.utils.database.DatabaseUtils;
import org.nsider.chatgames.utils.database.QueryUtils;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Level;

public class ChallengeQueries {

    private static ChatGames plugin;
    private static String tableName;

    public static void initialize(ChatGames instance) {
        plugin = instance;
        tableName = "chatgames_daily";
        createTable();
    }

    private static Connection getConnection() throws SQLException {
        return DatabaseUtils.getConnection();
    }

    private static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "UUID VARCHAR(100) NOT NULL," +
                "WinsToday INT NOT NULL," +
                "RewardClaimed BOOLEAN NOT NULL," +
                "LastUpdated DATE NOT NULL," +
                "ChallengeStreak INT NOT NULL," +
                "DailyStreak INT NOT NULL," +
                "GlobalStreak INT NOT NULL," +
                "WeeklyWins INT NOT NULL," +
                "PRIMARY KEY (UUID)" +
                ");";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        }
    }

    public static ChallengeData getChallengeData(OfflinePlayer player) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE UUID = ?")) {

            ps.setString(1, player.getUniqueId().toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDate lastUpdated = rs.getDate("LastUpdated").toLocalDate();
                    int wins = rs.getInt("WinsToday");
                    boolean claimed = rs.getBoolean("RewardClaimed");
                    int challengeStreak = rs.getInt("ChallengeStreak");
                    int dailyStreak = rs.getInt("DailyStreak");
                    int globalStreak = rs.getInt("GlobalStreak");
                    int weeklyWins = rs.getInt("WeeklyWins");

                    if (!lastUpdated.equals(LocalDate.now())) {
                        updateChallengeProgress(player.getUniqueId(), 0, false, LocalDate.now(), challengeStreak, dailyStreak, globalStreak, weeklyWins);
                        return new ChallengeData(0, false, LocalDate.now(), challengeStreak, dailyStreak, globalStreak, weeklyWins);
                    }
                    return new ChallengeData(wins, claimed, lastUpdated, challengeStreak, dailyStreak, globalStreak, weeklyWins);
                } else {
                    insertDefault(player.getUniqueId());
                    return new ChallengeData(0, false, LocalDate.now(), 0, 0, 0, 0);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
            return null;
        }
    }

    public static void updateChallengeProgress(UUID uuid, int wins, boolean claimed, LocalDate date,
                                               int challengeStreak, int dailyStreak, int globalStreak, int weeklyWins) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO " + tableName + " (UUID, WinsToday, RewardClaimed, LastUpdated, ChallengeStreak, DailyStreak, GlobalStreak, WeeklyWins) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

            QueryUtils.executeUpdate(ps, uuid.toString(), wins, claimed, Date.valueOf(date),
                    challengeStreak, dailyStreak, globalStreak, weeklyWins);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        }
    }

    public static void insertDefault(UUID uuid) {
        updateChallengeProgress(uuid, 0, false, LocalDate.now(), 0, 0, 0, 0);
    }
}