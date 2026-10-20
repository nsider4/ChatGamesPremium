package org.nsider.chatgames.database;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.*;
import org.nsider.chatgames.utils.database.DatabaseUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Initializes all query-related classes for the ChatGames plugin.
 *
 * <p>Runs once the connection pool is open: every query class learns its table
 * name, then the tables are created if they don't exist yet.</p>
 */
public final class QueryInitializer {

    private QueryInitializer() {}

    /**
     * Initializes all query modules and creates the tables.
     *
     * @param plugin The main instance of the ChatGames plugin.
     * @return true if every table exists afterwards
     */
    public static boolean initialize(ChatGames plugin) {
        DatabaseUtils.initialize(plugin);

        ManagementQueries.initialize(plugin);
        ValueQueries.initialize(plugin);
        UpdateQueries.initialize(plugin);
        FastestTimeQueries.initialize(plugin);
        TopPlayerQueries.initialize(plugin);

        try (Connection connection = DatabaseUtils.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(ManagementQueries.getSQLiteCreateChatGamesTable());
            statement.executeUpdate(FastestTimeQueries.getSQLiteCreateFastestTimesTable());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create the database tables.", e);
            return false;
        }

        // Creates its own table
        ChallengeQueries.initialize(plugin);
        return true;
    }
}
