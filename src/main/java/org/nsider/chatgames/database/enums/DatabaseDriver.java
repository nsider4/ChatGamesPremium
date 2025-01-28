package org.nsider.chatgames.database.enums;

import lombok.Getter;

/**
 * Enum representing different database drivers with their corresponding driver class names.
 */
@Getter
public enum DatabaseDriver {

    /**
     * MySQL database driver.
     */
    MySQL("com.mysql.cj.jdbc.Driver"),

    /**
     * SQLite database driver.
     */
    SQLite("org.sqlite.JDBC");

    /**
     * The fully qualified class name of the database driver.
     */
    private final String driverClass;

    /**
     * Constructor for the {@link DatabaseDriver} enum.
     *
     * @param driverClass The class name of the database driver.
     */
    DatabaseDriver(String driverClass) {
        this.driverClass = driverClass;
    }
}
