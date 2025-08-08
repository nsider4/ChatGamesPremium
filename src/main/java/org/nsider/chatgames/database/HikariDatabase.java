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
import org.nsider.chatgames.database.queries.FastestTimeQueries;
import org.nsider.chatgames.database.queries.ManagementQueries;
import org.nsider.inventorymanager.extras.Nonnull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * A database handler implementation using HikariCP for connection pooling.
 * This class supports both MySQL and SQLite database types.
 */
public class HikariDatabase implements DataHandler {

    private final ChatGames plugin;
    private HikariDataSource dataSource;
    private final String driverClass;
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
            plugin.getLogger().warning("No driver class found: " + driverClass);
        }
    }

    @Override
    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(storageType.name());

        if (section == null) {
            plugin.getLogger().warning("Failed to load database config.");
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(section.getString("user", "root"));
        hikariConfig.setPassword(section.getString("password", "pa55w0rd"));

        String jdbcUrl = buildJdbcUrl(section);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setDriverClassName(driverClass);
        hikariConfig.setMaximumPoolSize(section.getInt("Pool-Settings.max-pool-size", 10));
        hikariConfig.setMinimumIdle(section.getInt("Pool-Settings.min-idle", 10));
        hikariConfig.setMaxLifetime(section.getLong("Pool-Settings.max-lifetime", 180000L));
        hikariConfig.setConnectionTimeout(section.getLong("Pool-Settings.time-out", 20000L));
        hikariConfig.setPoolName("ChatGamesHikariPool");

        try {
            hikariConfig.setKeepaliveTime(section.getLong("Pool-Settings.keep-alive-time", 60000L));
        } catch (NoSuchMethodError ignored) {}

        hikariConfig.setDataSourceProperties(getProperties());
        dataSource = new HikariDataSource(hikariConfig);

        setupDatabase();
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

    private void setupDatabase() {
        try (Connection connection = getConnection(); Statement s = connection.createStatement()) {
            s.executeUpdate(ManagementQueries.getSQLiteCreateChatGamesTable());
            s.executeUpdate(FastestTimeQueries.getSQLiteCreateFastestTimesTable());
            plugin.getLogger().info("Connected to the database successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database.");
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a set of properties for the database connection. These properties help optimize the connection.
     *
     * @return A {@link Properties} object with optimized connection settings.
     */
    private static @Nonnull Properties getProperties() {
        final Properties properties = new Properties();

        Map<String, String> initialProperties = new HashMap<>();
        initialProperties.put("cachePrepStmts", "true");
        initialProperties.put("prepStmtCacheSize", "250");
        initialProperties.put("prepStmtCacheSqlLimit", "2048");
        initialProperties.put("useServerPrepStmts", "true");
        initialProperties.put("useLocalSessionState", "true");
        initialProperties.put("useLocalTransactionState", "true");

        properties.putAll(initialProperties);

        Map<String, String> additionalProperties = new HashMap<>();
        additionalProperties.put("rewriteBatchedStatements", "true");
        additionalProperties.put("cacheResultSetMetadata", "true");
        additionalProperties.put("cacheServerConfiguration", "true");
        additionalProperties.put("elideSetAutoCommits", "true");
        additionalProperties.put("maintainTimeStats", "false");

        properties.putAll(additionalProperties);
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

