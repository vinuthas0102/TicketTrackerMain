package com.tickettracker.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tickettracker.util.UuidUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinanceApprovalDAO extends BaseDAO {

    static class UuidBytesSerializer extends JsonSerializer<byte[]> {
        @Override
        public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(UuidUtil.bytesToUuidString(value));
            }
        }
    }

    public static class FinanceApproval {
        @JsonSerialize(using = UuidBytesSerializer.class)
        private byte[] id;
        @JsonProperty("ticketId")
        @JsonSerialize(using = UuidBytesSerializer.class)
        private byte[] ticketId;
        @JsonProperty("tentativeCost")
        private BigDecimal tentativeCost;
        @JsonProperty("costDeductedFrom")
        private String costDeductedFrom;
        private String remarks;
        @JsonProperty("approvalRemarks")
        private String approvalRemarks;
        @JsonProperty("financeOfficerId")
        @JsonSerialize(using = UuidBytesSerializer.class)
        private byte[] financeOfficerId;
        private String status;
        @JsonProperty("rejectionReason")
        private String rejectionReason;
        @JsonProperty("submittedBy")
        @JsonSerialize(using = UuidBytesSerializer.class)
        private byte[] submittedBy;
        @JsonProperty("submittedAt")
        private Timestamp submittedAt;
        @JsonProperty("decidedAt")
        private Timestamp decidedAt;
        @JsonProperty("createdAt")
        private Timestamp createdAt;
        @JsonProperty("updatedAt")
        private Timestamp updatedAt;
        @JsonProperty("approvalDocumentFileName")
        private String approvalDocumentFileName;
        @JsonProperty("approvalDocumentFilePath")
        private String approvalDocumentFilePath;
        @JsonProperty("approvalDocumentFileSize")
        private Integer approvalDocumentFileSize;
        @JsonProperty("approvalDocumentFileType")
        private String approvalDocumentFileType;
        @JsonProperty("approvalDocumentUploadedAt")
        private Timestamp approvalDocumentUploadedAt;

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
        public String getApprovalRemarks() { return approvalRemarks; }
        public void setApprovalRemarks(String approvalRemarks) { this.approvalRemarks = approvalRemarks; }
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
        public String getApprovalDocumentFileName() { return approvalDocumentFileName; }
        public void setApprovalDocumentFileName(String approvalDocumentFileName) { this.approvalDocumentFileName = approvalDocumentFileName; }
        public String getApprovalDocumentFilePath() { return approvalDocumentFilePath; }
        public void setApprovalDocumentFilePath(String approvalDocumentFilePath) { this.approvalDocumentFilePath = approvalDocumentFilePath; }
        public Integer getApprovalDocumentFileSize() { return approvalDocumentFileSize; }
        public void setApprovalDocumentFileSize(Integer approvalDocumentFileSize) { this.approvalDocumentFileSize = approvalDocumentFileSize; }
        public String getApprovalDocumentFileType() { return approvalDocumentFileType; }
        public void setApprovalDocumentFileType(String approvalDocumentFileType) { this.approvalDocumentFileType = approvalDocumentFileType; }
        public Timestamp getApprovalDocumentUploadedAt() { return approvalDocumentUploadedAt; }
        public void setApprovalDocumentUploadedAt(Timestamp approvalDocumentUploadedAt) { this.approvalDocumentUploadedAt = approvalDocumentUploadedAt; }
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

    public FinanceApproval approve(byte[] id, String approvalRemarks) throws SQLException {
        String sql = "UPDATE finance_approvals SET status = 'approved', approval_remarks = ?, " +
                "decided_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, approvalRemarks);
            stmt.setBytes(2, id);

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
        approval.setApprovalRemarks(rs.getString("approval_remarks"));
        approval.setFinanceOfficerId(rs.getBytes("finance_officer_id"));
        approval.setStatus(rs.getString("status"));
        approval.setRejectionReason(rs.getString("rejection_reason"));
        approval.setSubmittedBy(rs.getBytes("submitted_by"));
        approval.setSubmittedAt(rs.getTimestamp("submitted_at"));
        approval.setDecidedAt(rs.getTimestamp("decided_at"));
        approval.setCreatedAt(rs.getTimestamp("created_at"));
        approval.setUpdatedAt(rs.getTimestamp("updated_at"));
        approval.setApprovalDocumentFileName(rs.getString("approval_document_file_name"));
        approval.setApprovalDocumentFilePath(rs.getString("approval_document_file_path"));
        int fileSize = rs.getInt("approval_document_file_size");
        approval.setApprovalDocumentFileSize(rs.wasNull() ? null : fileSize);
        approval.setApprovalDocumentFileType(rs.getString("approval_document_file_type"));
        approval.setApprovalDocumentUploadedAt(rs.getTimestamp("approval_document_uploaded_at"));
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
