package org.nsider.chatgames.utils.database;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.sql.*;

/**
 * Utility class for executing SQL queries.
 * Provides methods to execute SQL queries and updates with bound parameters.
 */
public final class QueryUtils {

    private QueryUtils() {}

    /**
     * Executes a SQL update query with parameters.
     *
     * @param statement The prepared statement to execute. Must not be null.
     * @param params    The parameters to set in the prepared statement. Can be null or empty.
     * @return The number of rows affected by the update.
     * @throws SQLException If a database access error occurs.
     */
    public static int executeUpdate(@Nonnull PreparedStatement statement, @Nullable Object... params) throws SQLException {
        setParameters(statement, params);
        return statement.executeUpdate();
    }

    /**
     * Reads the rows of a result set. Called while the statement is still open.
     *
     * @param <T> the value built from the rows
     */
    @FunctionalInterface
    public interface ResultHandler<T> {
        T handle(ResultSet resultSet) throws SQLException;
    }

    /**
     * Executes a SQL query and maps its rows with {@code handler}.
     *
     * <p>The rows are read inside this method because closing the statement
     * also closes its result set, so a {@link ResultSet} can't be returned.</p>
     *
     * @param conn    The database connection. Must not be null.
     * @param sql     The SQL query to execute. Must not be null.
     * @param handler Reads the result set into a value.
     * @param params  The parameters to set in the prepared statement. Can be null or empty.
     * @return whatever {@code handler} returns
     * @throws SQLException If a database access error occurs.
     */
    public static <T> T query(@Nonnull Connection conn, @Nonnull String sql, @Nonnull ResultHandler<T> handler,
                              @Nullable Object... params) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            setParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return handler.handle(resultSet);
            }
        }
    }

    /**
     * Sets the provided parameters on the given {@link PreparedStatement}.
     * This method iterates over the array of parameters and binds them to the
     * {@link PreparedStatement} using 1-based indexing.
     *
     * @param statement The {@link PreparedStatement} to set the parameters on. Must not be null.
     * @param params    The parameters to set on the {@link PreparedStatement}.
     *                  These are passed as a varargs array and can be null or empty.
     * @throws SQLException If there is an error setting the parameters on the statement.
     */
    private static void setParameters(@Nonnull PreparedStatement statement, @Nullable Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
        }
    }
}