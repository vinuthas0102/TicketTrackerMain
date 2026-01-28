package com.tickettracker.dao;

import com.tickettracker.model.AuditLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO extends BaseDAO {

    public AuditLog create(AuditLog auditLog) throws SQLException {
        String sql = "INSERT INTO audit_logs (id, ticket_id, step_id, performed_by, action, " +
                "old_data, new_data, description, action_category, metadata) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = auditLog.getId();
            if (id == null) {
                id = generateUUID();
                auditLog.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, auditLog.getTicketId());
            stmt.setBytes(3, auditLog.getStepId());
            stmt.setBytes(4, auditLog.getPerformedBy());
            stmt.setString(5, auditLog.getAction());
            stmt.setString(6, auditLog.getOldData());
            stmt.setString(7, auditLog.getNewData());
            stmt.setString(8, auditLog.getDescription());
            stmt.setString(9, auditLog.getActionCategory());
            stmt.setString(10, auditLog.getMetadata());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created audit log: {} (rows affected: {})", auditLog.getAction(), rowsAffected);

            return auditLog;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public AuditLog findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToAuditLog(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditLog> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE ticket_id = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            List<AuditLog> auditLogs = new ArrayList<>();
            while (rs.next()) {
                auditLogs.add(mapResultSetToAuditLog(rs));
            }
            return auditLogs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditLog> findByStepId(byte[] stepId) throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE step_id = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, stepId);
            rs = stmt.executeQuery();

            List<AuditLog> auditLogs = new ArrayList<>();
            while (rs.next()) {
                auditLogs.add(mapResultSetToAuditLog(rs));
            }
            return auditLogs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditLog> findByPerformedBy(byte[] userId) throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE performed_by = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, userId);
            rs = stmt.executeQuery();

            List<AuditLog> auditLogs = new ArrayList<>();
            while (rs.next()) {
                auditLogs.add(mapResultSetToAuditLog(rs));
            }
            return auditLogs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditLog> findByActionCategory(String actionCategory) throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE action_category = ? ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, actionCategory);
            rs = stmt.executeQuery();

            List<AuditLog> auditLogs = new ArrayList<>();
            while (rs.next()) {
                auditLogs.add(mapResultSetToAuditLog(rs));
            }
            return auditLogs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditLog> findByTicketIdAndCategory(byte[] ticketId, String actionCategory)
            throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE ticket_id = ? AND action_category = ? " +
                "ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            stmt.setString(2, actionCategory);
            rs = stmt.executeQuery();

            List<AuditLog> auditLogs = new ArrayList<>();
            while (rs.next()) {
                auditLogs.add(mapResultSetToAuditLog(rs));
            }
            return auditLogs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<AuditLog> findByDateRange(Timestamp startDate, Timestamp endDate)
            throws SQLException {
        String sql = "SELECT * FROM audit_logs WHERE performed_at BETWEEN ? AND ? " +
                "ORDER BY performed_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setTimestamp(1, startDate);
            stmt.setTimestamp(2, endDate);
            rs = stmt.executeQuery();

            List<AuditLog> auditLogs = new ArrayList<>();
            while (rs.next()) {
                auditLogs.add(mapResultSetToAuditLog(rs));
            }
            return auditLogs;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM audit_logs WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted audit log ID: {} (rows affected: {})", bytesToHex(id), rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(rs.getBytes("id"));
        auditLog.setTicketId(rs.getBytes("ticket_id"));
        auditLog.setStepId(rs.getBytes("step_id"));
        auditLog.setPerformedBy(rs.getBytes("performed_by"));
        auditLog.setAction(rs.getString("action"));
        auditLog.setOldData(rs.getString("old_data"));
        auditLog.setNewData(rs.getString("new_data"));
        auditLog.setDescription(rs.getString("description"));
        auditLog.setActionCategory(rs.getString("action_category"));
        auditLog.setMetadata(rs.getString("metadata"));
        auditLog.setPerformedAt(rs.getTimestamp("performed_at"));
        return auditLog;
    }

    private byte[] generateUUID() throws SQLException {
        String sql = "SELECT SYS_GUID() FROM DUAL";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBytes(1);
            }
            throw new SQLException("Failed to generate UUID");
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
