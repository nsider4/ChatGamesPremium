package org.nsider.chatgames.database.enums;

import lombok.Getter;

/**
 * Enum representing the JDBC URL prefixes for different database types.
 */
@Getter
public enum JDBCUrlPrefix {

    /**
     * MySQL JDBC URL prefix.
     */
    MySQL("jdbc:mysql"),

    /**
     * SQLite JDBC URL prefix.
     */
    SQLite("jdbc:sqlite");

    /**
     * The JDBC URL prefix for the database type.
     */
    private final String urlPrefix;

    /**
     * Constructor for the {@link JDBCUrlPrefix} enum.
     *
     * @param urlPrefix The JDBC URL prefix for the database type.
     */
    JDBCUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }
}