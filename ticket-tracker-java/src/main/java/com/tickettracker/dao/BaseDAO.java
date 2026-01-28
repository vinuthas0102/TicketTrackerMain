package com.tickettracker.dao;

import com.tickettracker.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base DAO class providing common database operations.
 * All DAO classes should extend this class.
 */
public abstract class BaseDAO {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final DatabaseConfig dbConfig;

    public BaseDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    /**
     * Get a database connection from the pool
     *
     * @return Connection object
     * @throws SQLException if connection cannot be obtained
     */
    protected Connection getConnection() throws SQLException {
        return dbConfig.getConnection();
    }

    /**
     * Close JDBC resources safely
     *
     * @param conn Connection to close
     * @param stmt PreparedStatement to close
     * @param rs ResultSet to close
     */
    protected void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.warn("Error closing ResultSet", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.warn("Error closing PreparedStatement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("Error closing Connection", e);
            }
        }
    }

    /**
     * Close connection only
     *
     * @param conn Connection to close
     */
    protected void closeConnection(Connection conn) {
        closeResources(conn, null, null);
    }

    /**
     * Close statement and result set
     *
     * @param stmt PreparedStatement to close
     * @param rs ResultSet to close
     */
    protected void closeResources(PreparedStatement stmt, ResultSet rs) {
        closeResources(null, stmt, rs);
    }

    /**
     * Execute a query and return result set
     * NOTE: Caller is responsible for closing resources
     *
     * @param sql SQL query
     * @param params Query parameters
     * @return ResultSet
     * @throws SQLException if query execution fails
     */
    protected ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        logger.debug("Executing query: {}", sql);
        return stmt.executeQuery();
    }

    /**
     * Execute an update/insert/delete statement
     *
     * @param sql SQL statement
     * @param params Statement parameters
     * @return number of rows affected
     * @throws SQLException if statement execution fails
     */
    protected int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            logger.debug("Executing update: {}", sql);
            return stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Set parameters on a PreparedStatement
     *
     * @param stmt PreparedStatement
     * @param params Parameters to set
     * @throws SQLException if parameter setting fails
     */
    protected void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * Begin a transaction
     *
     * @param conn Connection
     * @throws SQLException if transaction cannot be started
     */
    protected void beginTransaction(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
    }

    /**
     * Commit a transaction
     *
     * @param conn Connection
     * @throws SQLException if commit fails
     */
    protected void commitTransaction(Connection conn) throws SQLException {
        conn.commit();
        conn.setAutoCommit(true);
    }

    /**
     * Rollback a transaction
     *
     * @param conn Connection
     */
    protected void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Error rolling back transaction", e);
            }
        }
    }
}
