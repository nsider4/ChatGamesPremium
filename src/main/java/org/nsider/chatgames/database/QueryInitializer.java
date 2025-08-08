package org.nsider.chatgames.database;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.database.queries.*;
import org.nsider.chatgames.utils.database.DatabaseUtils;

/**
 * Initializes all query-related classes for the ChatGames plugin.
 *
 * <p>This class ensures that all query modules (Management, Value, Update, and TopPlayer)
 * are properly initialized with the provided plugin instance. It is typically called
 * during the plugin's startup sequence.</p>
 */
public class QueryInitializer {

    /**
     * Constructs a new {@code QueryInitializer} and initializes all query modules.
     *
     * @param plugin The main instance of the ChatGames plugin.
     */
    public QueryInitializer(ChatGames plugin) {
        DatabaseUtils.initialize(plugin);

        ManagementQueries.initialize(plugin);
        ValueQueries.initialize(plugin);
        UpdateQueries.initialize(plugin);
        FastestTimeQueries.initialize(plugin);
        TopPlayerQueries.initialize(plugin);
    }
}
