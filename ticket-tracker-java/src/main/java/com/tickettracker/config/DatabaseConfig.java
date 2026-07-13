package com.tickettracker.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration and connection pool management using Apache Commons DBCP2.
 * This class provides a singleton connection pool for Oracle database access.
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static DatabaseConfig instance;
    private BasicDataSource dataSource;

    private DatabaseConfig() {
        initializeDataSource();
    }

    /**
     * Get the singleton instance of DatabaseConfig
     *
     * @return DatabaseConfig instance
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Initialize the Apache Commons DBCP2 connection pool
     */
    private void initializeDataSource() {
        try {
            Properties props = loadDatabaseProperties();

            dataSource = new BasicDataSource();

            // Basic connection properties
            dataSource.setDriverClassName(props.getProperty("db.driver", "oracle.jdbc.OracleDriver"));
            dataSource.setUrl(props.getProperty("db.url"));
            dataSource.setUsername(props.getProperty("db.username"));
            dataSource.setPassword(props.getProperty("db.password"));

            // Connection pool settings
            dataSource.setInitialSize(Integer.parseInt(props.getProperty("db.pool.initial", "5")));
            dataSource.setMaxTotal(Integer.parseInt(props.getProperty("db.pool.max", "20")));
            dataSource.setMaxIdle(Integer.parseInt(props.getProperty("db.pool.idle", "10")));
            dataSource.setMaxWaitMillis(Long.parseLong(props.getProperty("db.pool.maxWaitMillis", "10000")));

            // Connection validation
            dataSource.setValidationQuery(props.getProperty("db.validation.query", "SELECT 1 FROM DUAL"));
            dataSource.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("db.testOnBorrow", "true")));
            dataSource.setTestWhileIdle(Boolean.parseBoolean(props.getProperty("db.testWhileIdle", "true")));
            dataSource.setTimeBetweenEvictionRunsMillis(
                    Long.parseLong(props.getProperty("db.timeBetweenEvictionRunsMillis", "60000")));
            dataSource.setMinEvictableIdleTimeMillis(
                    Long.parseLong(props.getProperty("db.minEvictableIdleTimeMillis", "300000")));

            // Connection leak detection
            dataSource.setRemoveAbandonedTimeout(
                    Integer.parseInt(props.getProperty("db.removeAbandonedTimeout", "300")));
            dataSource.setRemoveAbandonedOnBorrow(
                    Boolean.parseBoolean(props.getProperty("db.removeAbandonedOnBorrow", "true")));
            dataSource.setRemoveAbandonedOnMaintenance(
                    Boolean.parseBoolean(props.getProperty("db.removeAbandonedOnMaintenance", "true")));
            dataSource.setLogAbandoned(Boolean.parseBoolean(props.getProperty("db.logAbandoned", "true")));

            logger.info("Database connection pool initialized successfully");
            logger.info("Database URL: {}", dataSource.getUrl());
            logger.info("Pool size: initial={}, max={}", dataSource.getInitialSize(), dataSource.getMaxTotal());

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Load database properties from database.properties file
     *
     * @return Properties object
     * @throws IOException if properties file cannot be loaded
     */
    private Properties loadDatabaseProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new IOException("Unable to find database.properties");
            }
            props.load(input);
            logger.info("Database properties loaded successfully");
        }
        return props;
    }

    /**
     * Get a database connection from the pool
     *
     * @return Connection object
     * @throws SQLException if connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        Connection conn = dataSource.getConnection();
        logger.debug("Connection obtained from pool. Active: {}, Idle: {}",
                dataSource.getNumActive(), dataSource.getNumIdle());
        return conn;
    }

    /**
     * Get connection pool statistics
     *
     * @return String with pool statistics
     */
    public String getPoolStatistics() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        return String.format("Connection Pool Stats - Active: %d, Idle: %d, Max: %d",
                dataSource.getNumActive(),
                dataSource.getNumIdle(),
                dataSource.getMaxTotal());
    }

    /**
     * Check if the connection pool is healthy
     *
     * @return true if pool can provide connections
     */
    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Close the data source and release all connections
     * Should be called during application shutdown
     */
    public void shutdown() {
        if (dataSource != null) {
            try {
                dataSource.close();
                logger.info("Database connection pool shut down successfully");
            } catch (SQLException e) {
                logger.error("Error closing data source", e);
            }
        }
    }
}
