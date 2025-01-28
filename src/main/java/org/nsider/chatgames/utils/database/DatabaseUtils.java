package org.nsider.chatgames.utils.database;

import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.annotations.Nonnull;
import org.nsider.chatgames.annotations.Nullable;
import org.nsider.chatgames.database.DataHandler;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class for managing database connections.
 * Provides methods to initialize the database handler and retrieve connections.
 */
public class DatabaseUtils {

    @Nullable
    private static DataHandler dataHandler;

    /**
     * Initializes the {@code DatabaseUtils} with the plugin's {@link DataHandler}.
     *
     * @param plugin The instance of your main plugin class. Must not be null.
     * @throws IllegalArgumentException if the plugin or its DataHandler is null.
     */
    public static void initialize(@Nonnull ChatGames plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null.");
        }
        dataHandler = plugin.getDataHandler();
        if (dataHandler == null) {
            throw new IllegalArgumentException("DataHandler instance cannot be null.");
        }
    }

    /**
     * Gets a connection from the database pool.
     *
     * @return A {@link Connection} instance. Never null.
     * @throws SQLException             If unable to retrieve a connection.
     * @throws IllegalStateException    If the {@code DatabaseUtils} has not been initialized.
     */
    @Nonnull
    public static Connection getConnection() throws SQLException {
        if (dataHandler == null) {
            throw new IllegalStateException("DatabaseUtils has not been initialized. Call initialize() first.");
        }
        return dataHandler.getConnection();
    }
}
