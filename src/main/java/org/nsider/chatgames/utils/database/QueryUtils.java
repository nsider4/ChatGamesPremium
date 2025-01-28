package org.nsider.chatgames.utils.database;

import org.nsider.chatgames.annotations.Nonnull;
import org.nsider.chatgames.annotations.Nullable;

import java.sql.*;

/**
 * Utility class for executing SQL queries.
 * Provides methods to execute SQL update queries with or without parameters.
 */
public class QueryUtils {

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
     * Executes a SQL query and returns the result set.
     *
     * @param conn The database connection. Must not be null.
     * @param sql  The SQL query to execute. Must not be null.
     * @param params The parameters to set in the prepared statement. Can be null or empty.
     * @return The ResultSet of the query.
     * @throws SQLException If a database access error occurs.
     */
    public static ResultSet executeQuery(@Nonnull Connection conn, @Nonnull String sql, @Nullable Object... params) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            setParameters(statement, params);
            return statement.executeQuery();
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