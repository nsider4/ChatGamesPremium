package org.nsider.chatgames.database;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.TopPlayerQueries;
import org.nsider.chatgames.database.queries.ValueQueries;
import org.nsider.chatgames.model.player.PlayerRepository;
import org.nsider.chatgames.utils.database.DatabaseUtils;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Prepares the database once the connection pool is open: creates missing tables
 * and gives the leaderboard queries their table names.
 */
public final class QueryInitializer {

    private QueryInitializer() {}

    /**
     * @return true if every table exists afterwards
     */
    public static boolean initialize(ChatGames plugin, PlayerRepository repository) {
        DatabaseUtils.initialize(plugin);

        try {
            repository.createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create the database tables.", e);
            return false;
        }

        ValueQueries.initialize(plugin);
        TopPlayerQueries.initialize(plugin);
        return true;
    }
}
