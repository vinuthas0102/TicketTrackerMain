package com.tickettracker.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user_management_audit table.
 * Tracks all user management actions (create, update, delete, lock, unlock, password reset).
 */
public class UserManagementAuditDAO extends BaseDAO {

    public void insert(AuditEntry entry) throws SQLException {
        String sql = "INSERT INTO user_management_audit " +
                "(action, target_user_id, performed_by, old_data, new_data, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, entry.getAction());
            stmt.setBytes(2, entry.getTargetUserId());
            stmt.setBytes(3, entry.getPerformedBy());
            stmt.setString(4, entry.getOldData());
            stmt.setString(5, entry.getNewData());
            stmt.setString(6, entry.getDescription());
            stmt.executeUpdate();
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public List<AuditEntry> findByTargetUserId(byte[] targetUserId) throws SQLException {
        String sql = "SELECT id, action, target_user_id, performed_by, old_data, new_data, " +
                "description, performed_at FROM user_management_audit " +
                "WHERE target_user_id = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<AuditEntry> entries = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, targetUserId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                entries.add(mapResultSetToEntry(rs));
            }
            return entries;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditEntry> findByPerformedBy(byte[] performedBy) throws SQLException {
        String sql = "SELECT id, action, target_user_id, performed_by, old_data, new_data, " +
                "description, performed_at FROM user_management_audit " +
                "WHERE performed_by = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<AuditEntry> entries = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, performedBy);
            rs = stmt.executeQuery();

            while (rs.next()) {
                entries.add(mapResultSetToEntry(rs));
            }
            return entries;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditEntry> findByActionType(String action) throws SQLException {
        String sql = "SELECT id, action, target_user_id, performed_by, old_data, new_data, " +
                "description, performed_at FROM user_management_audit " +
                "WHERE action = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<AuditEntry> entries = new ArrayList<>();

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, action);
            rs = stmt.executeQuery();

            while (rs.next()) {
                entries.add(mapResultSetToEntry(rs));
            }
            return entries;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private AuditEntry mapResultSetToEntry(ResultSet rs) throws SQLException {
        AuditEntry entry = new AuditEntry();
        entry.setId(rs.getBytes("id"));
        entry.setAction(rs.getString("action"));
        entry.setTargetUserId(rs.getBytes("target_user_id"));
        entry.setPerformedBy(rs.getBytes("performed_by"));
        entry.setOldData(rs.getString("old_data"));
        entry.setNewData(rs.getString("new_data"));
        entry.setDescription(rs.getString("description"));
        entry.setPerformedAt(rs.getTimestamp("performed_at"));
        return entry;
    }

    public static class AuditEntry {
        private byte[] id;
        private String action;
        private byte[] targetUserId;
        private byte[] performedBy;
        private String oldData;
        private String newData;
        private String description;
        private Timestamp performedAt;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public byte[] getTargetUserId() { return targetUserId; }
        public void setTargetUserId(byte[] targetUserId) { this.targetUserId = targetUserId; }
        public byte[] getPerformedBy() { return performedBy; }
        public void setPerformedBy(byte[] performedBy) { this.performedBy = performedBy; }
        public String getOldData() { return oldData; }
        public void setOldData(String oldData) { this.oldData = oldData; }
        public String getNewData() { return newData; }
        public void setNewData(String newData) { this.newData = newData; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Timestamp getPerformedAt() { return performedAt; }
        public void setPerformedAt(Timestamp performedAt) { this.performedAt = performedAt; }
    }
}
