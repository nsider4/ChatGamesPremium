package org.nsider.chatgames.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.config.settings.ConfigPaths;
import org.nsider.chatgames.config.settings.ConfigValue;
import org.nsider.chatgames.database.enums.DatabaseDriver;
import org.nsider.chatgames.database.enums.JDBCUrlPrefix;
import org.nsider.chatgames.database.enums.StorageType;
import org.nsider.inventorymanager.extras.Nonnull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A database handler implementation using HikariCP for connection pooling.
 * This class supports both MySQL and SQLite database types.
 */
public class HikariDatabase implements DataHandler {

    private static final String LEGACY_MYSQL_DRIVER = "com.mysql.jdbc.Driver";

    private final ChatGames plugin;
    private HikariDataSource dataSource;
    private String driverClass;
    private final StorageType storageType;
    private final String jdbcUrlPrefix;

    public HikariDatabase(ChatGames instance, StorageType storageType) {
        this.plugin = instance;
        this.storageType = storageType;
        this.driverClass = DatabaseDriver.valueOf(storageType.name()).getDriverClass();
        this.jdbcUrlPrefix = JDBCUrlPrefix.valueOf(storageType.name()).getUrlPrefix();

        try {
            Class.forName(this.driverClass);
        } catch (ClassNotFoundException e) {
            // Servers before 1.16 bundle MySQL Connector 5.x, which only has the legacy class name
            if (storageType == StorageType.MySQL && classExists(LEGACY_MYSQL_DRIVER)) {
                this.driverClass = LEGACY_MYSQL_DRIVER;
            } else {
                plugin.getLogger().warning("No driver class found: " + driverClass);
            }
        }
    }

    @Override
    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(storageType.name());

        if (section == null) {
            plugin.getLogger().severe("Missing the '" + storageType.name() + "' section in config.yml.");
            return false;
        }

        HikariConfig hikariConfig = new HikariConfig();
        if (storageType == StorageType.MySQL) {
            // No made-up default password: a missing one should fail visibly, not try a guess
            hikariConfig.setUsername(section.getString("user", "root"));
            hikariConfig.setPassword(section.getString("password", ""));
        }

        String jdbcUrl = buildJdbcUrl(section);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setDriverClassName(driverClass);
        if (storageType == StorageType.SQLite) {
            // SQLite allows one writer at a time; extra pooled connections only cause SQLITE_BUSY errors
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setMinimumIdle(1);
            // The SQLite driver bundled with 1.8-1.12 servers predates Connection.isValid(), which Hikari
            // calls unless a test query is set; without this the pool fails with AbstractMethodError
            hikariConfig.setConnectionTestQuery("SELECT 1");
        } else {
            hikariConfig.setMaximumPoolSize(section.getInt("Pool-Settings.max-pool-size", 10));
            hikariConfig.setMinimumIdle(section.getInt("Pool-Settings.min-idle", 10));
        }
        hikariConfig.setMaxLifetime(section.getLong("Pool-Settings.max-lifetime", 180000L));
        hikariConfig.setConnectionTimeout(section.getLong("Pool-Settings.time-out", 20000L));
        hikariConfig.setPoolName("ChatGamesHikariPool");

        try {
            hikariConfig.setKeepaliveTime(section.getLong("Pool-Settings.keep-alive-time", 60000L));
        } catch (NoSuchMethodError ignored) {}

        if (storageType == StorageType.MySQL) {
            // MySQL Connector/J tuning; the SQLite driver doesn't know these options
            hikariConfig.setDataSourceProperties(getProperties());
        }
        try {
            // Opens the first connection, so a wrong host or password fails here
            dataSource = new HikariDataSource(hikariConfig);
        } catch (RuntimeException | LinkageError e) {
            // LinkageError covers drivers too old for the JDBC calls the pool makes
            plugin.getLogger().log(Level.SEVERE, "Could not connect to the " + storageType + " database.", e);
            return false;
        }
        plugin.getLogger().info("Connected to the " + storageType + " database.");
        return true;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private String buildJdbcUrl(ConfigurationSection section) {
        if (storageType == StorageType.SQLite) {
            String dbFile = ConfigValue.getString(ConfigPaths.SQLITE_FILENAME);
            File dataFolder = new File(plugin.getDataFolder(), dbFile + ".db");
            if (!dataFolder.exists()) {
                try { dataFolder.createNewFile(); }
                catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "File write error: " + dataFolder.getName(), e);
                }
            }
            return String.format(jdbcUrlPrefix + ":%s", dataFolder.getAbsolutePath());
        } else {
            return String.format("%s://%s:%s/%s%s",
                    jdbcUrlPrefix,
                    ConfigValue.getString(ConfigPaths.MYSQL_HOST),
                    ConfigValue.getString(ConfigPaths.MYSQL_PORT),
                    ConfigValue.getString(ConfigPaths.MYSQL_DATABASE),
                    ConfigValue.getString(ConfigPaths.MYSQL_CONNECTION_PARAMS)
            );
        }
    }

    /**
     * Retrieves a set of properties for the database connection. These properties help optimize the connection.
     *
     * @return A {@link Properties} object with optimized connection settings.
     */
    private static @Nonnull Properties getProperties() {
        final Properties properties = new Properties();
        properties.setProperty("cachePrepStmts", "true");
        properties.setProperty("prepStmtCacheSize", "250");
        properties.setProperty("prepStmtCacheSqlLimit", "2048");
        properties.setProperty("useServerPrepStmts", "true");
        properties.setProperty("useLocalSessionState", "true");
        properties.setProperty("useLocalTransactionState", "true");
        properties.setProperty("rewriteBatchedStatements", "true");
        properties.setProperty("cacheResultSetMetadata", "true");
        properties.setProperty("cacheServerConfiguration", "true");
        properties.setProperty("elideSetAutoCommits", "true");
        properties.setProperty("maintainTimeStats", "false");
        return properties;
    }

    /**
     * Disables the database connection pool, closing all open connections.
     */
    @Override
    public void disable() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Gets a connection from the Hikari connection pool.
     *
     * @return A {@link Connection} object.
     * @throws SQLException if unable to obtain a connection.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Retrieves the storage type (MySQL or SQLite) used by this database handler.
     *
     * @return The {@link StorageType} of the database.
     */
    @Override
    public StorageType getStorageType() {
        return storageType;
    }
}

