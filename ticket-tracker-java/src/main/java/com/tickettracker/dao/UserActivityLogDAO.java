package com.tickettracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user_activity_logs table.
 * Tracks user activity events (login, logout, session refresh).
 */
public class UserActivityLogDAO extends BaseDAO {

    public void insert(ActivityLog log) throws SQLException {
        String sql = "INSERT INTO user_activity_logs " +
                "(user_id, action, ip_address, user_agent, details) " +
                "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, log.getUserId());
            stmt.setString(2, log.getAction());
            stmt.setString(3, log.getIpAddress());
            stmt.setString(4, log.getUserAgent());
            stmt.setString(5, log.getDetails());
            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public List<ActivityLog> findByUserId(byte[] userId, int limit) throws SQLException {
        String sql = "SELECT id, user_id, action, ip_address, user_agent, details, created_at " +
                "FROM user_activity_logs WHERE user_id = ? " +
                "ORDER BY created_at DESC FETCH FIRST ? ROWS ONLY";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<ActivityLog> logs = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            stmt.setInt(2, limit > 0 ? limit : 50);
            rs = stmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
            return logs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<ActivityLog> findRecentActivity(int limit) throws SQLException {
        String sql = "SELECT id, user_id, action, ip_address, user_agent, details, created_at " +
                "FROM user_activity_logs ORDER BY created_at DESC FETCH FIRST ? ROWS ONLY";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<ActivityLog> logs = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit > 0 ? limit : 50);
            rs = stmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
            return logs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private ActivityLog mapResultSetToLog(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getBytes("id"));
        log.setUserId(rs.getBytes("user_id"));
        log.setAction(rs.getString("action"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        log.setDetails(rs.getString("details"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        return log;
    }

    public static class ActivityLog {
        private byte[] id;
        private byte[] userId;
        private String action;
        private String ipAddress;
        private String userAgent;
        private String details;
        private Timestamp createdAt;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public byte[] getUserId() { return userId; }
        public void setUserId(byte[] userId) { this.userId = userId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    }
}
