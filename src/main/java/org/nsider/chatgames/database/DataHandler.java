package org.nsider.chatgames.database;

import org.nsider.chatgames.database.enums.StorageType;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for handling database operations.
 * Provides methods for initializing, closing, and accessing the database connection.
 */
public interface DataHandler {

    /**
     * Opens the connection pool.
     *
     * @return true if the database is reachable
     */
    boolean initialize();

    /**
     * Closes the storage connection.
     * This method should be called to properly close the database connection and release resources.
     */
    void disable();

    /**
     * Retrieves the storage type used by the database handler.
     *
     * @return The {@link StorageType} representing the type of storage (e.g., SQLite, MySQL).
     */
    StorageType getStorageType();

    /**
     * Retrieves the connection to the storage.
     *
     * @return The {@link Connection} object for interacting with the database.
     * @throws SQLException if unable to establish a connection or an error occurs.
     */
    Connection getConnection() throws SQLException;
}