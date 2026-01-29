package com.tickettracker.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinanceApprovalDAO extends BaseDAO {

    public static class FinanceApproval {
        private byte[] id;
        private byte[] ticketId;
        private BigDecimal tentativeCost;
        private String costDeductedFrom;
        private String remarks;
        private byte[] financeOfficerId;
        private String status;
        private String rejectionReason;
        private byte[] submittedBy;
        private Timestamp submittedAt;
        private Timestamp decidedAt;
        private Timestamp createdAt;
        private Timestamp updatedAt;

        public byte[] getId() { return id; }
        public void setId(byte[] id) { this.id = id; }
        public byte[] getTicketId() { return ticketId; }
        public void setTicketId(byte[] ticketId) { this.ticketId = ticketId; }
        public BigDecimal getTentativeCost() { return tentativeCost; }
        public void setTentativeCost(BigDecimal tentativeCost) { this.tentativeCost = tentativeCost; }
        public String getCostDeductedFrom() { return costDeductedFrom; }
        public void setCostDeductedFrom(String costDeductedFrom) { this.costDeductedFrom = costDeductedFrom; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public byte[] getFinanceOfficerId() { return financeOfficerId; }
        public void setFinanceOfficerId(byte[] financeOfficerId) { this.financeOfficerId = financeOfficerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRejectionReason() { return rejectionReason; }
        public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
        public byte[] getSubmittedBy() { return submittedBy; }
        public void setSubmittedBy(byte[] submittedBy) { this.submittedBy = submittedBy; }
        public Timestamp getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
        public Timestamp getDecidedAt() { return decidedAt; }
        public void setDecidedAt(Timestamp decidedAt) { this.decidedAt = decidedAt; }
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        public Timestamp getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    }

    public FinanceApproval create(FinanceApproval approval) throws SQLException {
        String sql = "INSERT INTO finance_approvals (id, ticket_id, tentative_cost, cost_deducted_from, " +
                "remarks, finance_officer_id, status, submitted_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);

            byte[] id = approval.getId();
            if (id == null) {
                id = generateUUID();
                approval.setId(id);
            }

            stmt.setBytes(1, id);
            stmt.setBytes(2, approval.getTicketId());
            stmt.setBigDecimal(3, approval.getTentativeCost());
            stmt.setString(4, approval.getCostDeductedFrom());
            stmt.setString(5, approval.getRemarks());
            stmt.setBytes(6, approval.getFinanceOfficerId());
            stmt.setString(7, approval.getStatus() != null ? approval.getStatus() : "pending");
            stmt.setBytes(8, approval.getSubmittedBy());

            int rowsAffected = stmt.executeUpdate();
            logger.info("Created finance approval for ticket (rows affected: {})", rowsAffected);

            return approval;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public FinanceApproval findById(byte[] id) throws SQLException {
        String sql = "SELECT * FROM finance_approvals WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToFinanceApproval(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FinanceApproval> findByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM finance_approvals WHERE ticket_id = ? ORDER BY submitted_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            List<FinanceApproval> approvals = new ArrayList<>();
            while (rs.next()) {
                approvals.add(mapResultSetToFinanceApproval(rs));
            }
            return approvals;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public FinanceApproval findLatestByTicketId(byte[] ticketId) throws SQLException {
        String sql = "SELECT * FROM finance_approvals WHERE ticket_id = ? " +
                "ORDER BY submitted_at DESC FETCH FIRST 1 ROW ONLY";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, ticketId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToFinanceApproval(rs);
            }
            return null;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FinanceApproval> findByFinanceOfficerId(byte[] financeOfficerId) throws SQLException {
        String sql = "SELECT * FROM finance_approvals WHERE finance_officer_id = ? " +
                "ORDER BY submitted_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, financeOfficerId);
            rs = stmt.executeQuery();

            List<FinanceApproval> approvals = new ArrayList<>();
            while (rs.next()) {
                approvals.add(mapResultSetToFinanceApproval(rs));
            }
            return approvals;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<FinanceApproval> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM finance_approvals WHERE status = ? ORDER BY submitted_at DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            rs = stmt.executeQuery();

            List<FinanceApproval> approvals = new ArrayList<>();
            while (rs.next()) {
                approvals.add(mapResultSetToFinanceApproval(rs));
            }
            return approvals;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public FinanceApproval approve(byte[] id) throws SQLException {
        String sql = "UPDATE finance_approvals SET status = 'approved', decided_at = CURRENT_TIMESTAMP, " +
                "updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Approved finance approval (rows affected: {})", rowsAffected);

            return findById(id);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public FinanceApproval reject(byte[] id, String rejectionReason) throws SQLException {
        String sql = "UPDATE finance_approvals SET status = 'rejected', rejection_reason = ?, " +
                "decided_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, rejectionReason);
            stmt.setBytes(2, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Rejected finance approval (rows affected: {})", rowsAffected);

            return findById(id);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(byte[] id) throws SQLException {
        String sql = "DELETE FROM finance_approvals WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBytes(1, id);

            int rowsAffected = stmt.executeUpdate();
            logger.info("Deleted finance approval (rows affected: {})", rowsAffected);

            return rowsAffected > 0;
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private FinanceApproval mapResultSetToFinanceApproval(ResultSet rs) throws SQLException {
        FinanceApproval approval = new FinanceApproval();
        approval.setId(rs.getBytes("id"));
        approval.setTicketId(rs.getBytes("ticket_id"));
        approval.setTentativeCost(rs.getBigDecimal("tentative_cost"));
        approval.setCostDeductedFrom(rs.getString("cost_deducted_from"));
        approval.setRemarks(rs.getString("remarks"));
        approval.setFinanceOfficerId(rs.getBytes("finance_officer_id"));
        approval.setStatus(rs.getString("status"));
        approval.setRejectionReason(rs.getString("rejection_reason"));
        approval.setSubmittedBy(rs.getBytes("submitted_by"));
        approval.setSubmittedAt(rs.getTimestamp("submitted_at"));
        approval.setDecidedAt(rs.getTimestamp("decided_at"));
        approval.setCreatedAt(rs.getTimestamp("created_at"));
        approval.setUpdatedAt(rs.getTimestamp("updated_at"));
        return approval;
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
}
