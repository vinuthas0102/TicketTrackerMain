package com.tickettracker.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static DatabaseConfig instance;
    private HikariDataSource dataSource;

    private DatabaseConfig() {
        initializeDataSource();
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    private void initializeDataSource() {
        try {
            Properties props = loadDatabaseProperties();

            HikariConfig config = new HikariConfig();

            config.setDriverClassName(props.getProperty("db.driver", "oracle.jdbc.OracleDriver"));
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));

            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "20")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
            config.setKeepaliveTime(Long.parseLong(props.getProperty("db.pool.keepaliveTime", "120000")));

            config.setConnectionTestQuery(props.getProperty("db.validation.query", "SELECT 1 FROM DUAL"));
            config.setPoolName("ticket-tracker-pool");

            dataSource = new HikariDataSource(config);

            logger.info("HikariCP connection pool initialized successfully");
            logger.info("Database URL: {}", config.getJdbcUrl());
            logger.info("Pool size: minIdle={}, max={}", config.getMinimumIdle(), config.getMaximumPoolSize());

        } catch (Exception e) {
            logger.error("Failed to initialize HikariCP connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

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

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        Connection conn = dataSource.getConnection();
        logger.debug("Connection obtained from pool. Active: {}, Idle: {}, Total: {}",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections());
        return conn;
    }

    public String getPoolStatistics() {
        if (dataSource == null) {
            return "DataSource not initialized";
        }
        var pool = dataSource.getHikariPoolMXBean();
        return String.format("Connection Pool Stats - Active: %d, Idle: %d, Total: %d, Threads Waiting: %d",
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getTotalConnections(),
                pool.getThreadsAwaitingConnection());
    }

    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("Health check failed", e);
            return false;
        }
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("HikariCP connection pool shut down successfully");
        }
    }
}
