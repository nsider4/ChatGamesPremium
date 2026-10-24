package org.nsider.chatgames.database;

/**
 * A utility class for handling error messages.
 */
public final class Errors {

    private Errors() {}

    /**
     * Returns a message indicating a failure to execute a SQL statement.
     *
     * @return The error message.
     */
    public static String sqlConnectionExecute() {
        return "Couldn't execute SQL statement: ";
    }
}
